package net.joostvdg.kube_app_version.versions;

import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.CollectorService;
import net.joostvdg.kube_app_version.versions.dto.OutdatedArtifactInfo;
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*; // For Optional

@Service
public class VersionComparatorService {

    private static final Logger logger = LoggerFactory.getLogger(VersionComparatorService.class);
    private final CollectorService collectorService;
    private final List<VersionFetcher> versionFetchers;

    public VersionComparatorService(CollectorService collectorService, List<VersionFetcher> versionFetchers) {
        this.collectorService = collectorService;
        this.versionFetchers = versionFetchers;
        logger.info("VersionComparatorService initialized with {} version fetchers.", Optional.of(versionFetchers.size()));
    }

    public List<OutdatedArtifactInfo> getOutdatedArtifacts() {
        List<OutdatedArtifactInfo> outdatedList = new ArrayList<>();
        Set<App> apps = collectorService.getAllCollectedApps();

        for (App app : apps) {
            for (AppVersion appVersion : app.getVersions()) {
                for (AppArtifact artifact : appVersion.getArtifacts()) {
                    String currentArtifactVersionStr = determineCurrentArtifactVersion(artifact, appVersion);

                    if (currentArtifactVersionStr == null || "unknown".equalsIgnoreCase(currentArtifactVersionStr)) {
                        logger.debug("Could not determine a valid current version for artifact {} (type: {}) in app {}, skipping comparison.",
                                artifact.getSource(), artifact.getArtifactType(), app.getName());
                        continue;
                    }

                    // Count number of dots, if we only have X.Y, we add .0 at the end
                    int dotCount = currentArtifactVersionStr.split("\\.").length;
                    if (dotCount == 1) {
                        // verify we don't have a -abc
                        if (currentArtifactVersionStr.contains("-")) {
                            // find the index of the hyphen, collect it as a suffix, add the .0 before the index of - and then add the suffix back
                            int hyphenIndex = currentArtifactVersionStr.indexOf('-');
                            String suffix = currentArtifactVersionStr.substring(hyphenIndex);
                            currentArtifactVersionStr = currentArtifactVersionStr.substring(0, hyphenIndex);
                            currentArtifactVersionStr += ".0" + suffix;
                        }
                        currentArtifactVersionStr += ".0";
                    }


                    Optional<com.github.zafarkhaja.semver.Version> currentParsedVersionOpt = SemanticVersionUtil.parseVersion(currentArtifactVersionStr);


                    for (VersionFetcher fetcher : versionFetchers) {
                        if (fetcher.supports(artifact)) {
                            try {
                                logger.debug("Fetching available versions for artifact: {} (type: {}) using {}",
                                        artifact.getSource(), artifact.getArtifactType(), fetcher.getClass().getSimpleName());
                                List<String> availableVersions = fetcher.getAvailableVersions(artifact);

                                if (availableVersions == null || availableVersions.isEmpty()) {
                                    logger.debug("No available versions found for artifact {}", artifact.getSource());
                                    continue;
                                }

                                Optional<com.github.zafarkhaja.semver.Version> latestOverallOpt = SemanticVersionUtil.getLatestOverallVersion(availableVersions);
                                Optional<com.github.zafarkhaja.semver.Version> latestGAOpt = SemanticVersionUtil.getLatestGARelease(availableVersions);
                                Optional<com.github.zafarkhaja.semver.Version> latestPreOpt = SemanticVersionUtil.getLatestPreRelease(availableVersions);

                                String latestOverallVersionStr = latestOverallOpt.map(Object::toString).orElse(null);
                                String latestGAVersionStr = latestGAOpt.map(Object::toString).orElse(null);
                                String latestPreVersionStr = latestPreOpt.map(Object::toString).orElse(null);

                                Optional<String> nextMinorOpt = SemanticVersionUtil.findNextMinorVersion(currentArtifactVersionStr, availableVersions);
                                Optional<String> nextMajorOpt = SemanticVersionUtil.findNextMajorVersion(currentArtifactVersionStr, availableVersions);

                                // Calculate Deltas
                                Optional<Long> minorDeltaOpt = SemanticVersionUtil.calculateMinorVersionDelta(currentParsedVersionOpt, nextMinorOpt);
                                Optional<Long> majorDeltaOpt = SemanticVersionUtil.calculateMajorVersionDelta(currentParsedVersionOpt, nextMajorOpt);

                                // Determine if outdated primarily based on latest GA release
                                boolean isOutdated = false;
                                if (latestGAVersionStr != null) {
                                    isOutdated = SemanticVersionUtil.isOutdated(currentArtifactVersionStr, latestGAVersionStr);
                                } else if (latestOverallVersionStr != null) {
                                    isOutdated = SemanticVersionUtil.isOutdated(currentArtifactVersionStr, latestOverallVersionStr);
                                    if (isOutdated) {
                                        logger.info("Artifact {} is outdated against latest overall (pre-release: {}), as no GA was found.", artifact.getSource(), latestOverallVersionStr);
                                    }
                                }


                                if (isOutdated) { // Only add to list if outdated
                                    logger.info("Artifact outdated: App: '{}', Artifact: '{}', Current: '{}', Latest GA: '{}', Major Delta: {}, Minor Delta: {}, Next Minor: {}, Next Major: {}",
                                            app.getName(), artifact.getSource(), currentArtifactVersionStr, latestGAVersionStr, majorDeltaOpt.orElse(null), minorDeltaOpt.orElse(null), nextMinorOpt.orElse(null), nextMajorOpt.orElse(null));
                                    outdatedList.add(new OutdatedArtifactInfo(
                                            app.getName(),
                                            app.getId(),
                                            appVersion.getVersion(),
                                            artifact.getSource(),
                                            artifact.getArtifactType(),
                                            currentArtifactVersionStr,
                                            latestOverallVersionStr,
                                            latestGAVersionStr,
                                            latestPreVersionStr,
                                            nextMinorOpt.orElse(null),
                                            nextMajorOpt.orElse(null),
                                            majorDeltaOpt.orElse(null), // Add delta
                                            minorDeltaOpt.orElse(null), // Add delta
                                            availableVersions
                                    ));
                                }
                            } catch (Exception e) {
                                logger.error("Error fetching or comparing versions for artifact {}: {}", artifact.getSource(), e.getMessage(), e);
                            }
                            break; // Found a supporting fetcher
                        }
                    }
                }
            }
        }
        return outdatedList;
    }

    private String determineCurrentArtifactVersion(AppArtifact artifact, AppVersion appVersion) {
        String artifactType = artifact.getArtifactType();
        if (artifactType == null) return null;

        switch (artifactType.toLowerCase()) {
            case "helm":
            case "git":
                return appVersion.getVersion();
            case "containerimage":
                String source = artifact.getSource();
                if (source == null) return null;
                int colonIndex = source.lastIndexOf(':');
                int atIndex = source.lastIndexOf('@');

                if (colonIndex > 0 && (atIndex == -1 || colonIndex > atIndex)) {
                    String tag = source.substring(colonIndex + 1);
                    return SemanticVersionUtil.parseVersion(tag)
                            .map(com.github.zafarkhaja.semver.Version::toString)
                            .orElse(tag);
                } else if (atIndex > 0 && colonIndex == -1) {
                    logger.debug("Artifact {} uses a digest '{}'. Digest comparison is not typical for 'latest version' checks.", source, source.substring(atIndex + 1));
                    return null;
                }
                logger.warn("Could not parse version tag from containerImage source: {}", source);
                return null;
            default:
                logger.debug("Unsupported artifact type for version determination: {}", artifactType);
                return null;
        }
    }

    public Map<String, List<String>> getAvailableVersionsForAllAppArtifacts() {
        Set<App> apps = collectorService.getAllCollectedApps();
        Map<String, List<String>> artifactVersionsMap = new HashMap<>();

        for (App app : apps) {
            for (AppVersion appVersion : app.getVersions()) {
                for (AppArtifact artifact : appVersion.getArtifacts()) {
                    if (artifact.getSource() == null || artifact.getArtifactType() == null) {
                        logger.debug("Skipping artifact for app '{}' due to missing source or type: {}", app.getName(), artifact);
                        continue;
                    }

                    for (VersionFetcher fetcher : versionFetchers) {
                        if (fetcher.supports(artifact)) {
                            try {
                                String mapKey = app.getName() + "::" + artifact.getArtifactType() + "::" + artifact.getSource();
                                List<String> availableVersions = fetcher.getAvailableVersions(artifact);
                                artifactVersionsMap.put(mapKey, availableVersions);
                            } catch (Exception e) {
                                String errorKey = app.getName() + "::" + artifact.getArtifactType() + "::" + artifact.getSource() + "::ERROR";
                                artifactVersionsMap.put(errorKey, List.of(e.getMessage()));
                                logger.error("Error fetching versions for artifact {}: {}", artifact.getSource(), e.getMessage(), e);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return artifactVersionsMap;
    }
}