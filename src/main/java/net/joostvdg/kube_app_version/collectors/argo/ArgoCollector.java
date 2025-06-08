/* (C)2025 */
package net.joostvdg.kube_app_version.collectors.argo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.ApplicationCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ArgoCollector implements ApplicationCollector { // Implement the interface
  private final Set<App> argoApps = Collections.synchronizedSet(new HashSet<>());

  private final Logger logger = LoggerFactory.getLogger(ArgoCollector.class);
  // private final CoreV1Api coreV1Api; // Injected via constructor
  private final ApiClient apiClient;

  private static final String AMSTERDAM_ZONE_ID = "Europe/Amsterdam";

  @Value("${cluster.name}")
  private String clusterName;

  @Value("${cluster.url}")
  private String clusterApiServerIp;

  public ArgoCollector(CoreV1Api coreV1Api, ApiClient apiClient) {
    // this.coreV1Api = coreV1Api;
    this.apiClient = apiClient;
  }

  @PostConstruct
  private void init() {
    printClusterInfo();
    try {
      collectArgoApplications();
    } catch (ApiException e) {
      logger.error(
          "Failed to collect Argo Applications during initialization: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private void printClusterInfo() {
    logger.info("Cluster Name: {}", clusterName);
    logger.info("Cluster API Server IP: {}", clusterApiServerIp);
  }

  private void collectArgoApplications() throws ApiException {
    // Note: Ensure your ApiClient is configured for the correct context/namespace,
    // or use listAll() for all namespaces if Argo apps are cluster-wide.
    DynamicKubernetesApi dynamicApi =
        new DynamicKubernetesApi("argoproj.io", "v1alpha1", "applications", apiClient);

    // Clears previously collected apps. If you want to update existing ones,
    // you'll need a different strategy (e.g. Map<String, App> and update logic).
    argoApps.clear();

    for (DynamicKubernetesObject argoAppCr : dynamicApi.list().getObject().getItems()) {
      String appName = argoAppCr.getMetadata().getName();
      logger.info("Processing Argo Application: {}", appName);

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
            "Creation timestamp is null for Argo App: {}. Using current time as fallback for"
                + " firstSeen.",
            appName);
        discoveredApp.setFirstSeen(now());
      }

      // lastSeen reflects when this collector last processed this app's data.
      discoveredApp.setLastSeen(now());

      // --- Populate AppVersion ---
      Set<AppVersion> appVersions = new HashSet<>();
      AppVersion currentVersion = new AppVersion();
      currentVersion.setDiscoveredAt(now());

      // AppVersion labels can be inherited from the App or be more specific if available
      currentVersion.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());

      JsonObject rawCr = argoAppCr.getRaw();
      JsonObject spec = rawCr.getAsJsonObject("spec"); // Can be null
      JsonObject status = rawCr.getAsJsonObject("status"); // Can be null

      // Extract version (target revision or sync revision)
      String appVersionString = "unknown";
      if (status != null && status.has("sync") && status.get("sync").isJsonObject()) {
        JsonObject syncStatus = status.getAsJsonObject("sync");
        if (syncStatus.has("revision") && syncStatus.get("revision").isJsonPrimitive()) {
          appVersionString = syncStatus.get("revision").getAsString();
        }
      }

      // Fallback to spec if status.sync.revision is not available or status is null
      if (("unknown".equals(appVersionString) || appVersionString.isEmpty()) && spec != null) {
        if (spec.has("source") && spec.get("source").isJsonObject()) {
          JsonObject source = spec.getAsJsonObject("source");
          if (source.has("targetRevision") && source.get("targetRevision").isJsonPrimitive()) {
            appVersionString = source.get("targetRevision").getAsString();
          }
        } else if (spec.has("sources") && spec.get("sources").isJsonArray()) {
          // Handle multi-source applications (e.g., from ApplicationSet)
          // This takes the targetRevision from the first source as a simplification.
          JsonArray sourcesArray = spec.getAsJsonArray("sources");
          if (!sourcesArray.isEmpty() && sourcesArray.get(0).isJsonObject()) {
            JsonObject firstSource = sourcesArray.get(0).getAsJsonObject();
            if (firstSource.has("targetRevision")
                && firstSource.get("targetRevision").isJsonPrimitive()) {
              appVersionString = firstSource.get("targetRevision").getAsString();
              if (sourcesArray.size() > 1) {
                logger.debug(
                    "Argo App {} has multiple sources. Using targetRevision from the first source:"
                        + " {}",
                    appName,
                    appVersionString);
              }
            }
          }
        }
      }
      currentVersion.setVersion(appVersionString);

      // --- Populate AppArtifacts ---
      Set<AppArtifact> artifacts = new HashSet<>();

      // 1. Extract Source Artifacts (Helm or Git) from spec
      if (spec != null) {
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
      } else {
        logger.debug("Argo App {} has no 'spec' field. Cannot extract source artifacts.", appName);
      }

      // 2. Extract Deployed Image Artifacts from status.summary (existing logic)
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
      // Note: If status.summary.images is not available/enabled in ArgoCD,
      // a more complex approach would be to parse status.resources,
      // find deployed workloads (Deployments, StatefulSets), and extract their container images.

      currentVersion.setArtifacts(artifacts);
      appVersions.add(currentVersion);
      discoveredApp.setVersions(appVersions);
      discoveredApp.setCurrentVersion(
          currentVersion); // TODO: create logic to determine the actual current version

      // Add the fully populated App object to the set.
      // For this to correctly avoid duplicates if run multiple times without clearing argoApps,
      // or to update existing entries, the App class would need equals() and hashCode()
      // implemented, typically based on 'id'.
      // Since argoApps is cleared at the start of this method in this example,
      // it just collects the current state.
      argoApps.add(discoveredApp);
    }
    logger.info("Finished collecting Argo applications. Total found: {}", argoApps.size());
  }

  /**
   * Helper method to extract source artifact details from a single source object. Handles both Helm
   * and Git types.
   *
   * @param sourceObject The JsonObject representing a single source (from spec.source or
   *     spec.sources[]).
   * @param artifacts The set to add the discovered artifacts to.
   * @param appName The name of the application (for logging).
   */
  private void extractSourceArtifact(
      JsonObject sourceObject, Set<AppArtifact> artifacts, String appName) {
    String source = "";
    String artifactType = "";

    String repoUrl =
        sourceObject.has("repoURL") && sourceObject.get("repoURL").isJsonPrimitive()
            ? sourceObject.get("repoURL").getAsString()
            : null;
    String chart =
        sourceObject.has("chart") && sourceObject.get("chart").isJsonPrimitive()
            ? sourceObject.get("chart").getAsString()
            : null;
    String path =
        sourceObject.has("path") && sourceObject.get("path").isJsonPrimitive()
            ? sourceObject.get("path").getAsString()
            : null;

    if (chart != null) {
      // It's a Helm chart source
      artifactType = "helm";
      if (repoUrl != null) {
        source = repoUrl + "/" + chart;
      } else {
        source = chart; // Should ideally have repoURL, but handle if missing
        logger.warn(
            "Argo App {} source has 'chart' but no 'repoURL'. Source set to just chart name: {}",
            appName,
            chart);
      }

    } else if (repoUrl != null && path != null) {
      // It's a Git repository source (assuming not Helm if chart is null)
      artifactType = "git";
      source = repoUrl + "/" + path;

    } else {
      // Source type could not be determined based on chart/repoURL/path
      logger.debug(
          "Argo App {}: Could not determine source artifact type from source object: {}",
          appName,
          sourceObject);
    }
    AppArtifact sourceArtifact = new AppArtifact(source, artifactType);
    artifacts.add(sourceArtifact);
    logger.debug(
        "Argo App {}: Added Helm source artifact: {}", appName, sourceArtifact.getSource());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of(AMSTERDAM_ZONE_ID));
  }

  // Implement the interface method
  @Override
  public Set<App> getCollectedApplications() {
    // Return a copy to prevent external modification if argoApps is not thread-safe for iteration
    // or if you want to provide a snapshot.
    // Collections.synchronizedSet only synchronizes individual operations, not iteration.
    // For simplicity here, returning the direct reference. If concurrent access during iteration
    // is a concern, consider returning new HashSet<>(argoApps) or using a ConcurrentHashMap.
    return argoApps;
  }
}
