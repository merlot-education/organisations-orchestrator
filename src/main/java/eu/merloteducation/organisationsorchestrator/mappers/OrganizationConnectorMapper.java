package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationConnectorMapper {
    OrganizationConnectorDto connectorExtensionToOrganizationConnectorDto(OrganisationConnectorExtension extension);
}
