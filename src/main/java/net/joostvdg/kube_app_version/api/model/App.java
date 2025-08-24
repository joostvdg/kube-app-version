/* (C)2025 */
package net.joostvdg.kube_app_version.api.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class App {
  private String id;
  private String name;
  private Map<String, String> labels;
  private LocalDateTime firstSeen;
  private LocalDateTime lastSeen;
  private AppVersion currentVersion;
  private Set<AppVersion> versions;

  // Add to App.java
  private Map<String, String> artifactNameMappings = new HashMap<>(); // artifactType -> chartName

  public Map<String, String> getArtifactNameMappings() {
    return artifactNameMappings;
  }

  public void setArtifactNameMappings(Map<String, String> artifactNameMappings) {
    this.artifactNameMappings =
        artifactNameMappings != null ? artifactNameMappings : new HashMap<>();
  }

  public void addArtifactNameMapping(String artifactType, String chartName) {
    this.artifactNameMappings.put(artifactType, chartName);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public LocalDateTime getFirstSeen() {
    return firstSeen;
  }

  public void setFirstSeen(LocalDateTime firstSeen) {
    this.firstSeen = firstSeen;
  }

  public LocalDateTime getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(LocalDateTime lastSeen) {
    this.lastSeen = lastSeen;
  }

  public Set<AppVersion> getVersions() {
    return versions;
  }

  public void setVersions(Set<AppVersion> versions) {
    this.versions = versions;
  }

  public AppVersion getCurrentVersion() {
    return currentVersion;
  }

  public void setCurrentVersion(AppVersion currentVersion) {
    this.currentVersion = currentVersion;
  }
}
