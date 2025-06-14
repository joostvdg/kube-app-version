/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig implements HealthIndicator {

  private static final Logger logger = LoggerFactory.getLogger(KubernetesClientConfig.class);
  private final KubernetesConfigProperties kubeConfig;
  private boolean connectionHealthy = false;

  public KubernetesClientConfig(KubernetesConfigProperties kubeConfig) {
    this.kubeConfig = kubeConfig;
  }

  @Bean
  public ApiClient apiClient() throws Exception {
    ApiClient client =
        switch (kubeConfig.getMode()) {
          case IN_CLUSTER -> apiClientFromCluster();
          case KUBECONFIG -> apiClientFromKubeConfig();
          case DIRECT -> apiClientDirect();
        };

    io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
    verifyConnection(client);
    return client;
  }

  private ApiClient apiClientFromCluster() throws IOException {
    //    String host = System.getenv("KUBERNETES_SERVICE_HOST");
    //    String port = System.getenv("KUBERNETES_SERVICE_PORT");
    //    logger.info(
    //        "Using in-cluster Kubernetes API configuration: KUBERNETES_SERVICE_HOST={},"
    //            + " KUBERNETES_SERVICE_PORT={}",
    //        host,
    //        port);
    //    return Config.fromCluster();
    logger.info("Using in-cluster Kubernetes API configuration (Config.defaultClient())");
    return Config.defaultClient();
  }

  private ApiClient apiClientFromKubeConfig() throws IOException {
    logger.info(
        "Using kubeconfig file: {} with context: {}",
        this.kubeConfig.getKubeconfigPath(),
        this.kubeConfig.getContextName());

    if (this.kubeConfig.getKubeconfigPath() == null
        || this.kubeConfig.getKubeconfigPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Kubeconfig path must be provided when using KUBECONFIG mode");
    }

    KubeConfig kc =
        KubeConfig.loadKubeConfig(
            Files.newBufferedReader(
                Paths.get(this.kubeConfig.getKubeconfigPath()), Charset.defaultCharset()));
    if (this.kubeConfig.getContextName() != null && !this.kubeConfig.getContextName().isEmpty()) {
      kc.setContext(this.kubeConfig.getContextName());
    }
    return ClientBuilder.kubeconfig(kc).build();
  }

  private ApiClient apiClientDirect() throws IOException {
    logger.info(
        "Using direct Kubernetes connection to cluster: {} at endpoint: {}",
        this.kubeConfig.getClusterName(),
        this.kubeConfig.getClusterEndpoint());

    if (this.kubeConfig.getClusterEndpoint() == null
        || this.kubeConfig.getClusterEndpoint().isEmpty()) {
      throw new IllegalArgumentException(
          "Cluster endpoint must be provided when using DIRECT mode");
    }

    return ClientBuilder.standard()
        .setBasePath("https://" + this.kubeConfig.getClusterEndpoint())
        .build();
  }

  private void verifyConnection(ApiClient client) {
    CoreV1Api api = new CoreV1Api(client);
    List<V1APIResource> testResources = null;
    try {
      testResources = api.getAPIResources().execute().getResources();
    } catch (ApiException e) {
      logger.error("Failed to verify Kubernetes API connection: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
    if (testResources.isEmpty()) {
      logger.warn("Was not able to collect any resources from the Kubernetes API.");
    }

    connectionHealthy = true;
    logger.info("Successfully verified Kubernetes API connection");
  }

  @Bean
  public CoreV1Api coreV1Api(ApiClient apiClient) {
    return new CoreV1Api(apiClient);
  }

  @Override
  public Health health() {
    return connectionHealthy
        ? Health.up().withDetail("mode", kubeConfig.getMode()).build()
        : Health.down().withDetail("mode", kubeConfig.getMode()).build();
  }
}
