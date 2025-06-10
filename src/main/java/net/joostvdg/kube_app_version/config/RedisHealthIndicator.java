/* (C)2025 */
package net.joostvdg.kube_app_version.health;

import net.joostvdg.kube_app_version.config.RedisConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

  private final RedisConnectionFactory redisConnectionFactory;
  private final RedisConfigProperties redisConfig;

  public RedisHealthIndicator(
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory,
      RedisConfigProperties redisConfig) {
    this.redisConnectionFactory = redisConnectionFactory;
    this.redisConfig = redisConfig;
  }

  @Override
  public Health health() {
    if (redisConfig.isDisabled()) {
      return Health.up()
          .withDetail("mode", "DISABLED")
          .withDetail("message", "Redis is disabled by configuration")
          .build();
    }

    if (redisConnectionFactory == null) {
      if (redisConfig.isRequired()) {
        return Health.down()
            .withDetail("mode", "REQUIRED")
            .withDetail("error", "Redis is required but no connection factory is available")
            .build();
      } else {
        return Health.up()
            .withDetail("mode", "OPTIONAL")
            .withDetail("status", "Redis is optional but not available")
            .build();
      }
    }

    try {
      redisConnectionFactory.getConnection().close();
      return Health.up()
          .withDetail("mode", redisConfig.getMode())
          .withDetail("status", "Connected")
          .build();
    } catch (Exception e) {
      Health.Builder builder = redisConfig.isRequired() ? Health.down() : Health.up();
      return builder
          .withDetail("mode", redisConfig.getMode())
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
