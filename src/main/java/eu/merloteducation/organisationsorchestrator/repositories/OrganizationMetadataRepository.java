package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMetadataRepository extends JpaRepository<OrganizationMetadata, String> {
    List<OrganizationMetadata> findByMembershipClass(MembershipClass membershipClass);
}
