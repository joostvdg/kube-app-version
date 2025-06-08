/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import net.joostvdg.kube_app_version.api.model.AppArtifact;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppArtifactRepository extends CrudRepository<AppArtifact, String> {}
