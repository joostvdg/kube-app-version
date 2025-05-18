package net.joostvdg.kube_app_version.collectors;

import net.joostvdg.kube_app_version.api.model.App;

import java.util.Set;

public interface ApplicationCollector {
    Set<App> getCollectedApplications();
}