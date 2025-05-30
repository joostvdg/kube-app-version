/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DockerHubOciHelmChartVersionFetcher implements VersionFetcher {

  private static final Logger logger =
      LoggerFactory.getLogger(DockerHubOciHelmChartVersionFetcher.class);
  private final HttpClient httpClient;
  private final Map<String, List<String>> versionCache = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Pattern DOCKERHUB_OCI_PATTERN =
      Pattern.compile("^oci://([^/]+\\.docker\\.io)/(.+)$");
  private static final String AUTH_SERVICE = "registry.docker.io";

  public DockerHubOciHelmChartVersionFetcher() {
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  @Override
  @SuppressWarnings("MixedMutabilityReturnType") // not using Guava you stupid parser
  public List<String> getAvailableVersions(AppArtifact artifact) throws Exception {
    if (!supports(artifact)) {
      logger.warn(
          "DockerHubOciHelmChartVersionFetcher does not support artifact: {}",
          artifact.getSource());
      return Collections.unmodifiableList(new ArrayList<>());
    }

    String source = artifact.getSource();
    String cacheKey = source;

    List<String> cachedVersions = versionCache.get(cacheKey);
    if (cachedVersions != null) {
      logger.debug("Returning cached versions for {}", cacheKey);
      return new ArrayList<>(cachedVersions);
    }

    Matcher matcher = DOCKERHUB_OCI_PATTERN.matcher(source);
    if (!matcher.matches()) {
      logger.error("Failed to parse Docker Hub OCI URL: {}", source);
      return Collections.emptyList();
    }

    String registryDomain = matcher.group(1);
    String repositoryPath = matcher.group(2);

    // Step 1: Get authentication token
    String authScope = "repository:" + repositoryPath + ":pull";
    String authUrl =
        String.format("https://auth.docker.io/token?service=%s&scope=%s", AUTH_SERVICE, authScope);

    logger.debug("Getting Docker Hub auth token from: {}", authUrl);

    HttpRequest tokenRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(authUrl))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15))
            .build();

    HttpResponse<String> tokenResponse;
    try {
      tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      logger.error("Failed to get Docker Hub auth token: {}", e.getMessage(), e);
      throw new Exception("Failed to get Docker Hub auth token: " + e.getMessage(), e);
    }

    if (tokenResponse.statusCode() != 200) {
      logger.error(
          "Failed to get Docker Hub auth token. HTTP status: {} - {}",
          tokenResponse.statusCode(),
          tokenResponse.body());
      throw new RuntimeException(
          "Failed to get Docker Hub auth token. HTTP status: " + tokenResponse.statusCode());
    }

    JsonNode tokenJson = objectMapper.readTree(tokenResponse.body());
    String token = tokenJson.path("token").asText();

    if (token == null || token.isEmpty()) {
      logger.error("Empty token received from Docker Hub auth");
      throw new RuntimeException("Empty token received from Docker Hub auth");
    }

    // Step 2: Get tags list using the token
    String tagsUrl = String.format("https://%s/v2/%s/tags/list", registryDomain, repositoryPath);

    logger.debug("Fetching Docker Hub OCI tags from: {}", tagsUrl);

    HttpRequest tagsRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(tagsUrl))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(15))
            .build();

    HttpResponse<String> tagsResponse;
    try {
      tagsResponse = httpClient.send(tagsRequest, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      logger.error("Failed to get Docker Hub tags: {}", e.getMessage(), e);
      throw new Exception("Failed to get Docker Hub tags: " + e.getMessage(), e);
    }

    if (tagsResponse.statusCode() != 200) {
      logger.error(
          "Failed to get Docker Hub tags. HTTP status: {} - {}",
          tagsResponse.statusCode(),
          tagsResponse.body());
      throw new RuntimeException(
          "Failed to get Docker Hub tags. HTTP status: " + tagsResponse.statusCode());
    }

    // Parse the tags from the response
    JsonNode tagsJson = objectMapper.readTree(tagsResponse.body());
    JsonNode tagsNode = tagsJson.path("tags");

    List<String> versions = new ArrayList<>();
    if (tagsNode.isArray()) {
      for (JsonNode tagNode : tagsNode) {
        String tag = tagNode.asText();
        if (tag != null && !tag.isEmpty()) {
          versions.add(tag);
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

    List<String> sortedVersionStrings =
        semVerList.stream().map(Version::toString).collect(Collectors.toList());

    logger.info(
        "Found and sorted {} versions for Docker Hub OCI chart: {}",
        sortedVersionStrings.size(),
        source);
    versionCache.put(cacheKey, Collections.unmodifiableList(new ArrayList<>(sortedVersionStrings)));
    logger.info("Versions: {}", sortedVersionStrings);

    return sortedVersionStrings;
  }

  @Override
  public boolean supports(AppArtifact artifact) {
    return artifact != null
        && "helm".equalsIgnoreCase(artifact.getArtifactType())
        && artifact.getSource() != null
        && DOCKERHUB_OCI_PATTERN.matcher(artifact.getSource()).matches();
  }
}
