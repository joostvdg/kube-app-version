/* (C)2025 */
package net.joostvdg.kube_app_version.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("AppArtifact")
public class AppArtifact implements Serializable {

  private @Id String identifier;
  private @Indexed String source;
  private String artifactName;
  private String artifactType;
  private LocalDateTime discoveredAt;
  private Map<String, String> metaData;

  /** Default empty constructor for Serialization (JSON). */
  public AppArtifact() {
    // for JSON Serialization
    this.metaData = new HashMap<>();
  }

  /**
   * Primary constructor.
   *
   * @param source source of the artifact, e.g., oci://ghcr.io/gabe565/charts/gotify
   * @param artifactType the type of the artifact, e.g., containerImage, git, helm
   */
  public AppArtifact(String source, String artifactType, String artifactName) {
    this.artifactType = artifactType;
    this.source = source;
    this.artifactName = artifactName;
    this.discoveredAt = LocalDateTime.now(ZoneId.systemDefault());
    this.identifier = source + "::" + artifactType;
    this.metaData = new HashMap<>();
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

  public Map<String, String> getMetaData() {
    return metaData;
  }

  public void setMetaData(Map<String, String> metaData) {
    this.metaData = metaData;
  }

  public String getArtifactName() {
    return artifactName;
  }

  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Adds a key-value pair to the metadata map.
   *
   * @param key the metadata key
   * @param value the metadata value
   */
  public void addMetadata(String key, String value) {
    if (this.metaData == null) {
      this.metaData = new HashMap<>();
    }
    this.metaData.put(key, value);
  }
}
