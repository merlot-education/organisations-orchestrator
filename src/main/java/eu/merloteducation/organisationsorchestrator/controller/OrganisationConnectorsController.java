package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;

@RestController
@RequestMapping("/organization/{orgaId}/connectors/")
@PreAuthorize("@authorityChecker.representsOrganization(authentication, #orgaId)")
public class OrganisationConnectorsController {

    @Autowired
    private OrganisationConnectorsService connectorsService;

    /**
     * GET endpoint for retrieving all connectors to a given organization id.
     *
     * @param orgaId    organization id
     * @return list of connectors of this organization
     */
    @GetMapping("")
    public List<OrganisationConnectorExtension> getAllConnectors(@PathVariable(value = "orgaId") String orgaId) {
        return connectorsService.getAllConnectors(orgaId);
    }

    /**
     * GET endpoint for given a connector id and organization id, return this specific connector.
     *
     * @param orgaId      organization id
     * @param connectorId connector id
     * @return connector
     */
    @GetMapping("connector/{connectorId}")
    public OrganisationConnectorExtension getConnector(@PathVariable(value = "orgaId") String orgaId,
                                                       @PathVariable(value = "connectorId") String connectorId) {
        return connectorsService.getConnector(orgaId, connectorId);
    }

    /**
     * POST endpoint for creating a new connector for the respective organization.
     *
     * @param orgaId    organization id
     * @param postModel connector model
     * @return newly created connector
     */
    @PostMapping("connector")
    public OrganisationConnectorExtension postConnector(@PathVariable(value = "orgaId") String orgaId,
                                                        @Valid @RequestBody PostOrganisationConnectorModel postModel) {
        return connectorsService.postConnector(orgaId, postModel);
    }

    /**
     * PATCH endpoint for updating an existing connector.
     *
     * @param orgaId      organization id
     * @param connectorId connector id
     * @param patchModel  update for connector
     * @return updated connector
     */
    @PatchMapping("connector/{connectorId}")
    public OrganisationConnectorExtension patchConnector(@PathVariable(value = "orgaId") String orgaId,
                                                         @PathVariable(value = "connectorId") String connectorId,
                                                         @Valid @RequestBody PatchOrganisationConnectorModel patchModel) {
        return connectorsService.patchConnector(orgaId, connectorId, patchModel);
    }

    /**
     * DELETE endpoint for deleting a connector by its database id.
     *
     * @param orgaId    organization id
     * @param id        connector database id
     */
    @DeleteMapping("connector/{id}")
    public void deleteConnector(@PathVariable(value = "orgaId") String orgaId,
                                @PathVariable(value = "id") String id) {
        connectorsService.deleteConnector(id);
    }
}
