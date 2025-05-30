/* (C)2025 */
package net.joostvdg.kube_app_version.api.model;

import java.time.LocalDateTime;

public class AppArtifact {
  private String source;
  private String artifactType;
  private LocalDateTime discoveredAt;

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(String artifactType) {
    this.artifactType = artifactType;
  }

  public LocalDateTime getDiscoveredAt() {
    return discoveredAt;
  }

  public void setDiscoveredAt(LocalDateTime discoveredAt) {
    this.discoveredAt = discoveredAt;
  }
}
