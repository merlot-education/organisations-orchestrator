package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationMetadataRepository extends JpaRepository<OrganizationMetadata, String> {
    Optional<OrganizationMetadata> findByMerlotId(String merlotId);

    void deleteByMerlotId(String merlotId);
}
