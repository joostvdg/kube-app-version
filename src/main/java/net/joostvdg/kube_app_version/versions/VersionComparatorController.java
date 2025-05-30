/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.util.List;
import net.joostvdg.kube_app_version.versions.dto.OutdatedArtifactInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/versions")
public class VersionComparatorController {

  private final VersionComparatorService versionComparatorService;

  public VersionComparatorController(VersionComparatorService versionComparatorService) {
    this.versionComparatorService = versionComparatorService;
  }

  @GetMapping("/outdated")
  public List<OutdatedArtifactInfo> getOutdatedApplications() {
    return versionComparatorService.getOutdatedArtifacts();
  }

  // You could also expose the raw available versions if needed
  // @GetMapping("/available")
  // public Map<String, List<String>> getAllAvailableVersions() {
  //     return versionComparatorService.getAvailableVersionsForAllAppArtifacts();
  // }
}
