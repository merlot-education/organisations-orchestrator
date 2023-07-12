package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrganisationConnectorsExtensionRepository extends JpaRepository<OrganisationConnectorExtension, String> {
    Page<OrganisationConnectorExtension> findAllByOrganisation(String orgaId, Pageable pageable);
}
