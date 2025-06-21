/* (C)2025 */
package net.joostvdg.kube_app_version.versions.helm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.versions.VersionFetcher;
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GithubOciHelmChartVersionFetcher implements VersionFetcher {

  private static final Logger logger =
      LoggerFactory.getLogger(GithubOciHelmChartVersionFetcher.class);
  private final HttpClient httpClient;
  private final Map<String, List<String>> versionCache = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Pattern GITHUB_OCI_PATTERN = Pattern.compile("^oci://ghcr\\.io/(.+)$");

  public GithubOciHelmChartVersionFetcher() {
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  @Override
  @SuppressWarnings("MixedMutabilityReturnType") // not using Guava you stupid parser
  @Cacheable(cacheNames = "kubeversion", key = "#artifact.source")
  public List<String> getAvailableVersions(AppArtifact artifact) throws Exception {
    if (!supports(artifact)) {
      logger.warn(
          "GithubOciHelmChartVersionFetcher does not support artifact: {}", artifact.getSource());
      return Collections.emptyList();
    }

    String source = artifact.getSource();
    String cacheKey = artifact.getIdentifier();

    List<String> cachedVersions = versionCache.get(cacheKey);
    if (cachedVersions != null) {
      logger.debug("Returning cached versions for {}", cacheKey);
      return List.copyOf(cachedVersions);
    }

    String githubToken = System.getenv("GITHUB_TOKEN");
    if (githubToken == null || githubToken.isEmpty()) {
      logger.error("GITHUB_TOKEN environment variable is not set or empty");
      return Collections.emptyList();
    }

    Matcher matcher = GITHUB_OCI_PATTERN.matcher(source);
    if (!matcher.matches()) {
      logger.error("Failed to parse GitHub OCI URL: {}", source);
      return Collections.emptyList();
    }

    String path = matcher.group(1);
    String[] pathParts = path.split("/", 2);
    if (pathParts.length < 2) {
      logger.error("Invalid GitHub OCI path format: {}", path);
      return Collections.emptyList();
    }

    logger.debug("Path parts of GitHub OCI URL: {}", Arrays.toString(pathParts));
    String username = pathParts[0];
    String packagePath = pathParts[1] + "/" + artifact.getArtifactName();
    String encodedPackagePath =
        URLEncoder.encode(packagePath, StandardCharsets.UTF_8).replace("+", "%20");

    String apiUrl =
        "https://api.github.com/users/"
            + username
            + "/packages/container/"
            + encodedPackagePath.replace("/", "%2F")
            + "/versions";
    logger.debug("Fetching GitHub OCI versions from: {}", apiUrl);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + githubToken)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .timeout(Duration.ofSeconds(10))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      logger.error(
          "Failed to fetch {}. HTTP status: {} - {}",
          apiUrl,
          response.statusCode(),
          response.body());
      throw new RuntimeException(
          "Failed to fetch " + apiUrl + ". HTTP status: " + response.statusCode());
    }

    List<String> versions = new ArrayList<>();
    JsonNode root = objectMapper.readTree(response.body());

    if (root.isArray()) {
      for (JsonNode versionNode : root) {
        JsonNode metadataNode = versionNode.path("metadata");
        JsonNode containerNode = metadataNode.path("container");
        JsonNode tagsNode = containerNode.path("tags");

        if (tagsNode.isArray()) {
          for (JsonNode tagNode : tagsNode) {
            String tag = tagNode.asText();
            versions.add(tag);
          }
        }
      }
    }

    if (versions.isEmpty()) {
      logger.warn("No versions found for {}", source);
      versionCache.put(cacheKey, Collections.emptyList());
      return Collections.emptyList();
    }

    // Parse and sort versions
    List<Version> semVerList =
        versions.stream()
            .map(SemanticVersionUtil::parseVersion)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    Collections.sort(semVerList, Collections.reverseOrder());

    List<String> sortedVersionStrings = semVerList.stream().map(Version::toString).toList();

    logger.info(
        "Found and sorted {} versions for GitHub OCI chart: {}",
        sortedVersionStrings.size(),
        source);
    versionCache.put(cacheKey, Collections.unmodifiableList(new ArrayList<>(sortedVersionStrings)));
    // log versions for temp debugging
    logger.info("Versions: {}", sortedVersionStrings);

    return sortedVersionStrings;
  }

  @Override
  public boolean supports(AppArtifact artifact) {
    return artifact != null
        && "helm".equalsIgnoreCase(artifact.getArtifactType())
        && artifact.getSource() != null
        && artifact.getSource().startsWith("oci://ghcr.io/");
  }
}
