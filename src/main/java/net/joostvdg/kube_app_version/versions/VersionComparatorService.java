package net.joostvdg.kube_app_version.versions;

import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.CollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // This method will eventually return a more structured comparison result.
    // For now, it demonstrates fetching available versions.
    public Map<String, List<String>> getAvailableVersionsForAllAppArtifacts() {
        Set<App> apps = collectorService.getAllCollectedApps();
        Map<String, List<String>> artifactVersions = new HashMap<>();

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
                                logger.info("Fetching versions for artifact: {} (type: {}) using {}",
                                        artifact.getSource(), artifact.getArtifactType(), fetcher.getClass().getSimpleName());
                                List<String> availableVersions = fetcher.getAvailableVersions(artifact);
                                // Using artifact source as key, could be more specific if needed
                                artifactVersions.put(app.getName() + "::" + artifact.getSource(), availableVersions);
                                logger.debug("Available versions for {}: {}", artifact.getSource(), availableVersions);
                            } catch (Exception e) {
                                logger.error("Error fetching versions for artifact {}: {}", artifact.getSource(), e.getMessage(), e);
                                artifactVersions.put(app.getName() + "::" + artifact.getSource() + "::ERROR", List.of(e.getMessage()));
                            }
                            break; // Found a supporting fetcher
                        }
                    }
                }
            }
        }
        return artifactVersions;
    }

    // You might want to add a new endpoint in a controller to expose this data.
    // For example, in a new VersionController:
    // @GetMapping("/api/versions/available")
    // public Map<String, List<String>> getAllAvailableVersions() {
    //     return versionComparatorService.getAvailableVersionsForAllAppArtifacts();
    // }
}