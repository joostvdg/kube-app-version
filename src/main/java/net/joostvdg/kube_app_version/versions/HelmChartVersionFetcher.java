package net.joostvdg.kube_app_version.versions;

import com.github.zafarkhaja.semver.Version;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HelmChartVersionFetcher implements VersionFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HelmChartVersionFetcher.class);
    private final HttpClient httpClient;
    private final Map<String, List<String>> versionCache = new ConcurrentHashMap<>();

    // Pattern to identify versions like "X.Y-prerelease" (e.g., "1.2-alpha", "10.0-beta.1")
    // It captures X.Y in group 1 and the pre-release part in group 2.
    private static final Pattern XY_PRERELEASE_PATTERN = Pattern.compile("^(\\d+\\.\\d+)-([a-zA-Z0-9][a-zA-Z0-9.-]*)$");


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

    private Version parseAndNormalizeVersionString(String versionStr, String chartNameForLogging) {
        if (versionStr == null) {
            logger.warn("Chart '{}': Null version string encountered. Skipping.", chartNameForLogging);
            return null;
        }

        String originalVersionString = versionStr;
        String processedString = versionStr;

        if (processedString.startsWith("v")) {
            processedString = processedString.substring(1);
        }

        try {
            // Attempt direct parsing first
            var possibleVersion = Version.tryParse(processedString, false);
            if (possibleVersion != null && possibleVersion.isPresent()) {
                return possibleVersion.get();
            } else {
                logger.warn("Chart '{}': Invalid semantic version format '{}'. Skipping.", chartNameForLogging, originalVersionString);
                return null;
            }
        } catch (IllegalArgumentException e1) {
            // Direct parsing failed, try to normalize if it matches X.Y-prerelease pattern
            Matcher matcher = XY_PRERELEASE_PATTERN.matcher(processedString);
            if (matcher.matches()) {
                String majorMinor = matcher.group(1); // X.Y
                String prerelease = matcher.group(2); // prerelease part
                String normalizedVersion = majorMinor + ".0-" + prerelease;
                logger.debug("Chart '{}': Normalizing version '{}' (original: '{}') to '{}'",
                        chartNameForLogging, processedString, originalVersionString, normalizedVersion);
                try {
                    return Version.valueOf(normalizedVersion);
                } catch (IllegalArgumentException e2) {
                    logger.warn("Chart '{}': Invalid semantic version format after normalization. Original: '{}', Cleaned: '{}', Normalized: '{}'. Skipping. Error: {}",
                            chartNameForLogging, originalVersionString, processedString, normalizedVersion, e2.getMessage());
                    return null;
                }
            } else {
                // Did not match X.Y-prerelease pattern, so the original error stands
                logger.warn("Chart '{}': Invalid semantic version format. Original: '{}', Cleaned: '{}'. Skipping. Error: {}",
                        chartNameForLogging, originalVersionString, processedString, e1.getMessage());
                return null;
            }
        }
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

        List<Version> semVerList = rawVersions.stream()
                .map(s -> parseAndNormalizeVersionString(s, chartName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collections.sort(semVerList, Collections.reverseOrder()); // Sorts descending (latest first)

        List<String> sortedVersionStrings = semVerList.stream()
                .map(Version::toString) // This will give the canonical string form (without 'v')
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

            // Test case for X.Y-prerelease
            AppArtifact testXYPrerelease = new AppArtifact();
            testXYPrerelease.setArtifactType("helm");
            // Simulate a chart that might have such versions (actual repo might not)
            // For testing, we'd ideally mock the HTTP response or have a local test index.yaml
            logger.info("Simulating fetch for a chart with X.Y-prerelease style versions (e.g., mychart/mychartname)");
            // Manually testing the parser function:
            Version parsedVer = fetcher.parseAndNormalizeVersionString("1.2-alpha", "mychart");
            logger.info("Parsed '1.2-alpha': {}", parsedVer != null ? parsedVer.toString() : "null");
            parsedVer = fetcher.parseAndNormalizeVersionString("v0.5-beta.2", "mychart");
            logger.info("Parsed 'v0.5-beta.2': {}", parsedVer != null ? parsedVer.toString() : "null");
            parsedVer = fetcher.parseAndNormalizeVersionString("1.2.3-rc1", "mychart");
            logger.info("Parsed '1.2.3-rc1': {}", parsedVer != null ? parsedVer.toString() : "null");
            parsedVer = fetcher.parseAndNormalizeVersionString("1.2.3", "mychart");
            logger.info("Parsed '1.2.3': {}", parsedVer != null ? parsedVer.toString() : "null");
            parsedVer = fetcher.parseAndNormalizeVersionString("invalid-version", "mychart");
            logger.info("Parsed 'invalid-version': {}", parsedVer != null ? parsedVer.toString() : "null");


        } catch (Exception e) {
            logger.error("Error fetching chart versions: {}", e.getMessage(), e);
        }
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