package net.joostvdg.kube_app_version.collectors;

import net.joostvdg.kube_app_version.api.model.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollectorService {

    private static final Logger logger = LoggerFactory.getLogger(CollectorService.class);
    private final List<ApplicationCollector> applicationCollectors;

    public CollectorService(List<ApplicationCollector> applicationCollectors) {
        this.applicationCollectors = applicationCollectors;
        logger.info("CollectorService initialized with {} application collectors.", applicationCollectors.size());
    }

    public Set<App> getAllCollectedApps() {
        logger.debug("Fetching all collected apps from {} collectors.", applicationCollectors.size());
        Set<App> allApps = applicationCollectors.stream()
                .map(ApplicationCollector::getCollectedApplications)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        logger.info("Total apps collected from all sources: {}", allApps.size());
        return allApps;
    }
}