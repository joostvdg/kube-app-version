/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import net.joostvdg.kube_app_version.versions.AppArtifactRepository;
import net.joostvdg.kube_app_version.versions.NoOpAppArtifactRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.redis.mode", havingValue = "DISABLED")
public class RedisDisabledConfig {

  @Bean
  public AppArtifactRepository appArtifactRepository() {
    return new NoOpAppArtifactRepository();
  }
}
