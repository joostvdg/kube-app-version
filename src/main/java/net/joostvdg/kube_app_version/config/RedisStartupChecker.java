/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.mode", havingValue = "DISABLED", matchIfMissing = false)
public class RedisStartupChecker implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(RedisStartupChecker.class);
  private final RedisConnectionFactory redisConnectionFactory;
  private final RedisConfigProperties redisConfig;

  public RedisStartupChecker(
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory,
      RedisConfigProperties redisConfig) {
    this.redisConnectionFactory = redisConnectionFactory;
    this.redisConfig = redisConfig;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (redisConfig.isDisabled()) {
      logger.info("Redis is disabled by configuration");
      return;
    }

    if (redisConnectionFactory == null) {
      handleConnectionFailure("Redis connection factory is not available");
      return;
    }

    try {
      redisConnectionFactory.getConnection().close();
      logger.info("Successfully connected to Redis");
    } catch (Exception e) {
      handleConnectionFailure("Failed to connect to Redis: " + e.getMessage());
    }
  }

  private void handleConnectionFailure(String message) {
    if (redisConfig.isRequired()) {
      logger.error("{} (mode: REQUIRED)", message);
    } else {
      logger.warn("{} (mode: OPTIONAL) - continuing without Redis functionality", message);
    }
  }
}
