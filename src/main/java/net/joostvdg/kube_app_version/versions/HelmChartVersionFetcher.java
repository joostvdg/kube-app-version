/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import com.github.zafarkhaja.semver.Version;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

@Service
public class HelmChartVersionFetcher implements VersionFetcher {

  private static final Logger logger = LoggerFactory.getLogger(HelmChartVersionFetcher.class);
  private final HttpClient httpClient;
  private final Map<String, List<String>> versionCache = new ConcurrentHashMap<>();

  // XY_PRERELEASE_PATTERN is removed as normalization is now in SemanticVersionUtil

  public HelmChartVersionFetcher() {
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  private String[] parseHelmSource(String source) {
    if (source == null || source.isEmpty()) {
      logger.warn("Helm source string is null or empty.");
      return null;
    }
    int lastSlashIndex = source.lastIndexOf('/');
    if (lastSlashIndex == -1 || lastSlashIndex == 0 || lastSlashIndex == source.length() - 1) {
      logger.warn("Invalid Helm source format: '{}'. Expected 'repoUrl/chartName'.", source);
      return null;
    }
    String repoUrl = source.substring(0, lastSlashIndex);
    String chartName = source.substring(lastSlashIndex + 1);
    return new String[] {repoUrl, chartName};
  }

  // parseAndNormalizeVersionString method is REMOVED from here.

  @Override
  @SuppressWarnings("MixedMutabilityReturnType") // not using Guava you stupid parser
  public List<String> getAvailableVersions(AppArtifact artifact) throws Exception {
    if (!supports(artifact)) {
      logger.warn(
          "HelmChartVersionFetcher does not support artifact type: {}", artifact.getArtifactType());
      return Collections.emptyList();
    }

    String[] parsedSource = parseHelmSource(artifact.getSource());
    if (parsedSource == null) {
      return Collections.emptyList();
    }
    String repoUrl = parsedSource[0];
    String chartName = parsedSource[1];
    String cacheKey = repoUrl + "/" + chartName;

    List<String> cachedVersions = versionCache.get(cacheKey);
    if (cachedVersions != null) {
      logger.debug("Returning cached versions for {}", cacheKey);
      return List.copyOf(cachedVersions);
    }

    logger.debug("No cache hit for {}. Fetching from remote.", cacheKey);
    String indexFileUrl = repoUrl.endsWith("/") ? repoUrl + "index.yaml" : repoUrl + "/index.yaml";
    logger.debug("Fetching Helm index file from: {}", indexFileUrl);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(indexFileUrl))
            .header("Accept", "application/yaml, text/yaml, */*")
            .timeout(Duration.ofSeconds(10))
            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      logger.error("Failed to send request to {}: {}", indexFileUrl, e.getMessage(), e);
      throw new Exception("Failed to send request to " + indexFileUrl + ": " + e.getMessage(), e);
    }

    if (response.statusCode() != 200) {
      logger.error(
          "Failed to fetch {}. HTTP status: {} - {}",
          indexFileUrl,
          response.statusCode(),
          response.body());
      throw new RuntimeException(
          "Failed to fetch " + indexFileUrl + ". HTTP status: " + response.statusCode());
    }

    String yamlContent = response.body();
    Yaml yaml = new Yaml();
    Map<String, Object> indexData;
    try {
      indexData = yaml.load(yamlContent);
    } catch (YAMLException e) {
      logger.error("Failed to parse YAML from {}: {}", indexFileUrl, e.getMessage(), e);
      throw new Exception("Failed to parse YAML from " + indexFileUrl + ": " + e.getMessage(), e);
    }

    List<String> rawVersions = new ArrayList<>();
    if (indexData == null || !indexData.containsKey("entries")) {
      logger.warn(
          "YAML content from {} does not contain 'entries' or is null. Caching empty list.",
          indexFileUrl);
      versionCache.put(cacheKey, Collections.emptyList());
      return Collections.emptyList();
    }

    Object entriesObject = indexData.get("entries");
    if (!(entriesObject instanceof Map)) {
      logger.warn("'entries' in YAML from {} is not a Map. Caching empty list.", indexFileUrl);
      versionCache.put(cacheKey, Collections.emptyList());
      return Collections.emptyList();
    }
    Map<String, Object> entries = (Map<String, Object>) entriesObject;

    if (entries.containsKey(chartName)) {
      Object chartEntriesObject = entries.get(chartName);
      if (!(chartEntriesObject instanceof List)) {
        logger.warn(
            "Chart entry for '{}' in YAML from {} is not a List. Caching empty list.",
            chartName,
            indexFileUrl);
        versionCache.put(cacheKey, Collections.emptyList());
        return Collections.emptyList();
      }
      List<Map<String, Object>> chartVersionEntries =
          (List<Map<String, Object>>) chartEntriesObject;

      for (Map<String, Object> chartVersionEntry : chartVersionEntries) {
        if (chartVersionEntry != null && chartVersionEntry.containsKey("version")) {
          Object versionObj = chartVersionEntry.get("version");
          if (versionObj != null) {
            rawVersions.add(versionObj.toString());
          }
        }
      }
    } else {
      logger.warn("Chart '{}' not found in {}. Caching empty list.", chartName, indexFileUrl);
      versionCache.put(cacheKey, Collections.emptyList());
      return Collections.emptyList();
    }

    if (rawVersions.isEmpty()) {
      logger.info(
          "No raw versions found for chart '{}' in repo '{}'. Caching empty list.",
          chartName,
          repoUrl);
      versionCache.put(cacheKey, Collections.emptyList());
      return Collections.emptyList();
    }

    // Use SemanticVersionUtil for parsing and normalization
    List<Version> semVerList =
        rawVersions.stream()
            .map(SemanticVersionUtil::parseVersion) // Returns Optional<Version>
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    Collections.sort(semVerList, Collections.reverseOrder()); // Sorts descending (latest first)

    List<String> sortedVersionStrings =
        semVerList.stream()
            .map(Version::toString) // This will give the canonical string form
            .collect(Collectors.toList());

    logger.info(
        "Found and sorted {} versions for chart '{}' in repo '{}'. Caching result.",
        sortedVersionStrings.size(),
        chartName,
        repoUrl);
    versionCache.put(cacheKey, Collections.unmodifiableList(new ArrayList<>(sortedVersionStrings)));

    return sortedVersionStrings;
  }

  @Override
  public boolean supports(AppArtifact artifact) {
    return artifact != null
        && "helm".equalsIgnoreCase(artifact.getArtifactType())
        && artifact.getSource() != null
        && !artifact.getSource().startsWith("oci://");
  }
}
