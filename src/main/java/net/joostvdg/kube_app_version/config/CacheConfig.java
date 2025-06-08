/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager caffeineCacheManager() {
    return new CaffeineCacheManager("kubeversion");
  }

  //  @Bean
  //  public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
  //    RedisCacheConfiguration config =
  //        RedisCacheConfiguration.defaultCacheConfig()
  //            .entryTtl(Duration.ofHours(1))
  //            .serializeValuesWith(
  //                RedisSerializationContext.SerializationPair.fromSerializer(
  //                    new GenericJackson2JsonRedisSerializer()));
  //
  //    return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
  //  }
  //
  //  @Primary
  //  @Bean
  //  public CompositeCacheManager cacheManager(
  //      CacheManager caffeineCacheManager, CacheManager redisCacheManager) {
  //    CompositeCacheManager manager =
  //        new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
  //    manager.setFallbackToNoOpCache(false);
  //    return manager;
  //  }
}
