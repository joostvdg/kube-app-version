/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.joostvdg.kube_app_version.api.model.App;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import net.joostvdg.kube_app_version.api.model.AppVersion;
import net.joostvdg.kube_app_version.collectors.CollectorService;
import net.joostvdg.kube_app_version.config.OutdatedArtifactsProperties;
import net.joostvdg.kube_app_version.versions.util.SemanticVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutdatedArtifactsService {

  private static final Logger logger = LoggerFactory.getLogger(OutdatedArtifactsService.class);
  private final CollectorService collectorService;
  private final List<VersionFetcher> versionFetchers;
  private final AppArtifactRepository appVersionRepository;
  private final OutdatedArtifactInfoRepository outdatedArtifactInfoRepository;
  private final OutdatedArtifactsProperties properties;

  public OutdatedArtifactsService(
      CollectorService collectorService,
      List<VersionFetcher> versionFetchers,
      AppArtifactRepository appVersionRepository,
      OutdatedArtifactInfoRepository outdatedArtifactInfoRepository,
      OutdatedArtifactsProperties properties) {
    this.collectorService = collectorService;
    this.versionFetchers = versionFetchers;
    this.appVersionRepository = appVersionRepository;
    this.outdatedArtifactInfoRepository = outdatedArtifactInfoRepository;
    this.properties = properties;
    logger.info(
        "OutdatedArtifactsService initialized with {} version fetchers.",
        Optional.of(versionFetchers.size()));
  }

  @PostConstruct
  public void onStartup() {
    logger.info("OutdatedArtifactsService startup initiated.");
    if (properties.isCollectOnStartup()) {
      logger.info("Collecting all available versions for all app artifacts on startup...");
      getAvailableVersionsForAllAppArtifacts();
      logger.info("Finished collecting available versions.");
    } else {
      logger.info("Skipping collection of available versions on startup as per configuration.");
    }

    if (properties.isRefreshOnStartup()) {
      logger.info("Refreshing outdated artifacts cache on startup...");
      refreshOutdatedArtifacts();
      logger.info("Finished refreshing outdated artifacts cache.");
    } else {
      logger.info("Skipping refresh of outdated artifacts cache on startup as per configuration.");
    }
  }

  public List<OutdatedArtifactInfo> getOutdatedArtifacts() {
    if (needsRefresh()) {
      refreshOutdatedArtifacts();
    }

    List<OutdatedArtifactInfo> artifacts = fetchFromCache();
    return artifacts.isEmpty() ? getOutdatedArtifactsParallel() : artifacts;
  }

  private boolean needsRefresh() {
    Optional<OutdatedArtifactInfo> latestEntry = findLatestCachedEntry();
    return latestEntry.isEmpty()
        || Duration.between(
                    latestEntry.get().getLastUpdated(), LocalDateTime.now(ZoneId.systemDefault()))
                .toMinutes()
            >= properties.getCacheValidityMinutes();
  }

  private Optional<OutdatedArtifactInfo> findLatestCachedEntry() {
    List<OutdatedArtifactInfo> all = fetchFromCache();
    return all.stream().max(Comparator.comparing(OutdatedArtifactInfo::getLastUpdated));
  }

  private List<OutdatedArtifactInfo> fetchFromCache() {
    List<OutdatedArtifactInfo> all = new ArrayList<>();
    outdatedArtifactInfoRepository.findAll().forEach(all::add);
    return all;
  }

  @Scheduled(fixedDelayString = "${app.version.outdated-artifacts.interval-ms:3600000}")
  public void refreshOutdatedArtifacts() {
    List<OutdatedArtifactInfo> outdated = getOutdatedArtifactsParallel();
    outdated.forEach(info -> info.setTimeToLive(properties.getCacheValidityMinutes() * 60));
    outdatedArtifactInfoRepository.saveAll(outdated);
  }

  private String determineCurrentArtifactVersion(AppArtifact artifact, AppVersion appVersion) {
    String artifactType = artifact.getArtifactType();
    if (artifactType == null) return null;

    return switch (artifactType.toLowerCase(Locale.ROOT)) {
      case "helm", "git" -> appVersion.getVersion();
      case "containerimage" -> {
        String source = artifact.getSource();
        if (source == null) yield null;
        int colonIndex = source.lastIndexOf(':');
        int atIndex = source.lastIndexOf('@');

        if (colonIndex > 0 && (atIndex == -1 || colonIndex > atIndex)) {
          String tag = source.substring(colonIndex + 1);
          yield SemanticVersionUtil.parseVersion(tag)
              .map(com.github.zafarkhaja.semver.Version::toString)
              .orElse(tag);
        } else if (atIndex > 0 && colonIndex == -1) {
          logger.debug(
              "Artifact {} uses a digest '{}'. Digest comparison is not typical for 'latest"
                  + " version' checks.",
              source,
              source.substring(atIndex + 1));
          yield null;
        }
        logger.warn("Could not parse version tag from containerImage source: {}", source);
        yield null;
      }
      default -> {
        logger.debug("Unsupported artifact type for version determination: {}", artifactType);
        yield null;
      }
    };
  }

  @Cacheable(value = "availableVersions")
  public Map<String, List<String>> getAvailableVersionsForAllAppArtifacts() {
    long startTime = System.nanoTime();
    Set<App> apps = collectorService.getAllCollectedApps();
    Map<String, List<String>> artifactVersionsMap = new HashMap<>();

    for (App app : apps) {
      for (AppVersion appVersion : app.getVersions()) {
        for (AppArtifact artifact : appVersion.getArtifacts()) {
          if (artifact.getSource() == null || artifact.getArtifactType() == null) {
            logger.debug(
                "Skipping artifact for app '{}' due to missing source or type: {}",
                app.getName(),
                artifact);
            continue;
          }

          for (VersionFetcher fetcher : versionFetchers) {
            if (fetcher.supports(artifact)) {
              try {
                String mapKey =
                    app.getName() + "::" + artifact.getArtifactType() + "::" + artifact.getSource();
                List<String> availableVersions = fetcher.getAvailableVersions(artifact);
                artifactVersionsMap.put(mapKey, availableVersions);
              } catch (Exception e) {
                String errorKey =
                    app.getName()
                        + "::"
                        + artifact.getArtifactType()
                        + "::"
                        + artifact.getSource()
                        + "::ERROR";
                artifactVersionsMap.put(errorKey, List.of(e.getMessage()));
                logger.error(
                    "Error fetching versions for artifact {}: {}",
                    artifact.getSource(),
                    e.getMessage(),
                    e);
              }
              break;
            }
          }
        }
      }
    }
    long endTime = System.nanoTime();
    long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    logger.info(
        "getAvailableVersionsForAllAppArtifacts completed in {} ms, processed {} artifacts.",
        durationMillis,
        artifactVersionsMap.size());
    return artifactVersionsMap;
  }

  private List<OutdatedArtifactInfo> getOutdatedArtifactsParallel() {
    long startTime = System.nanoTime();
    ExecutorService executor =
        Executors.newFixedThreadPool(10); // Adjust thread pool size as needed
    List<CompletableFuture<Optional<OutdatedArtifactInfo>>> futures = new ArrayList<>();
    Set<App> apps = collectorService.getAllCollectedApps();

    for (App app : apps) {
      AppVersion currentAppVersion = app.getCurrentVersion();
      for (AppArtifact artifact : currentAppVersion.getArtifacts()) {
        CompletableFuture<Optional<OutdatedArtifactInfo>> future =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return processArtifact(app, currentAppVersion, artifact);
                  } catch (Exception e) {
                    logger.error(
                        "Error processing artifact {}: {}",
                        artifact.getSource(),
                        e.getMessage(),
                        e);
                    return Optional.empty();
                  }
                },
                executor);
        futures.add(future);
      }
    }

    List<OutdatedArtifactInfo> outdatedList =
        futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .flatMap(Optional::stream)
            .toList();

    executor.shutdown();
    long endTime = System.nanoTime();
    long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    logger.info(
        "getOutdatedArtifactsParallel completed in {} ms, found {} outdated artifacts.",
        durationMillis,
        outdatedList.size());
    return outdatedList;
  }

  private Optional<OutdatedArtifactInfo> processArtifact(
      App app, AppVersion appVersion, AppArtifact artifact) throws Exception {
    saveAppArtifact(artifact);
    String currentArtifactVersionStr = determineCurrentArtifactVersion(artifact, appVersion);

    if (currentArtifactVersionStr == null
        || "unknown".equalsIgnoreCase(currentArtifactVersionStr)) {
      logger.debug(
          "Could not determine a valid current version for artifact {} (type: {}) in app {},"
              + " skipping comparison.",
          artifact.getSource(),
          artifact.getArtifactType(),
          app.getName());
      return Optional.empty();
    }

    // Count number of dots, if we only have X.Y, we add .0 at the end
    int dotCount = currentArtifactVersionStr.split("\\.").length;
    if (dotCount == 1) {
      if (currentArtifactVersionStr.contains("-")) {
        int hyphenIndex = currentArtifactVersionStr.indexOf('-');
        String suffix = currentArtifactVersionStr.substring(hyphenIndex);
        currentArtifactVersionStr =
            currentArtifactVersionStr.substring(0, hyphenIndex) + ".0" + suffix;
      } else {
        currentArtifactVersionStr += ".0";
      }
    }

    // TODO: can this be removed?
    //    Optional<com.github.zafarkhaja.semver.Version> currentParsedVersionOpt =
    //        SemanticVersionUtil.parseVersion(currentArtifactVersionStr);

    for (VersionFetcher fetcher : versionFetchers) {
      if (fetcher.supports(artifact)) {
        try {
          List<String> availableVersions = fetcher.getAvailableVersions(artifact);
          if (availableVersions == null || availableVersions.isEmpty()) {
            return Optional.empty();
          }

          Optional<com.github.zafarkhaja.semver.Version> latestOverallOpt =
              SemanticVersionUtil.getLatestOverallVersion(availableVersions);
          Optional<com.github.zafarkhaja.semver.Version> latestGAOpt =
              SemanticVersionUtil.getLatestGARelease(availableVersions);

          String latestGAVersionStr = latestGAOpt.map(Object::toString).orElse(null);
          String latestOverallVersionStr = latestOverallOpt.map(Object::toString).orElse(null);

          boolean isOutdated =
              (latestGAVersionStr != null
                      && SemanticVersionUtil.isOutdated(
                          currentArtifactVersionStr, latestGAVersionStr))
                  || (latestGAVersionStr == null
                      && latestOverallVersionStr != null
                      && SemanticVersionUtil.isOutdated(
                          currentArtifactVersionStr, latestOverallVersionStr));

          if (isOutdated) {
            return Optional.of(
                createOutdatedInfo(
                    app,
                    appVersion,
                    artifact,
                    currentArtifactVersionStr,
                    availableVersions,
                    latestOverallOpt,
                    latestGAOpt));
          }
        } catch (Exception e) {
          logger.error(
              "Error fetching or comparing versions for artifact {}: {}",
              artifact.getSource(),
              e.getMessage(),
              e);
        }
        break;
      }
    }
    return Optional.empty();
  }

  private OutdatedArtifactInfo createOutdatedInfo(
      App app,
      AppVersion appVersion,
      AppArtifact artifact,
      String currentVersion,
      List<String> availableVersions,
      Optional<com.github.zafarkhaja.semver.Version> latestOverallOpt,
      Optional<com.github.zafarkhaja.semver.Version> latestGAOpt) {

    Optional<String> nextMinorOpt =
        SemanticVersionUtil.findNextMinorVersion(currentVersion, availableVersions);
    Optional<String> nextMajorOpt =
        SemanticVersionUtil.findNextMajorVersion(currentVersion, availableVersions);
    Optional<com.github.zafarkhaja.semver.Version> currentParsedVersionOpt =
        SemanticVersionUtil.parseVersion(currentVersion);

    return new OutdatedArtifactInfo(
        app.getName(),
        app.getId(),
        appVersion.getVersion(),
        artifact.getSource(),
        artifact.getArtifactType(),
        currentVersion,
        latestOverallOpt.map(Object::toString).orElse(null),
        latestGAOpt.map(Object::toString).orElse(null),
        SemanticVersionUtil.getLatestPreRelease(availableVersions)
            .map(Object::toString)
            .orElse(null),
        nextMinorOpt.orElse(null),
        nextMajorOpt.orElse(null),
        SemanticVersionUtil.calculateMajorVersionDelta(currentParsedVersionOpt, nextMajorOpt)
            .orElse(null),
        SemanticVersionUtil.calculateMinorVersionDelta(currentParsedVersionOpt, nextMinorOpt)
            .orElse(null),
        availableVersions);
  }

  public void saveAppArtifact(AppArtifact appArtifact) {
    try {
      appVersionRepository.save(appArtifact);
    } catch (Exception e) {
      logger.warn("Failed to save artifact {}: {}", appArtifact.getSource(), e.getMessage());
    }
  }

  @SuppressWarnings("MixedMutabilityReturnType")
  public List<AppArtifact> getAllAppArtifacts() {
    try {
      List<AppArtifact> appArtifacts = new ArrayList<>();
      appVersionRepository.findAll().forEach(appArtifacts::add);
      return appArtifacts;
    } catch (Exception e) {
      logger.warn("Failed to fetch artifacts: {}", e.getMessage());
      return Collections.emptyList();
    }
  }
}
