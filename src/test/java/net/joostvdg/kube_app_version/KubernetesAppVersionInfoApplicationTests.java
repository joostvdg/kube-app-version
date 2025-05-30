/* (C)2025 */
package net.joostvdg.kube_app_version;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {TestKubernetesClientConfig.class})
@SpringBootTest
class KubernetesAppVersionInfoApplicationTests {

  @Test
  void contextLoads() {}
}
