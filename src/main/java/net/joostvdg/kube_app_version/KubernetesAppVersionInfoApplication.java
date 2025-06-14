/* (C)2025 */
package net.joostvdg.kube_app_version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication
public class KubernetesAppVersionInfoApplication {

  public static void main(String[] args) {
    SpringApplication.run(KubernetesAppVersionInfoApplication.class, args);
  }
}
