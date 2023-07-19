package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.springframework.beans.factory.annotation.Autowired;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganisationConnectorsService {

    @Autowired
    private OrganisationConnectorsExtensionRepository connectorsRepo;


    public List<OrganisationConnectorExtension> getAllConnectors(String orgaId) throws Exception {
        List<OrganisationConnectorExtension> connectors = connectorsRepo.findAllByOrgaId(orgaId);

        return connectors;
    }

    public OrganisationConnectorExtension getConnector(String orgaId, String connectorId) throws Exception {
        OrganisationConnectorExtension connector =  connectorsRepo.findById(connectorId).orElse(null);

        return connector;
    }

    public OrganisationConnectorExtension postConnector(String orgaId, PostOrganisationConnectorModel postModel) throws Exception {
        OrganisationConnectorExtension connector = new OrganisationConnectorExtension();
        connector.setOrgaId(orgaId);
        connector.setId((postModel.getId()));
        connector.setConnectorEndpoint(postModel.getConnectorEndpoint());
        connector.setConnectorAccessToken(postModel.getConnectorAccessToken());
        connector.setBucketNames(postModel.getBucketNames());

        connectorsRepo.save(connector);
        return connector;
    }

    public OrganisationConnectorExtension patchConnector(String orgaIdId, String connectorId, PatchOrganisationConnectorModel patchModel) throws Exception {

        OrganisationConnectorExtension connector =  connectorsRepo.findById(connectorId).orElse(null);
        if(connector == null){
            return null;
        }

        connector.setConnectorEndpoint(patchModel.getConnectorEndpoint());
        connector.setConnectorAccessToken(patchModel.getConnectorAccessToken());
        connector.setBucketNames(patchModel.getBucketNames());

        connectorsRepo.save(connector);
        return connector;
    }

    public void deleteConnector(String connectorId) throws Exception {
        connectorsRepo.deleteById(connectorId);
    }

}
