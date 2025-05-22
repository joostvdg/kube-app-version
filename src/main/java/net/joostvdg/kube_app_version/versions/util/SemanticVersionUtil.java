package net.joostvdg.kube_app_version.versions.util;

// This is critical. Real semantic version comparison is non-trivial. You should replace this with a robust library like com.github.zafarkhaja:java-semver.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
// import com.github.zafarkhaja.semver.Version; // Example if using java-semver

public class SemanticVersionUtil {

    private static final Logger logger = LoggerFactory.getLogger(SemanticVersionUtil.class);

    /**
     * Determines the latest version from a list.
     * IMPORTANT: This is a placeholder. It assumes versions are somewhat sortable as strings
     * or that the fetcher provides them sorted (latest first).
     * For robust results, use a proper semantic version sorting mechanism.
     */
    public static String getLatestVersion(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        // The HelmChartVersionFetcher sorts them latest first.
        // If other fetchers don't, this needs to be more robust.
        // For example, using java-semver:
        // return versions.stream()
        //       .map(s -> {
        //           try { return Version.valueOf(s); }
        //           catch (Exception e) { logger.warn("Invalid version format: {}", s); return null; }
        //       })
        //       .filter(Objects::nonNull)
        //       .max(Version::compareTo)
        //       .map(Version::toString)
        //       .orElse(null);

        // Simple approach assuming fetchers sort latest first (like HelmChartVersionFetcher does)
        return versions.get(0);
    }

    /**
     * Checks if the current version is outdated compared to the latest available version.
     * IMPORTANT: This is a placeholder. It uses simple string inequality.
     * For robust results, use proper semantic version comparison.
     *
     * @param currentVersion The version currently in use.
     * @param latestAvailableVersion The latest version available.
     * @return true if currentVersion is considered older than latestAvailableVersion.
     */
    public static boolean isOutdated(String currentVersion, String latestAvailableVersion) {
        if (currentVersion == null || latestAvailableVersion == null) {
            return false; // Cannot compare
        }
        // Remove common prefixes like 'v' for more consistent comparison if not using a semver library
        String cleanCurrent = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;
        String cleanLatest = latestAvailableVersion.startsWith("v") ? latestAvailableVersion.substring(1) : latestAvailableVersion;

        // Proper semantic version comparison is needed here.
        // Example using java-semver:
        // try {
        //     Version current = Version.valueOf(cleanCurrent);
        //     Version latest = Version.valueOf(cleanLatest);
        //     return latest.greaterThan(current);
        // } catch (Exception e) {
        //     logger.warn("Failed to compare versions semantically: '{}' vs '{}'. Falling back to string comparison. Error: {}", cleanCurrent, cleanLatest, e.getMessage());
        //     // Fallback to simple inequality if parsing fails (not ideal)
        //     return !cleanCurrent.equals(cleanLatest);
        // }

        // Highly simplified placeholder:
        // If they are not equal, and getLatestVersion picked the "latest", assume it's newer.
        // This is NOT robust for complex versions (e.g. 1.0.0-alpha vs 1.0.0).
        if (cleanCurrent.equals(cleanLatest)) {
            return false;
        }
        // This basic check assumes that if strings are different, and one is "latest", it's an update.
        // This will have false positives/negatives for pre-releases, build metadata, etc.
        // For example, "1.0.0" vs "1.0.0-SNAPSHOT" - this logic might incorrectly say 1.0.0 is outdated if 1.0.0-SNAPSHOT is "latest".
        // Or "1.10.0" vs "1.2.0" - string sort would be wrong.
        // The getLatestVersion() method MUST ensure truly the latest is returned.
        logger.warn("Using basic string inequality for version comparison: '{}' vs '{}'. Strongly recommend implementing proper semantic versioning.", cleanCurrent, cleanLatest);
        return true; // If not equal, and latest is determined correctly, then it's "outdated".
        // This relies heavily on getLatestVersion being accurate.
    }
}