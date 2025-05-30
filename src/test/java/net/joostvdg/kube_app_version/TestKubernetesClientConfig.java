/* (C)2025 */
package net.joostvdg.kube_app_version;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestKubernetesClientConfig {

  @Bean("apiClient.stub")
  public ApiClient apiClient() {
    ApiClient client = null;

    return client;
  }

  @Primary
  @Bean("coreV1Api.stub")
  public CoreV1Api coreV1Api(ApiClient apiClient) {
    return Mockito.mock(CoreV1Api.class);
  }
}
