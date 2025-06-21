/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.version.outdated-artifacts")
public class OutdatedArtifactsProperties {

  private boolean collectOnStartup = true;
  private boolean refreshOnStartup = true;
  private long intervalMs = 3600000; // 1 hour
  private long cacheValidityMinutes = 60;

  public boolean isCollectOnStartup() {
    return collectOnStartup;
  }

  public void setCollectOnStartup(boolean collectOnStartup) {
    this.collectOnStartup = collectOnStartup;
  }

  public boolean isRefreshOnStartup() {
    return refreshOnStartup;
  }

  public void setRefreshOnStartup(boolean refreshOnStartup) {
    this.refreshOnStartup = refreshOnStartup;
  }

  public long getIntervalMs() {
    return intervalMs;
  }

  public void setIntervalMs(long intervalMs) {
    this.intervalMs = intervalMs;
  }

  public long getCacheValidityMinutes() {
    return cacheValidityMinutes;
  }

  public void setCacheValidityMinutes(long cacheValidityMinutes) {
    this.cacheValidityMinutes = cacheValidityMinutes;
  }
}
