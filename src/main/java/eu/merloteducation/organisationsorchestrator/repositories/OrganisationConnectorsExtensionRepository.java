package eu.merloteducation.organisationsorchestrator.repositories;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganisationConnectorsExtensionRepository extends JpaRepository<OrganisationConnectorExtension, String> {

}
