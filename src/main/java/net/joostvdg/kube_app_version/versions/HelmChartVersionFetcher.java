package net.joostvdg.kube_app_version.versions;

import net.joostvdg.kube_app_version.api.model.AppArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class HelmChartVersionFetcher implements VersionFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HelmChartVersionFetcher.class);
    private final HttpClient httpClient;

    public HelmChartVersionFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10)) // Add a connection timeout
                .build();
    }

    /**
     * Parses the source string (expected format: repoUrl/chartName) into repository URL and chart name.
     * @param source The source string.
     * @return A String array where [0] is repoUrl and [1] is chartName, or null if parsing fails.
     */
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
        return new String[]{repoUrl, chartName};
    }

    @Override
    @SuppressWarnings("unchecked") // For casting from Yaml parsing
    public List<String> getAvailableVersions(AppArtifact artifact) throws Exception {
        if (!supports(artifact)) {
            logger.warn("HelmChartVersionFetcher does not support artifact type: {}", artifact.getArtifactType());
            return Collections.emptyList();
        }

        String[] parsedSource = parseHelmSource(artifact.getSource());
        if (parsedSource == null) {
            return Collections.emptyList();
        }
        String repoUrl = parsedSource[0];
        String chartName = parsedSource[1];

        String indexFileUrl = repoUrl.endsWith("/") ? repoUrl + "index.yaml" : repoUrl + "/index.yaml";
        logger.debug("Fetching Helm index file from: {}", indexFileUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexFileUrl))
                .header("Accept", "application/yaml, text/yaml, */*")
                .timeout(Duration.ofSeconds(10)) // Add a request timeout
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logger.error("Failed to send request to {}: {}", indexFileUrl, e.getMessage(), e);
            throw new Exception("Failed to send request to " + indexFileUrl + ": " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            logger.error("Failed to fetch {}. HTTP status: {} - {}", indexFileUrl, response.statusCode(), response.body());
            throw new RuntimeException("Failed to fetch " + indexFileUrl + ". HTTP status: " + response.statusCode());
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

        List<String> versions = new ArrayList<>();
        if (indexData == null || !indexData.containsKey("entries")) {
            logger.warn("YAML content from {} does not contain 'entries' or is null.", indexFileUrl);
            return Collections.emptyList();
        }

        Object entriesObject = indexData.get("entries");
        if (!(entriesObject instanceof Map)) {
            logger.warn("'entries' in YAML from {} is not a Map.", indexFileUrl);
            return Collections.emptyList();
        }
        Map<String, Object> entries = (Map<String, Object>) entriesObject;

        if (entries.containsKey(chartName)) {
            Object chartEntriesObject = entries.get(chartName);
            if (!(chartEntriesObject instanceof List)) {
                logger.warn("Chart entry for '{}' in YAML from {} is not a List.", chartName, indexFileUrl);
                return Collections.emptyList();
            }
            List<Map<String, Object>> chartVersionEntries = (List<Map<String, Object>>) chartEntriesObject;

            for (Map<String, Object> chartVersionEntry : chartVersionEntries) {
                if (chartVersionEntry != null && chartVersionEntry.containsKey("version")) {
                    Object versionObj = chartVersionEntry.get("version");
                    if (versionObj != null) {
                        versions.add(versionObj.toString());
                    }
                }
            }
            logger.info("Found {} versions for chart '{}' in repo '{}'", versions.size(), chartName, repoUrl);
        } else {
            logger.warn("Chart '{}' not found in {}", chartName, indexFileUrl);
            return Collections.emptyList();
        }

        // Consider using a semantic version comparator for more accurate sorting if needed
        versions.sort(Collections.reverseOrder());
        return versions;
    }

    @Override
    public boolean supports(AppArtifact artifact) {
        return artifact != null && "helm".equalsIgnoreCase(artifact.getArtifactType());
    }

    // Main method can be kept for standalone testing during development
    public static void main(String[] args) {
        HelmChartVersionFetcher fetcher = new HelmChartVersionFetcher();
        AppArtifact testArtifact = new AppArtifact();
        testArtifact.setArtifactType("helm");
        // Example from your README
        testArtifact.setSource("https://charts.jetstack.io/cert-manager");
        // testArtifact.setSource("https://some-other-repo.com/my-chart"); // another example

        try {
            logger.info("Fetching versions for artifact source: {}", testArtifact.getSource());
            List<String> versions = fetcher.getAvailableVersions(testArtifact);
            if (versions.isEmpty()) {
                logger.info("No versions found for {}", testArtifact.getSource());
            } else {
                logger.info("Available versions for {}:", testArtifact.getSource());
                versions.forEach(version -> logger.info(" - {}", version));
            }
        } catch (Exception e) {
            logger.error("Error fetching chart versions: {}", e.getMessage(), e);
        }
    }
}