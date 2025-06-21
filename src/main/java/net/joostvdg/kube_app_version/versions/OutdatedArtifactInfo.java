/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash("outdated_artifact_info")
public class OutdatedArtifactInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id private String id; // Composite key for Redis
  private final String appName;
  private final String appId;
  private final String deployedAppVersion;
  private final String artifactSource;
  private final String artifactType;
  private final String currentArtifactVersion;
  private final String latestOverallVersion;
  private final String latestGARelease;
  private final String latestPreRelease;
  private final String nextMinorVersion;
  private final String nextMajorVersion;
  private final Long majorVersionDelta;
  private final Long minorVersionDelta;
  private final List<String> availableArtifactVersions;
  private LocalDateTime lastUpdated;

  @TimeToLive private Long timeToLive;

  public OutdatedArtifactInfo(
      String appName,
      String appId,
      String deployedAppVersion,
      String artifactSource,
      String artifactType,
      String currentArtifactVersion,
      String latestOverallVersion,
      String latestGARelease,
      String latestPreRelease,
      String nextMinorVersion,
      String nextMajorVersion,
      Long majorVersionDelta,
      Long minorVersionDelta,
      List<String> availableArtifactVersions) {
    this.appName = appName;
    this.appId = appId;
    this.deployedAppVersion = deployedAppVersion;
    this.artifactSource = artifactSource;
    this.artifactType = artifactType;
    this.currentArtifactVersion = currentArtifactVersion;
    this.latestOverallVersion = latestOverallVersion;
    this.latestGARelease = latestGARelease;
    this.latestPreRelease = latestPreRelease;
    this.nextMinorVersion = nextMinorVersion;
    this.nextMajorVersion = nextMajorVersion;
    this.majorVersionDelta = majorVersionDelta;
    this.minorVersionDelta = minorVersionDelta;
    this.availableArtifactVersions = availableArtifactVersions;
    this.lastUpdated = LocalDateTime.now(ZoneId.systemDefault());
    this.id = generateId();
  }

  private String generateId() {
    return appId + ":" + artifactSource + ":" + artifactType;
  }

  // --- Getters ---
  public String getId() {
    return id;
  }

  public String getAppName() {
    return appName;
  }

  public String getAppId() {
    return appId;
  }

  public String getDeployedAppVersion() {
    return deployedAppVersion;
  }

  public String getArtifactSource() {
    return artifactSource;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public String getCurrentArtifactVersion() {
    return currentArtifactVersion;
  }

  public String getLatestOverallVersion() {
    return latestOverallVersion;
  }

  public String getLatestGARelease() {
    return latestGARelease;
  }

  public String getLatestPreRelease() {
    return latestPreRelease;
  }

  public String getNextMinorVersion() {
    return nextMinorVersion;
  }

  public String getNextMajorVersion() {
    return nextMajorVersion;
  }

  public Long getMajorVersionDelta() {
    return majorVersionDelta;
  }

  public Long getMinorVersionDelta() {
    return minorVersionDelta;
  }

  public List<String> getAvailableArtifactVersions() {
    return availableArtifactVersions;
  }

  public LocalDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Long getTimeToLive() {
    return timeToLive;
  }

  public void setTimeToLive(Long timeToLive) {
    this.timeToLive = timeToLive;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof OutdatedArtifactInfo that)) return false;

    return Objects.equals(appName, that.appName)
        && Objects.equals(appId, that.appId)
        && Objects.equals(deployedAppVersion, that.deployedAppVersion)
        && Objects.equals(artifactSource, that.artifactSource)
        && Objects.equals(artifactType, that.artifactType)
        && Objects.equals(currentArtifactVersion, that.currentArtifactVersion)
        && Objects.equals(latestOverallVersion, that.latestOverallVersion)
        && Objects.equals(latestGARelease, that.latestGARelease)
        && Objects.equals(latestPreRelease, that.latestPreRelease)
        && Objects.equals(nextMinorVersion, that.nextMinorVersion)
        && Objects.equals(nextMajorVersion, that.nextMajorVersion)
        && Objects.equals(majorVersionDelta, that.majorVersionDelta)
        && Objects.equals(minorVersionDelta, that.minorVersionDelta)
        && Objects.equals(availableArtifactVersions, that.availableArtifactVersions)
        && Objects.equals(lastUpdated, that.lastUpdated);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(appName);
    result = 31 * result + Objects.hashCode(appId);
    result = 31 * result + Objects.hashCode(deployedAppVersion);
    result = 31 * result + Objects.hashCode(artifactSource);
    result = 31 * result + Objects.hashCode(artifactType);
    result = 31 * result + Objects.hashCode(currentArtifactVersion);
    result = 31 * result + Objects.hashCode(latestOverallVersion);
    result = 31 * result + Objects.hashCode(latestGARelease);
    result = 31 * result + Objects.hashCode(latestPreRelease);
    result = 31 * result + Objects.hashCode(nextMinorVersion);
    result = 31 * result + Objects.hashCode(nextMajorVersion);
    result = 31 * result + Objects.hashCode(majorVersionDelta);
    result = 31 * result + Objects.hashCode(minorVersionDelta);
    result = 31 * result + Objects.hashCode(availableArtifactVersions);
    result = 31 * result + Objects.hashCode(lastUpdated);
    return result;
  }

  @Override
  public String toString() {
    return "OutdatedArtifactInfo{"
        + "appName='"
        + appName
        + '\''
        + ", appId='"
        + appId
        + '\''
        + ", deployedAppVersion='"
        + deployedAppVersion
        + '\''
        + ", artifactSource='"
        + artifactSource
        + '\''
        + ", artifactType='"
        + artifactType
        + '\''
        + ", currentArtifactVersion='"
        + currentArtifactVersion
        + '\''
        + ", latestOverallVersion='"
        + latestOverallVersion
        + '\''
        + ", latestGARelease='"
        + latestGARelease
        + '\''
        + ", latestPreRelease='"
        + latestPreRelease
        + '\''
        + ", nextMinorVersion='"
        + nextMinorVersion
        + '\''
        + ", nextMajorVersion='"
        + nextMajorVersion
        + '\''
        + ", majorVersionDelta="
        + majorVersionDelta
        + ", minorVersionDelta="
        + minorVersionDelta
        + ", availableArtifactVersionsCount="
        + (availableArtifactVersions != null ? availableArtifactVersions.size() : 0)
        + ", lastUpdated="
        + lastUpdated
        + '}';
  }
}
