/* (C)2025 */
package net.joostvdg.kube_app_version.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.redis.mode", havingValue = "REQUIRED")
public class RedisConnectionMonitor {

  private static final Logger logger = LoggerFactory.getLogger(RedisConnectionMonitor.class);
  private final RedisConnectionFactory redisConnectionFactory;

  public RedisConnectionMonitor(
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
    this.redisConnectionFactory = redisConnectionFactory;
  }

  @Scheduled(fixedDelayString = "${app.redis.reconnectInterval:60000}")
  public void checkRedisConnection() {
    if (redisConnectionFactory == null) {
      logger.error("Redis connection factory is not available but mode is set to REQUIRED");
      return;
    }

    try {
      redisConnectionFactory.getConnection().close();
      logger.debug("Redis connection check successful");
    } catch (Exception e) {
      logger.error("Redis connection check failed: {}", e.getMessage(), e);
    }
  }
}
