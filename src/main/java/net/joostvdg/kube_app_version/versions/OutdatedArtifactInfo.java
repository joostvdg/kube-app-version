package net.joostvdg.kube_app_version.versions.dto;

import java.util.List;
import java.util.Objects;

public class OutdatedArtifactInfo {
    private String appName;
    private String appId;
    private String deployedAppVersion; // Overall version of the deployed app
    private String artifactSource;
    private String artifactType;
    private String currentArtifactVersion;
    private String latestOverallVersion; // Could be GA or pre-release
    private String latestGARelease;      // Latest stable release
    private String latestPreRelease;     // Latest pre-release (if any)
    private String nextMinorVersion;     // Next available minor update for current
    private String nextMajorVersion;     // Next available major update for current
    private Long majorVersionDelta;   // Delta to latest GA Major
    private Long minorVersionDelta;   // Delta to latest GA Minor (within same Major)
    private List<String> availableArtifactVersions; // Full list for context

    public OutdatedArtifactInfo(String appName, String appId, String deployedAppVersion,
                                String artifactSource, String artifactType, String currentArtifactVersion,
                                String latestOverallVersion, String latestGARelease, String latestPreRelease,
                                String nextMinorVersion, String nextMajorVersion,
                                Long majorVersionDelta, Long minorVersionDelta, // New params
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
        this.majorVersionDelta = majorVersionDelta; // New field
        this.minorVersionDelta = minorVersionDelta; // New field
        this.availableArtifactVersions = availableArtifactVersions;
    }

    // --- Getters ---
    public String getAppName() { return appName; }
    public String getAppId() { return appId; }
    public String getDeployedAppVersion() { return deployedAppVersion; }
    public String getArtifactSource() { return artifactSource; }
    public String getArtifactType() { return artifactType; }
    public String getCurrentArtifactVersion() { return currentArtifactVersion; }
    public String getLatestOverallVersion() { return latestOverallVersion; }
    public String getLatestGARelease() { return latestGARelease; }
    public String getLatestPreRelease() { return latestPreRelease; }
    public String getNextMinorVersion() { return nextMinorVersion; }
    public String getNextMajorVersion() { return nextMajorVersion; }
    public Long getMajorVersionDelta() { return majorVersionDelta; } // New getter
    public Long getMinorVersionDelta() { return minorVersionDelta; } // New getter
    public List<String> getAvailableArtifactVersions() { return availableArtifactVersions; }

    // --- equals, hashCode, toString (important for collections and logging) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutdatedArtifactInfo that = (OutdatedArtifactInfo) o;
        return Objects.equals(appName, that.appName) &&
                Objects.equals(appId, that.appId) &&
                Objects.equals(deployedAppVersion, that.deployedAppVersion) &&
                Objects.equals(artifactSource, that.artifactSource) &&
                Objects.equals(artifactType, that.artifactType) &&
                Objects.equals(currentArtifactVersion, that.currentArtifactVersion) &&
                Objects.equals(latestOverallVersion, that.latestOverallVersion) &&
                Objects.equals(latestGARelease, that.latestGARelease) &&
                Objects.equals(latestPreRelease, that.latestPreRelease) &&
                Objects.equals(nextMinorVersion, that.nextMinorVersion) &&
                Objects.equals(nextMajorVersion, that.nextMajorVersion) &&
                Objects.equals(majorVersionDelta, that.majorVersionDelta) && // New field
                Objects.equals(minorVersionDelta, that.minorVersionDelta);   // New field
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appId, deployedAppVersion, artifactSource, artifactType,
                currentArtifactVersion, latestOverallVersion, latestGARelease, latestPreRelease,
                nextMinorVersion, nextMajorVersion, majorVersionDelta, minorVersionDelta); // New fields
    }

    @Override
    public String toString() {
        return "OutdatedArtifactInfo{" +
                "appName='" + appName + '\'' +
                ", appId='" + appId + '\'' +
                ", deployedAppVersion='" + deployedAppVersion + '\'' +
                ", artifactSource='" + artifactSource + '\'' +
                ", artifactType='" + artifactType + '\'' +
                ", currentArtifactVersion='" + currentArtifactVersion + '\'' +
                ", latestOverallVersion='" + latestOverallVersion + '\'' +
                ", latestGARelease='" + latestGARelease + '\'' +
                ", latestPreRelease='" + latestPreRelease + '\'' +
                ", nextMinorVersion='" + nextMinorVersion + '\'' +
                ", nextMajorVersion='" + nextMajorVersion + '\'' +
                ", majorVersionDelta=" + majorVersionDelta + // New field
                ", minorVersionDelta=" + minorVersionDelta + // New field
                ", availableArtifactVersionsCount=" + (availableArtifactVersions != null ? availableArtifactVersions.size() : 0) +
                '}';
    }
}