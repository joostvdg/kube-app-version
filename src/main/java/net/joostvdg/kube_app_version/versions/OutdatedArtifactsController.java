/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.util.List;
import java.util.Map;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/versions")
public class OutdatedArtifactsController {

  private final OutdatedArtifactsService outdatedArtifactsService;

  public OutdatedArtifactsController(OutdatedArtifactsService outdatedArtifactsService) {
    this.outdatedArtifactsService = outdatedArtifactsService;
  }

  @GetMapping("/outdated")
  public List<OutdatedArtifactInfo> getOutdatedApplications() {
    return outdatedArtifactsService.getOutdatedArtifacts();
  }

  @GetMapping("/available")
  public Map<String, List<String>> getAllAvailableVersions() {
    return outdatedArtifactsService.getAvailableVersionsForAllAppArtifacts();
  }

  // TODO: temp debug, remove or move
  @GetMapping("/artifacts")
  public List<AppArtifact> getAllAppArtifacts() {
    return outdatedArtifactsService.getAllAppArtifacts();
  }
}
