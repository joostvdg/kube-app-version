/* (C)2025 */
package net.joostvdg.kube_app_version.collectors.argo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "argo.collector")
public class ArgoCollectorConfig {
  private boolean runOnStartup = true;
  private boolean scheduledCollectionEnabled = true;
  private int collectionIntervalMinutes = 15;

  // Getters and setters
  public boolean isRunOnStartup() {
    return runOnStartup;
  }

  public void setRunOnStartup(boolean runOnStartup) {
    this.runOnStartup = runOnStartup;
  }

  public boolean isScheduledCollectionEnabled() {
    return scheduledCollectionEnabled;
  }

  public void setScheduledCollectionEnabled(boolean scheduledCollectionEnabled) {
    this.scheduledCollectionEnabled = scheduledCollectionEnabled;
  }

  public int getCollectionIntervalMinutes() {
    return collectionIntervalMinutes;
  }

  public void setCollectionIntervalMinutes(int collectionIntervalMinutes) {
    this.collectionIntervalMinutes = collectionIntervalMinutes;
  }
}
