/* (C)2025 */
package net.joostvdg.kube_app_version.versions;

import java.util.Collections;
import java.util.Optional;
import net.joostvdg.kube_app_version.api.model.AppArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpAppArtifactRepository implements AppArtifactRepository {
  private static final Logger logger = LoggerFactory.getLogger(NoOpAppArtifactRepository.class);

  @Override
  public <S extends AppArtifact> S save(S entity) {
    logger.debug("Redis disabled - not saving AppArtifact: {}", entity.getSource());
    return entity;
  }

  @Override
  public <S extends AppArtifact> Iterable<S> saveAll(Iterable<S> entities) {
    logger.debug("Redis disabled - not saving AppArtifacts");
    return entities;
  }

  @Override
  public Optional<AppArtifact> findById(String id) {
    return Optional.empty();
  }

  @Override
  public boolean existsById(String id) {
    return false;
  }

  @Override
  public Iterable<AppArtifact> findAll() {
    return Collections.emptyList();
  }

  @Override
  public Iterable<AppArtifact> findAllById(Iterable<String> ids) {
    return Collections.emptyList();
  }

  @Override
  public long count() {
    return 0;
  }

  @Override
  public void deleteById(String id) {
    // No-op
  }

  @Override
  public void delete(AppArtifact entity) {
    // No-op
  }

  @Override
  public void deleteAllById(Iterable<? extends String> ids) {
    // No-op
  }

  @Override
  public void deleteAll(Iterable<? extends AppArtifact> entities) {
    // No-op
  }

  @Override
  public void deleteAll() {
    // No-op
  }
}
