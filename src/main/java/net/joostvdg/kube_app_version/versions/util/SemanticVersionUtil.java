package net.joostvdg.kube_app_version.versions.util;

import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SemanticVersionUtil {

    private static final Logger logger = LoggerFactory.getLogger(SemanticVersionUtil.class);
    // Pattern to identify versions like "X.Y" (e.g., "1.2", "10.0")
    private static final Pattern XY_PATTERN = Pattern.compile("^(\\d+\\.\\d+)$");
    // Pattern to identify versions like "X.Y-prerelease" (e.g., "1.2-alpha", "10.0-beta.1")
    private static final Pattern XY_PRERELEASE_PATTERN = Pattern.compile("^(\\d+\\.\\d+)-([a-zA-Z0-9][a-zA-Z0-9.-]*)$");


    public static Optional<Version> parseVersion(String versionStr) {
        if (versionStr == null) {
            return Optional.empty();
        }

        String originalVersionString = versionStr;
        String processedString = versionStr;

        if (processedString.startsWith("v")) {
            processedString = processedString.substring(1);
        }

        Optional<Version> parsedOpt;

        parsedOpt = Version.tryParse(processedString, false);
        if (parsedOpt.isPresent()) {
            return parsedOpt;
        }

        Matcher xyMatcher = XY_PATTERN.matcher(processedString);
        if (xyMatcher.matches()) {
            String majorMinor = xyMatcher.group(1);
            String normalizedVersion = majorMinor + ".0";
            logger.debug("Normalizing X.Y format: '{}' (original: '{}') to '{}'",
                    processedString, originalVersionString, normalizedVersion);
            parsedOpt = Version.tryParse(normalizedVersion, false);
            if (parsedOpt.isPresent()) {
                return parsedOpt;
            } else {
                logger.warn("Normalization of X.Y format to '{}' still resulted in unparsable version. Original: '{}'", normalizedVersion, originalVersionString);
            }
        }

        Matcher xyPrereleaseMatcher = XY_PRERELEASE_PATTERN.matcher(processedString);
        if (xyPrereleaseMatcher.matches()) {
            String majorMinor = xyPrereleaseMatcher.group(1);
            String prerelease = xyPrereleaseMatcher.group(2);
            String normalizedVersion = majorMinor + ".0-" + prerelease;
            logger.debug("Normalizing X.Y-prerelease format: '{}' (original: '{}') to '{}'",
                    processedString, originalVersionString, normalizedVersion);
            parsedOpt = Version.tryParse(normalizedVersion, false);
            if (parsedOpt.isPresent()) {
                return parsedOpt;
            } else {
                logger.warn("Normalization of X.Y-prerelease format to '{}' still resulted in unparsable version. Original: '{}'", normalizedVersion, originalVersionString);
            }
        }

        if (!parsedOpt.isPresent()) {
            logger.warn("Invalid semantic version format, all normalization attempts failed. Original: '{}', Cleaned: '{}'.",
                    originalVersionString, processedString);
        }
        return parsedOpt;
    }

    private static List<Version> parseAll(List<String> versionStrings) {
        if (versionStrings == null || versionStrings.isEmpty()) {
            return Collections.emptyList();
        }
        return versionStrings.stream()
                .map(SemanticVersionUtil::parseVersion)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static Optional<Version> getLatestOverallVersion(List<String> availableVersionStrings) {
        if (availableVersionStrings == null || availableVersionStrings.isEmpty()) {
            return Optional.empty();
        }
        return parseVersion(availableVersionStrings.get(0));
    }

    public static Optional<Version> getLatestGARelease(List<String> availableVersionStrings) {
        List<Version> parsedVersions = parseAll(availableVersionStrings);
        return parsedVersions.stream()
                .filter(v -> !v.isPreRelease())
                .max(Version::compareTo);
    }

    public static Optional<Version> getLatestPreRelease(List<String> availableVersionStrings) {
        List<Version> parsedVersions = parseAll(availableVersionStrings);
        return parsedVersions.stream()
                .filter(Version::isPreRelease)
                .max(Version::compareTo);
    }

    public static boolean isOutdated(String currentVersionStr, String latestGAVersionStr) {
        Optional<Version> currentOpt = parseVersion(currentVersionStr);
        Optional<Version> latestGAOpt = parseVersion(latestGAVersionStr);

        if (currentOpt.isEmpty() || latestGAOpt.isEmpty()) {
            logger.debug("Cannot compare versions due to parsing error: current='{}', latestGA='{}'", currentVersionStr, latestGAVersionStr);
            return false;
        }
        return latestGAOpt.get().greaterThan(currentOpt.get());
    }

    public static Optional<String> findNextMinorVersion(String currentVersionStr, List<String> availableVersionStrings) {
        Optional<Version> currentOpt = parseVersion(currentVersionStr);
        if (currentOpt.isEmpty()) {
            return Optional.empty();
        }
        Version current = currentOpt.get();
        List<Version> availableParsed = parseAll(availableVersionStrings);

        var versionsWithinSameMinor = availableParsed.
                stream()
                .filter(v -> !v.isPreRelease()) // Only consider GA releases
                .filter(v -> v.majorVersion() == current.majorVersion()) // Same Major
                .filter(v -> v.minorVersion() == current.minorVersion()) // Same Minor
                .toList();

        for (Version v : versionsWithinSameMinor) {
            logger.info("Found minor version options: {}", v);
        }

        return versionsWithinSameMinor.stream()
                .filter(v -> v.isHigherThanOrEquivalentTo(current)) // Newer than current (handles minor or patch increase)
                .max(Version::compareTo)
                .map(Version::toString);
    }

    public static Optional<String> findNextMajorVersion(String currentVersionStr, List<String> availableVersionStrings) {
        Optional<Version> currentOpt = parseVersion(currentVersionStr);
        if (currentOpt.isEmpty()) {
            return Optional.empty();
        }
        Version current = currentOpt.get();
        List<Version> availableParsed = parseAll(availableVersionStrings);

        // We want the highest Minor version within the same Major version
        return availableParsed.stream()
                .filter(v -> !v.isPreRelease()) // Only consider GA releases
                .filter(v -> v.majorVersion() == current.majorVersion()) // Same Major
                .filter(v -> v.isHigherThanOrEquivalentTo(current)) // Newer than current (handles minor or patch increase)
                .max(Version::compareTo) // Get the latest within that next major series
                .map(Version::toString);
    }

    /**
     * Calculates the difference in major versions between the current version and the latest GA release.
     *
     * @param currentVersionOpt Optional current version.
     * @param latestGAReleaseOpt Optional latest GA release.
     * @return The difference in major versions, or null if comparison isn't possible or not applicable.
     */
    public static Optional<Integer> calculateMajorVersionDelta(Optional<Version> currentVersionOpt, Optional<Version> latestGAReleaseOpt) {
        if (currentVersionOpt.isPresent() && latestGAReleaseOpt.isPresent()) {
            Version current = currentVersionOpt.get();
            Version latestGA = latestGAReleaseOpt.get();
            if (latestGA.getMajorVersion() > current.getMajorVersion()) {
                return Optional.of(latestGA.getMajorVersion() - current.getMajorVersion());
            }
            return Optional.of(0); // Same major or current is somehow newer (shouldn't happen if latestGA is truly latest)
        }
        return Optional.empty(); // Not enough info to calculate
    }

    /**
     * Finds the latest GA release within the same major series as the current version.
     *
     * @param currentVersionOpt Optional current version.
     * @param availableVersionStrings List of all available version strings.
     * @return Optional latest GA version in the same major series.
     */
    public static Optional<Version> findLatestMinorInSameMajorSeries(Optional<Version> currentVersionOpt, List<String> availableVersionStrings) {
        if (currentVersionOpt.isEmpty()) {
            return Optional.empty();
        }
        Version current = currentVersionOpt.get();
        List<Version> availableParsed = parseAll(availableVersionStrings);

        return availableParsed.stream()
                .filter(v -> !v.isPreRelease()) // Only GA
                .filter(v -> v.getMajorVersion() == current.getMajorVersion()) // Same Major
                .max(Version::compareTo);
    }


    /**
     * Calculates the difference in minor versions between the current version and
     * the latest GA release within the same major series.
     *
     * @param currentVersionOpt Optional current version.
     * @param latestMinorInSameMajorOpt Optional latest GA release in the same major series.
     * @return The difference in minor versions, or null if comparison isn't possible or not applicable.
     */
    public static Optional<Integer> calculateMinorVersionDelta(Optional<Version> currentVersionOpt, Optional<Version> latestMinorInSameMajorOpt) {
        if (currentVersionOpt.isPresent() && latestMinorInSameMajorOpt.isPresent()) {
            Version current = currentVersionOpt.get();
            Version latestMinorGA = latestMinorInSameMajorOpt.get();

            // Ensure they are indeed in the same major series for a meaningful minor delta
            if (current.getMajorVersion() == latestMinorGA.getMajorVersion()) {
                if (latestMinorGA.getMinorVersion() > current.getMinorVersion()) {
                    return Optional.of(latestMinorGA.getMinorVersion() - current.getMinorVersion());
                }
                return Optional.of(0); // Same minor or current is somehow newer
            } else {
                logger.debug("Cannot calculate minor delta between different major versions: current={}, latestMinorGA={}", current, latestMinorGA);
            }
        }
        return Optional.empty(); // Not enough info or different majors
    }
}