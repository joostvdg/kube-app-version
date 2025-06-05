/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager caffeineCacheManager() {
    return new CaffeineCacheManager("kubeversion");
  }

  //    @Bean
  //    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
  //        return RedisCacheManager.builder(redisConnectionFactory).build();
  //    }

  @Primary
  @Bean
  public CompositeCacheManager cacheManager(CacheManager caffeine, CacheManager redis) {
    CompositeCacheManager manager = new CompositeCacheManager(caffeine, redis);
    manager.setFallbackToNoOpCache(false);
    return manager;
  }
}
