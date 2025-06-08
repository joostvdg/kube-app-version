/* (C)2025 */
package net.joostvdg.kube_app_version.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("AppArtifact")
public class AppArtifact implements Serializable {

  private @Indexed String source;
  private String artifactType;
  private LocalDateTime discoveredAt;
  private @Id String identifier;

  /** Default empty constructor for Serialization (JSON). */
  public AppArtifact() {
    // for JSON Serialization
  }

  /**
   * Primary constructor.
   *
   * @param source source of the artifact, e.g., oci://ghcr.io/gabe565/charts/gotify
   * @param artifactType the type of the artifact, e.g., containerImage, git, helm
   */
  public AppArtifact(String source, String artifactType) {
    this.artifactType = artifactType;
    this.source = source;
    this.discoveredAt = LocalDateTime.now(ZoneId.systemDefault());
    this.identifier = source + "::" + artifactType;
  }

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

  public String getIdentifier() {
    return identifier;
  }

  // TODO: for JSON serialization  only?
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}
