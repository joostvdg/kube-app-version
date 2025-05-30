/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.util.List;
import net.joostvdg.kube_app_version.api.model.AppArtifact;

public interface VersionFetcher {

  /**
   * Fetches a list of available versions for the given artifact.
   *
   * @param artifact The application artifact (e.g., Helm chart, Git repo).
   * @return A list of available version strings.
   * @throws Exception if an error occurs during version fetching.
   */
  List<String> getAvailableVersions(AppArtifact artifact) throws Exception;

  /**
   * Checks if this version fetcher supports the given artifact.
   *
   * @param artifact The application artifact.
   * @return true if this fetcher can handle the artifact type, false otherwise.
   */
  boolean supports(AppArtifact artifact);
}
