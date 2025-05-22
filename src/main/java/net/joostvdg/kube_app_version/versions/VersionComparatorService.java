package net.joostvdg.kube_app_version.versions;

import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.CollectorService;
import net.joostvdg.kube_app_version.versions.dto.OutdatedArtifactInfo; // Import the DTO
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil; // Placeholder for SemVer
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // Keep for getAvailableVersionsForAllAppArtifacts if still used
import java.util.List;
import java.util.Map; // Keep for getAvailableVersionsForAllAppArtifacts if still used
import java.util.Set;

@Service
public class VersionComparatorService {

    private static final Logger logger = LoggerFactory.getLogger(VersionComparatorService.class);
    private final CollectorService collectorService;
    private final List<VersionFetcher> versionFetchers;

    public VersionComparatorService(CollectorService collectorService, List<VersionFetcher> versionFetchers) {
        this.collectorService = collectorService;
        this.versionFetchers = versionFetchers;
        logger.info("VersionComparatorService initialized with {} version fetchers.", versionFetchers.size());
    }

    public List<OutdatedArtifactInfo> getOutdatedArtifacts() {
        List<OutdatedArtifactInfo> outdatedList = new ArrayList<>();
        Set<App> apps = collectorService.getAllCollectedApps();

        for (App app : apps) {
            for (AppVersion appVersion : app.getVersions()) {
                for (AppArtifact artifact : appVersion.getArtifacts()) {
                    String currentArtifactVersion = determineCurrentArtifactVersion(artifact, appVersion);

                    if (currentArtifactVersion == null || "unknown".equalsIgnoreCase(currentArtifactVersion)) {
                        logger.debug("Could not determine a valid current version for artifact {} (type: {}) in app {}, skipping comparison.",
                                artifact.getSource(), artifact.getArtifactType(), app.getName());
                        continue;
                    }

                    for (VersionFetcher fetcher : versionFetchers) {
                        if (fetcher.supports(artifact)) {
                            try {
                                logger.debug("Fetching available versions for artifact: {} (type: {}) using {}",
                                        artifact.getSource(), artifact.getArtifactType(), fetcher.getClass().getSimpleName());
                                List<String> availableVersions = fetcher.getAvailableVersions(artifact);

                                if (availableVersions == null || availableVersions.isEmpty()) {
                                    logger.debug("No available versions found for artifact {}", artifact.getSource());
                                    continue; // No versions to compare against
                                }

                                // The HelmChartVersionFetcher sorts latest first. Other fetchers should too,
                                // or getLatestVersion needs to implement robust sorting.
                                String latestAvailableVersion = SemanticVersionUtil.getLatestVersion(availableVersions);

                                if (latestAvailableVersion == null) {
                                    logger.warn("Could not determine the latest version from available versions for artifact {}", artifact.getSource());
                                    continue;
                                }

                                logger.debug("Comparing current artifact version '{}' with latest available '{}' for {} ({})",
                                        currentArtifactVersion, latestAvailableVersion, artifact.getSource(), artifact.getArtifactType());

                                if (SemanticVersionUtil.isOutdated(currentArtifactVersion, latestAvailableVersion)) {
                                    logger.info("Artifact outdated: App: '{}', Artifact: '{}', Current: '{}', Latest: '{}'",
                                            app.getName(), artifact.getSource(), currentArtifactVersion, latestAvailableVersion);
                                    outdatedList.add(new OutdatedArtifactInfo(
                                            app.getName(),
                                            app.getId(),
                                            appVersion.getVersion(), // Overall deployed version of the app
                                            artifact.getSource(),
                                            artifact.getArtifactType(),
                                            currentArtifactVersion,
                                            latestAvailableVersion,
                                            availableVersions // Include all available for context
                                    ));
                                }
                            } catch (Exception e) {
                                logger.error("Error fetching or comparing versions for artifact {}: {}", artifact.getSource(), e.getMessage(), e);
                            }
                            break; // Found a supporting fetcher, no need to check others for this artifact
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
                // For Helm and Git, the AppVersion.version (from Argo's targetRevision/sync.revision)
                // is considered the current version of that source artifact.
                return appVersion.getVersion();
            case "containerimage":
                String source = artifact.getSource();
                if (source == null) return null;
                // Naive parsing: assumes "image:tag" or "image@digest"
                // Prefers tag if both colon and @ are present (though unlikely in a single source string from Argo)
                int colonIndex = source.lastIndexOf(':');
                int atIndex = source.lastIndexOf('@'); // Digests are usually for immutability, not "latest" checks

                if (colonIndex > 0 && (atIndex == -1 || colonIndex > atIndex)) {
                    return source.substring(colonIndex + 1);
                } else if (atIndex > 0 && colonIndex == -1) { // Only digest
                    logger.debug("Artifact {} uses a digest '{}'. Digest comparison is not typical for 'latest version' checks.", source, source.substring(atIndex + 1));
                    return null; // Or return the digest if you have a way to compare it, but usually we compare tags.
                }
                logger.warn("Could not parse version tag from containerImage source: {}", source);
                return null;
            default:
                logger.debug("Unsupported artifact type for version determination: {}", artifactType);
                return null;
        }
    }


    // This method might still be useful for other purposes or direct queries
    public Map<String, List<String>> getAvailableVersionsForAllAppArtifacts() {
        Set<App> apps = collectorService.getAllCollectedApps();
        Map<String, List<String>> artifactVersionsMap = new HashMap<>(); // Renamed for clarity

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