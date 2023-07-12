package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.organisationsorchestrator.models.OrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import eu.merloteducation.organisationsorchestrator.repositories.IOrganisationConnectorsExtensionRepository;

import java.security.Principal;
import java.util.List;

public class OrganisationConnectorsService {

    @Autowired
    private IOrganisationConnectorsExtensionRepository connectorsRepo;


    public List<OrganisationConnectorModel> getAllConnectors(String orgaId) throws Exception {
        return null;
    }

    public OrganisationConnectorModel getConnector(String connectorId) throws Exception {
        return null;
    }

    public OrganisationConnectorModel postConnector(PostOrganisationConnectorModel postModel) throws Exception {
        return null;
    }

    public OrganisationConnectorModel updateConnector(String connectorId, PatchOrganisationConnectorModel patchModel) throws Exception {
        return null;
    }

    public void deleteConnector(String connectorId) throws Exception {
        connectorsRepo.deleteById(connectorId);
    }

}
