package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganisationConnectorsExtensionRepository extends JpaRepository<OrganisationConnectorExtension, String> {
    List<OrganisationConnectorExtension> findAllByOrgaId(String orgaId);
    Optional<OrganisationConnectorExtension> findByOrgaIdAndConnectorId(String orgaId, String connectorId);
}
