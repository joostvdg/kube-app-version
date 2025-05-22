package net.joostvdg.kube_app_version.versions;

import com.github.zafarkhaja.semver.Version;
// Make sure SemanticVersionUtil is imported if not in the same package,
// but it seems to be in net.joostvdg.kube_app_version.versions.util
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
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
import java.util.Objects;
import java.util.Optional; // Added for clarity, though stream handles it
import java.util.concurrent.ConcurrentHashMap;
// Pattern and Matcher are no longer needed here
import java.util.stream.Collectors;

@Service
public class HelmChartVersionFetcher implements VersionFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HelmChartVersionFetcher.class);
    private final HttpClient httpClient;
    private final Map<String, List<String>> versionCache = new ConcurrentHashMap<>();

    // XY_PRERELEASE_PATTERN is removed as normalization is now in SemanticVersionUtil

    public HelmChartVersionFetcher() {
        this.httpClient = HttpClient.newBuilder()
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
        return new String[]{repoUrl, chartName};
    }

    // parseAndNormalizeVersionString method is REMOVED from here.

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
        String cacheKey = repoUrl + "/" + chartName;

        List<String> cachedVersions = versionCache.get(cacheKey);
        if (cachedVersions != null) {
            logger.debug("Returning cached versions for {}", cacheKey);
            return new ArrayList<>(cachedVersions);
        }

        logger.debug("No cache hit for {}. Fetching from remote.", cacheKey);
        String indexFileUrl = repoUrl.endsWith("/") ? repoUrl + "index.yaml" : repoUrl + "/index.yaml";
        logger.debug("Fetching Helm index file from: {}", indexFileUrl);

        HttpRequest request = HttpRequest.newBuilder()
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

        List<String> rawVersions = new ArrayList<>();
        if (indexData == null || !indexData.containsKey("entries")) {
            logger.warn("YAML content from {} does not contain 'entries' or is null. Caching empty list.", indexFileUrl);
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
                logger.warn("Chart entry for '{}' in YAML from {} is not a List. Caching empty list.", chartName, indexFileUrl);
                versionCache.put(cacheKey, Collections.emptyList());
                return Collections.emptyList();
            }
            List<Map<String, Object>> chartVersionEntries = (List<Map<String, Object>>) chartEntriesObject;

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
            logger.info("No raw versions found for chart '{}' in repo '{}'. Caching empty list.", chartName, repoUrl);
            versionCache.put(cacheKey, Collections.emptyList());
            return Collections.emptyList();
        }

        // Use SemanticVersionUtil for parsing and normalization
        List<Version> semVerList = rawVersions.stream()
                .map(SemanticVersionUtil::parseVersion) // Returns Optional<Version>
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Collections.sort(semVerList, Collections.reverseOrder()); // Sorts descending (latest first)

        List<String> sortedVersionStrings = semVerList.stream()
                .map(Version::toString) // This will give the canonical string form
                .collect(Collectors.toList());

        logger.info("Found and sorted {} versions for chart '{}' in repo '{}'. Caching result.", sortedVersionStrings.size(), chartName, repoUrl);
        versionCache.put(cacheKey, Collections.unmodifiableList(new ArrayList<>(sortedVersionStrings)));

        return sortedVersionStrings;
    }

    @Override
    public boolean supports(AppArtifact artifact) {
        return artifact != null && "helm".equalsIgnoreCase(artifact.getArtifactType());
    }

    public static void main(String[] args) {
        HelmChartVersionFetcher fetcher = new HelmChartVersionFetcher();
        AppArtifact testArtifact = new AppArtifact();
        testArtifact.setArtifactType("helm");
        testArtifact.setSource("https://charts.jetstack.io/cert-manager");

        try {
            logger.info("Fetching versions for artifact source: {}", testArtifact.getSource());
            List<String> versions = fetcher.getAvailableVersions(testArtifact);
            logVersions(testArtifact.getSource(), versions);

            logger.info("Fetching versions again for artifact source (should hit cache): {}", testArtifact.getSource());
            versions = fetcher.getAvailableVersions(testArtifact);
            logVersions(testArtifact.getSource(), versions);

            AppArtifact anotherArtifact = new AppArtifact();
            anotherArtifact.setArtifactType("helm");
            anotherArtifact.setSource("https://prometheus-community.github.io/helm-charts/kube-prometheus-stack");
            logger.info("Fetching versions for artifact source: {}", anotherArtifact.getSource());
            List<String> versions2 = fetcher.getAvailableVersions(anotherArtifact);
            logVersions(anotherArtifact.getSource(), versions2);

            // Test cases for SemanticVersionUtil.parseVersion (which is now used internally)
            logger.info("Testing SemanticVersionUtil.parseVersion directly:");
            testParse("1.2-alpha");
            testParse("v0.5-beta.2");
            testParse("1.2.3-rc1");
            testParse("1.2.3");
            testParse("invalid-version");
            testParse("1.2"); // Test X.Y normalization
            testParse("v10.3"); // Test X.Y normalization with 'v'
            testParse("1"); // Should fail
            testParse("1.2.3.4"); // Should fail (SemVer only has 3 components + pre/build)
            testParse("1.2.3+build"); // Should parse
            testParse("1.2-alpha+build"); // Should parse (X.Y-prerelease normalized to X.Y.0-prerelease)

        } catch (Exception e) {
            logger.error("Error in main: {}", e.getMessage(), e);
        }
    }

    private static void testParse(String versionStr) {
        Optional<Version> parsedVer = SemanticVersionUtil.parseVersion(versionStr);
        logger.info("Parsed '{}': {}", versionStr, parsedVer.map(Version::toString).orElse("null (failed to parse)"));
    }

    private static void logVersions(String source, List<String> versions) {
        if (versions.isEmpty()) {
            logger.info("No versions found for {}", source);
        } else {
            logger.info("Available versions for {}: (Count: {})", source, versions.size());
            int limit = 5;
            if (versions.size() <= 2 * limit) {
                versions.forEach(version -> logger.info(" - {}", version));
            } else {
                for(int i=0; i < limit; i++) {
                    logger.info(" - {}", versions.get(i));
                }
                logger.info("   ... ({} more versions) ...", versions.size() - 2 * limit);
                for(int i = versions.size() - limit; i < versions.size(); i++) {
                    logger.info(" - {}", versions.get(i));
                }
            }
        }
    }
}