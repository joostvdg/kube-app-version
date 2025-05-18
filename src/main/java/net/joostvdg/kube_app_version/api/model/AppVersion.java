package net.joostvdg.kube_app_version.api.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class AppVersion {
    private String version;
    private LocalDateTime discoveredAt;
    private Map<String, String> labels;
    private Set<AppArtifact> artifacts;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Set<AppArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Set<AppArtifact> artifacts) {
        this.artifacts = artifacts;
    }
}
