/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutdatedArtifactInfoRepository
    extends CrudRepository<OutdatedArtifactInfo, String> {}
