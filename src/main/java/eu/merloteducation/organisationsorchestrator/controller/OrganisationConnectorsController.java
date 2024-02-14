package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.api.organization.PatchOrganisationConnectorModel;
import eu.merloteducation.modelslib.api.organization.PostOrganisationConnectorModel;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import eu.merloteducation.organisationsorchestrator.service.ParticipantConnectorsService;

//@RestController
//@RequestMapping("/organization/{orgaId}/connectors/")
//@PreAuthorize("@authorityChecker.representsOrganization(authentication, #orgaId)")
public class OrganisationConnectorsController {

//    @Autowired
//    private ParticipantConnectorsService connectorsService;

//    /**
//     * GET endpoint for retrieving all connectors to a given organization id.
//     *
//     * @param orgaId    organization id
//     * @return list of connectors of this organization
//     */
//    @GetMapping("")
//    public List<OrganizationConnectorDto> getAllConnectors(@PathVariable(value = "orgaId") String orgaId) {
//        return connectorsService.getAllConnectors(orgaId);
//    }
//

//    /**
//     * POST endpoint for creating a new connector for the respective organization.
//     *
//     * @param orgaId    organization id
//     * @param postModel connector model
//     * @return newly created connector
//     */
//    @PostMapping("connector")
//    public OrganizationConnectorDto postConnector(@PathVariable(value = "orgaId") String orgaId,
//                                                        @Valid @RequestBody PostOrganisationConnectorModel postModel) {
//        return connectorsService.postConnector(orgaId, postModel);
//    }
//
//    /**
//     * PATCH endpoint for updating an existing connector.
//     *
//     * @param orgaId      organization id
//     * @param connectorId connector id
//     * @param patchModel  update for connector
//     * @return updated connector
//     */
//    @PatchMapping("connector/{connectorId}")
//    public OrganizationConnectorDto patchConnector(@PathVariable(value = "orgaId") String orgaId,
//                                                         @PathVariable(value = "connectorId") String connectorId,
//                                                         @Valid @RequestBody PatchOrganisationConnectorModel patchModel) {
//        return connectorsService.patchConnector(orgaId, connectorId, patchModel);
//    }
//
//    /**
//     * DELETE endpoint for deleting a connector by its database id.
//     *
//     * @param orgaId    organization id
//     * @param id        connector database id
//     */
//    @DeleteMapping("connector/{id}")
//    public void deleteConnector(@PathVariable(value = "orgaId") String orgaId,
//                                @PathVariable(value = "id") String id) {
//        connectorsService.deleteConnector(id);
//    }
}
