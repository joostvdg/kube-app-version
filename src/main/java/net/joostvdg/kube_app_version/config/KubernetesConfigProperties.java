/* (C)2025 */
package net.joostvdg.kube_app_version.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.kubernetes")
@Component
public class KubernetesConfigProperties {

  private Mode mode = Mode.DIRECT;
  private String kubeconfigPath;
  private String contextName;
  private String clusterName = "local";
  private String clusterEndpoint = "127.0.0.1:443";

  public enum Mode {
    IN_CLUSTER, // Use in-cluster configuration
    KUBECONFIG, // Use kubeconfig file with specific context
    DIRECT // Use direct connection to cluster with name and endpoint
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public String getKubeconfigPath() {
    return kubeconfigPath;
  }

  public void setKubeconfigPath(String kubeconfigPath) {
    this.kubeconfigPath = kubeconfigPath;
  }

  public String getContextName() {
    return contextName;
  }

  public void setContextName(String contextName) {
    this.contextName = contextName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getClusterEndpoint() {
    return clusterEndpoint;
  }

  public void setClusterEndpoint(String clusterEndpoint) {
    this.clusterEndpoint = clusterEndpoint;
  }
}
