/* (C)2025 */
package net.joostvdg.kube_app_version.collectors;

import java.util.Set;
import net.joostvdg.kube_app_version.api.model.App;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CollectorController {

  private final CollectorService collectorService;

  public CollectorController(CollectorService collectorService) {
    this.collectorService = collectorService;
  }

  @GetMapping("/api/apps") // Changed endpoint from /api/collected-apps for consistency
  public Set<App> getCollectedApplications() {
    return collectorService.getAllCollectedApps();
  }
}
