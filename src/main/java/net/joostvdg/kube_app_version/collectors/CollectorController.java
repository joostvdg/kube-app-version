package net.joostvdg.kube_app_version.collectors;

import net.joostvdg.kube_app_version.api.model.App;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class CollectorController {

    private final List<ApplicationCollector> applicationCollectors; // Changed to a List

    // Inject a List of all ApplicationCollector implementations
    public CollectorController(List<ApplicationCollector> applicationCollectors) {
        this.applicationCollectors = applicationCollectors;
    }

    @GetMapping("/api/apps")
    public Set<App> getCollectedApplications() {
        // Iterate over all collectors, get their apps, and collect them into a single set
        return applicationCollectors.stream()
                .map(ApplicationCollector::getCollectedApplications) // Get apps from each collector
                .flatMap(Collection::stream) // Flatten the stream of Set<App> into a Stream<App>
                .collect(Collectors.toSet()); // Collect into a single Set<App> to ensure uniqueness
    }
}