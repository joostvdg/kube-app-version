/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class VersionRefreshService {

  @Value("${app.version.cache.minutes:60}")
  private long cacheValidMinutes;

  private final VersionComparatorService versionComparatorService;
  private final OutdatedArtifactInfoRepository outdatedArtifactInfoRepository;

  public VersionRefreshService(
      VersionComparatorService versionComparatorService,
      OutdatedArtifactInfoRepository outdatedArtifactInfoRepository) {
    this.versionComparatorService = versionComparatorService;
    this.outdatedArtifactInfoRepository = outdatedArtifactInfoRepository;
  }

  public List<OutdatedArtifactInfo> getOutdatedArtifacts() {
    if (needsRefresh()) {
      refreshOutdatedArtifacts();
    }

    List<OutdatedArtifactInfo> artifacts = fetchFromCache();
    return artifacts.isEmpty()
        ? versionComparatorService.getOutdatedArtifactsParallel()
        : artifacts;
  }

  private boolean needsRefresh() {
    Optional<OutdatedArtifactInfo> latestEntry = findLatestCachedEntry();
    return latestEntry.isEmpty()
        || Duration.between(
                    latestEntry.get().getLastUpdated(), LocalDateTime.now(ZoneId.systemDefault()))
                .toMinutes()
            >= cacheValidMinutes;
  }

  private Optional<OutdatedArtifactInfo> findLatestCachedEntry() {
    List<OutdatedArtifactInfo> all = fetchFromCache();
    return all.stream().max((a, b) -> a.getLastUpdated().compareTo(b.getLastUpdated()));
  }

  private List<OutdatedArtifactInfo> fetchFromCache() {
    List<OutdatedArtifactInfo> all = List.of();
    outdatedArtifactInfoRepository.findAll().forEach(all::add);
    return all;
  }

  @Scheduled(fixedDelayString = "${app.version.refresh.interval.ms:3600000}")
  public void refreshOutdatedArtifacts() {
    List<OutdatedArtifactInfo> outdated = versionComparatorService.getOutdatedArtifactsParallel();
    outdated.forEach(info -> info.setTimeToLive(cacheValidMinutes * 60));
    outdatedArtifactInfoRepository.saveAll(outdated);
  }
}
