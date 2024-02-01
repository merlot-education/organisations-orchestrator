package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationMetadataRepository extends JpaRepository<OrganizationMetadata, String> {
    Optional<OrganizationMetadata> findByOrgaId(String merlotId);

    List<OrganizationMetadata> findByMembershipClass(MembershipClass membershipClass);

    @Query("SELECT orgaId FROM OrganizationMetadata metadata WHERE metadata.active = :active")
    List<String> findByActive(@Param("active") boolean active);

    void deleteByOrgaId(String merlotId);
}
