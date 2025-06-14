/* (C)2025 */
package net.joostvdg.kube_app_version.collectors.argo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.ApplicationCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ArgoCollector implements ApplicationCollector {
  private final Set<App> argoApps = Collections.synchronizedSet(new HashSet<>());
  private final Logger logger = LoggerFactory.getLogger(ArgoCollector.class);
  private final ApiClient apiClient;
  private final ArgoCollectorConfig config;
  private static final String AMSTERDAM_ZONE_ID = "Europe/Amsterdam";

  public ArgoCollector(ApiClient apiClient, ArgoCollectorConfig config) {
    this.apiClient = apiClient;
    this.config = config;
  }

  @PostConstruct
  private void init() {
    logger.info("Initializing Argo Collector");
    if (config.isRunOnStartup()) {
      try {
        collectArgoApplications();
      } catch (ApiException e) {
        logger.error(
            "Failed to collect Argo Applications during initialization: {}", e.getMessage(), e);
        throw new RuntimeException(e);
      }
    } else {
      logger.info("Skipping initial collection as configured (runOnStartup=false)");
    }
  }

  @Scheduled(
      fixedDelayString = "${argo.collector.collectionIntervalMinutes:15}",
      timeUnit = TimeUnit.MINUTES)
  public void scheduledCollection() {
    if (!config.isScheduledCollectionEnabled()) {
      logger.debug("Scheduled collection is disabled by configuration");
      return;
    }

    logger.info("Running scheduled Argo application collection");
    try {
      collectArgoApplications();
    } catch (ApiException e) {
      logger.error("Failed to collect Argo Applications in scheduled run: {}", e.getMessage(), e);
    }
  }

  private void collectArgoApplications() throws ApiException {
    List<DynamicKubernetesObject> argoAppList = fetchArgoApplications();

    // Clear previously collected apps
    argoApps.clear();

    if (argoAppList.isEmpty()) {
      logger.warn("No Argo Applications found.");
      return;
    }

    for (DynamicKubernetesObject argoAppCr : argoAppList) {
      App discoveredApp = processArgoApplication(argoAppCr);
      argoApps.add(discoveredApp);
    }

    logger.info("Finished collecting Argo applications. Total found: {}", argoApps.size());
  }

  private List<DynamicKubernetesObject> fetchArgoApplications() throws ApiException {
    DynamicKubernetesApi dynamicApi =
        new DynamicKubernetesApi("argoproj.io", "v1alpha1", "applications", apiClient);

    if (dynamicApi.list() == null
        || dynamicApi.list().getObject() == null
        || dynamicApi.list().getObject().getItems() == null) {
      return Collections.emptyList();
    }

    return dynamicApi.list().getObject().getItems();
  }

  private App processArgoApplication(DynamicKubernetesObject argoAppCr) {
    String appName = argoAppCr.getMetadata().getName();
    logger.info("Processing Argo Application: {}", appName);

    App discoveredApp = createBasicAppInfo(argoAppCr, appName);
    AppVersion currentVersion = createAppVersion(argoAppCr, appName, discoveredApp.getLabels());

    Set<AppVersion> appVersions = new HashSet<>();
    appVersions.add(currentVersion);
    discoveredApp.setVersions(appVersions);
    discoveredApp.setCurrentVersion(currentVersion);

    return discoveredApp;
  }

  private App createBasicAppInfo(DynamicKubernetesObject argoAppCr, String appName) {
    App discoveredApp = new App();
    discoveredApp.setId(argoAppCr.getMetadata().getUid());
    discoveredApp.setName(appName);

    Map<String, String> labels = argoAppCr.getMetadata().getLabels();
    discoveredApp.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());

    OffsetDateTime creationTimestamp = argoAppCr.getMetadata().getCreationTimestamp();
    if (creationTimestamp != null) {
      discoveredApp.setFirstSeen(creationTimestamp.toLocalDateTime());
    } else {
      logger.warn(
          "Creation timestamp is null for Argo App: {}. Using current time as fallback.", appName);
      discoveredApp.setFirstSeen(now());
    }

    discoveredApp.setLastSeen(now());
    return discoveredApp;
  }

  private AppVersion createAppVersion(
      DynamicKubernetesObject argoAppCr, String appName, Map<String, String> appLabels) {
    AppVersion currentVersion = new AppVersion();
    currentVersion.setDiscoveredAt(now());

    // Copy labels from App
    currentVersion.setLabels(new HashMap<>(appLabels));

    JsonObject rawCr = argoAppCr.getRaw();
    String appVersionString = extractVersionString(rawCr, appName);
    currentVersion.setVersion(appVersionString);

    Set<AppArtifact> artifacts = extractArtifacts(rawCr, appName);
    currentVersion.setArtifacts(artifacts);

    return currentVersion;
  }

  private String extractVersionString(JsonObject rawCr, String appName) {
    JsonObject spec = rawCr.getAsJsonObject("spec");
    JsonObject status = rawCr.getAsJsonObject("status");

    String appVersionString = "unknown";

    // Try to extract from status first
    if (status != null && status.has("sync") && status.get("sync").isJsonObject()) {
      JsonObject syncStatus = status.getAsJsonObject("sync");
      if (syncStatus.has("revision") && syncStatus.get("revision").isJsonPrimitive()) {
        appVersionString = syncStatus.get("revision").getAsString();
      }
    }

    // Fallback to spec if needed
    if (("unknown".equals(appVersionString) || appVersionString.isEmpty()) && spec != null) {
      appVersionString = extractVersionFromSpec(spec, appName);
    }

    return appVersionString;
  }

  private String extractVersionFromSpec(JsonObject spec, String appName) {
    String appVersionString = "unknown";

    if (spec.has("source") && spec.get("source").isJsonObject()) {
      JsonObject source = spec.getAsJsonObject("source");
      if (source.has("targetRevision") && source.get("targetRevision").isJsonPrimitive()) {
        appVersionString = source.get("targetRevision").getAsString();
      }
    } else if (spec.has("sources") && spec.get("sources").isJsonArray()) {
      JsonArray sourcesArray = spec.getAsJsonArray("sources");
      if (!sourcesArray.isEmpty() && sourcesArray.get(0).isJsonObject()) {
        JsonObject firstSource = sourcesArray.get(0).getAsJsonObject();
        if (firstSource.has("targetRevision")
            && firstSource.get("targetRevision").isJsonPrimitive()) {
          appVersionString = firstSource.get("targetRevision").getAsString();
          if (sourcesArray.size() > 1) {
            logger.debug(
                "Argo App {} has multiple sources. Using targetRevision from first source: {}",
                appName,
                appVersionString);
          }
        }
      }
    }

    return appVersionString;
  }

  private Set<AppArtifact> extractArtifacts(JsonObject rawCr, String appName) {
    Set<AppArtifact> artifacts = new HashSet<>();
    JsonObject spec = rawCr.getAsJsonObject("spec");
    JsonObject status = rawCr.getAsJsonObject("status");

    // Extract source artifacts from spec
    extractSourceArtifacts(spec, artifacts, appName);

    // Extract deployed image artifacts from status
    extractImageArtifacts(status, artifacts);

    return artifacts;
  }

  private void extractSourceArtifacts(JsonObject spec, Set<AppArtifact> artifacts, String appName) {
    if (spec == null) {
      logger.debug("Argo App {} has no 'spec' field. Cannot extract source artifacts.", appName);
      return;
    }

    if (spec.has("source") && spec.get("source").isJsonObject()) {
      JsonObject source = spec.getAsJsonObject("source");
      extractSourceArtifact(source, artifacts, appName);
    } else if (spec.has("sources") && spec.get("sources").isJsonArray()) {
      JsonArray sourcesArray = spec.getAsJsonArray("sources");
      for (JsonElement sourceElement : sourcesArray) {
        if (sourceElement.isJsonObject()) {
          extractSourceArtifact(sourceElement.getAsJsonObject(), artifacts, appName);
        }
      }
    }
  }

  private void extractImageArtifacts(JsonObject status, Set<AppArtifact> artifacts) {
    if (status != null && status.has("summary") && status.get("summary").isJsonObject()) {
      JsonObject summary = status.getAsJsonObject("summary");
      if (summary.has("images") && summary.get("images").isJsonArray()) {
        JsonArray images = summary.getAsJsonArray("images");
        for (JsonElement imageElement : images) {
          if (imageElement.isJsonPrimitive()) {
            String imageName = imageElement.getAsString();
            AppArtifact artifact = new AppArtifact(imageName, "containerImage");
            artifacts.add(artifact);
          }
        }
      }
    }
  }

  private void extractSourceArtifact(
      JsonObject source, Set<AppArtifact> artifacts, String appName) {
    if (source == null) {
      return;
    }

    // Extract repository URL if available
    if (source.has("repoURL") && source.get("repoURL").isJsonPrimitive()) {
      String repoUrl = source.get("repoURL").getAsString();
      String sourceType = determineSourceType(source);

      // Create artifact with repo URL
      AppArtifact artifact = new AppArtifact(repoUrl, sourceType);

      // Add optional chart info for Helm charts
      if ("helm".equals(sourceType)
          && source.has("chart")
          && source.get("chart").isJsonPrimitive()) {
        artifact.addMetadata("chart", source.get("chart").getAsString());
      }

      // Add path for non-root directory sources
      if (source.has("path") && source.get("path").isJsonPrimitive()) {
        String path = source.get("path").getAsString();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
          artifact.addMetadata("path", path);
        }
      }

      artifacts.add(artifact);
    } else {
      logger.debug("Argo App {} source is missing repoURL", appName);
    }
  }

  private String determineSourceType(JsonObject source) {
    if (source.has("chart") && source.get("chart").isJsonPrimitive()) {
      return "helm";
    } else if (source.has("plugin") && source.get("plugin").isJsonObject()) {
      return "plugin";
    } else if (source.has("directory") && source.get("directory").isJsonObject()) {
      return "directory";
    } else {
      return "git"; // Default to git
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of(AMSTERDAM_ZONE_ID));
  }

  @Override
  public Set<App> getCollectedApplications() {
    return Collections.unmodifiableSet(argoApps);
  }
}
