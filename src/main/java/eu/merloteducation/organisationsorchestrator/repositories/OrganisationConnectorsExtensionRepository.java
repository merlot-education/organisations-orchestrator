package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganisationConnectorsExtensionRepository extends JpaRepository<OrganisationConnectorExtension, String> {
    List<OrganisationConnectorExtension> findAllByOrgaId(String orgaId);
}
