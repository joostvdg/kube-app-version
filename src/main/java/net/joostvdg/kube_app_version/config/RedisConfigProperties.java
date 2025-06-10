/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.redis")
@Component
public class RedisConfigProperties {

  private Mode mode = Mode.OPTIONAL;
  private long reconnectInterval = 60000; // in milliseconds

  public enum Mode {
    DISABLED, // Don't use Redis at all
    OPTIONAL, // Use if available, function without it
    REQUIRED // Application is unhealthy if Redis is unavailable
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public boolean isDisabled() {
    return Mode.DISABLED == mode;
  }

  public boolean isRequired() {
    return Mode.REQUIRED == mode;
  }

  public boolean isOptional() {
    return Mode.OPTIONAL == mode;
  }

  public long getReconnectInterval() {
    return reconnectInterval;
  }

  public void setReconnectInterval(long reconnectInterval) {
    this.reconnectInterval = reconnectInterval;
  }
}
