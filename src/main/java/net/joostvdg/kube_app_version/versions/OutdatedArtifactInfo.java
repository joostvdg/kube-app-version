package net.joostvdg.kube_app_version.versions.dto;

import java.util.List;
import java.util.Objects;

public class OutdatedArtifactInfo {
    private String appName;
    private String appId; // From App.getId()
    private String deployedAppVersion; // From AppVersion.getVersion() - the overall version of the deployed app
    private String artifactSource;
    private String artifactType;
    private String currentArtifactVersion; // The specific version of THIS artifact
    private String latestArtifactVersion;
    private List<String> availableArtifactVersions;

    public OutdatedArtifactInfo(String appName, String appId, String deployedAppVersion, String artifactSource, String artifactType, String currentArtifactVersion, String latestArtifactVersion, List<String> availableArtifactVersions) {
        this.appName = appName;
        this.appId = appId;
        this.deployedAppVersion = deployedAppVersion;
        this.artifactSource = artifactSource;
        this.artifactType = artifactType;
        this.currentArtifactVersion = currentArtifactVersion;
        this.latestArtifactVersion = latestArtifactVersion;
        this.availableArtifactVersions = availableArtifactVersions;
    }

    // Getters (and potentially setters, equals, hashCode, toString if not using records)
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

    public String getLatestArtifactVersion() {
        return latestArtifactVersion;
    }

    public List<String> getAvailableArtifactVersions() {
        return availableArtifactVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutdatedArtifactInfo that = (OutdatedArtifactInfo) o;
        return Objects.equals(appName, that.appName) && Objects.equals(appId, that.appId) && Objects.equals(deployedAppVersion, that.deployedAppVersion) && Objects.equals(artifactSource, that.artifactSource) && Objects.equals(artifactType, that.artifactType) && Objects.equals(currentArtifactVersion, that.currentArtifactVersion) && Objects.equals(latestArtifactVersion, that.latestArtifactVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appId, deployedAppVersion, artifactSource, artifactType, currentArtifactVersion, latestArtifactVersion);
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
                ", latestArtifactVersion='" + latestArtifactVersion + '\'' +
                ", availableArtifactVersions=" + availableArtifactVersions +
                '}';
    }
}