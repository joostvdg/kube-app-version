/* (C)2025 */
package net.joostvdg.kube_app_version.api.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class App {
  private String id;
  private String name;
  private Map<String, String> labels;
  private LocalDateTime firstSeen;
  private LocalDateTime lastSeen;
  private Set<AppVersion> versions;

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
}
