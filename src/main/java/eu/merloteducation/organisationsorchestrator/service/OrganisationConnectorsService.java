package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.api.organization.PatchOrganisationConnectorModel;
import eu.merloteducation.modelslib.api.organization.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationConnectorMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.beans.factory.annotation.Autowired;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganisationConnectorsService {

    @Autowired
    private OrganisationConnectorsExtensionRepository connectorsRepo;

    @Autowired
    private OrganizationConnectorMapper organizationConnectorMapper;

    /**
     * Given an organization id, return all related connectors.
     *
     * @param orgaId organization id
     * @return connectors of organization
     */
    public List<OrganizationConnectorDto> getAllConnectors(String orgaId) {
        return connectorsRepo.findAllByOrgaId(orgaId).stream()
                .map(o -> organizationConnectorMapper.connectorExtensionToOrganizationConnectorDto(o))
                .toList();
    }

    /**
     * Given an organization id and a connector id, return the specific connector.
     *
     * @param orgaId organization id
     * @param connectorId connector id
     * @return connector
     */
    public OrganizationConnectorDto getConnector(String orgaId, String connectorId) {
        return organizationConnectorMapper.connectorExtensionToOrganizationConnectorDto(
                connectorsRepo.findByOrgaIdAndConnectorId(orgaId, connectorId).orElse(null));
    }

    /**
     * Given an organization id and a model of a new connector, create the connector entry in the database.
     *
     * @param orgaId organization id
     * @param postModel model of new connector
     * @return newly created connector
     */
    public OrganizationConnectorDto postConnector(String orgaId, PostOrganisationConnectorModel postModel) {
        OrganisationConnectorExtension connector = new OrganisationConnectorExtension();
        connector.setOrgaId(orgaId);
        connector.setConnectorId((postModel.getConnectorId()));
        connector.setConnectorEndpoint(postModel.getConnectorEndpoint());
        connector.setConnectorAccessToken(postModel.getConnectorAccessToken());
        connector.setBucketNames(postModel.getBucketNames());

        return organizationConnectorMapper.connectorExtensionToOrganizationConnectorDto(connectorsRepo.save(connector));
    }

    /**
     * Given an organization id, a connector id and a model of an updated connector, update the respective fields
     * of the connector in the database.
     *
     * @param orgaId organization id
     * @param connectorId connector id
     * @param patchModel updated model of connector
     * @return updated connector
     */
    public OrganizationConnectorDto patchConnector(String orgaId, String connectorId, PatchOrganisationConnectorModel patchModel) {

        OrganisationConnectorExtension connector =  connectorsRepo.findByOrgaIdAndConnectorId(orgaId, connectorId).orElse(null);
        if(connector == null){
            return null;
        }

        connector.setConnectorEndpoint(patchModel.getConnectorEndpoint());
        connector.setConnectorAccessToken(patchModel.getConnectorAccessToken());
        connector.setBucketNames(patchModel.getBucketNames());

        return organizationConnectorMapper.connectorExtensionToOrganizationConnectorDto(connectorsRepo.save(connector));
    }

    /**
     * Delete the connector by the given database connector id.
     *
     * @param id database connector id
     */
    public void deleteConnector(String id) {
        connectorsRepo.deleteById(id);
    }

}
