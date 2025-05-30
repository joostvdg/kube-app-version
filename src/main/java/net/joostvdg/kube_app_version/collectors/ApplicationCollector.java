/* (C)2025 */
package net.joostvdg.kube_app_version.collectors;

import java.util.Set;
import net.joostvdg.kube_app_version.api.model.App;

public interface ApplicationCollector {
  Set<App> getCollectedApplications();
}
