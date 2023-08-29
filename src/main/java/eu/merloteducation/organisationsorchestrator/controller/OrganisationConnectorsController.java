package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/organization/{orgaId}/connectors/")
public class OrganisationConnectorsController {

    @Autowired
    private OrganisationConnectorsService connectorsService;

    // TODO refactor to library
    private Set<String> getMerlotRoles() {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // TODO refactor to library
    private Set<String> getRepresentedOrgaIds() {
        Set<String> roles = getMerlotRoles();
        // extract all orgaIds from the OrgRep and OrgLegRep Roles
        return roles
                .stream()
                .filter(s -> s.startsWith("ROLE_OrgRep_") || s.startsWith("ROLE_OrgLegRep_"))
                .map(s -> s.replace("ROLE_OrgRep_", "").replace("ROLE_OrgLegRep_", ""))
                .collect(Collectors.toSet());
    }

    /**
     * GET endpoint for retrieving all connectors to a given organization id.
     *
     * @param orgaId    organization id
     * @return list of connectors of this organization
     */
    @GetMapping("")
    public List<OrganisationConnectorExtension> getAllConnectors(@PathVariable(value = "orgaId") String orgaId) {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds().contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
                                                       @PathVariable(value = "orgaId") String connectorId) {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds().contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds().contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds().contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds().contains(orgaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        connectorsService.deleteConnector(id);
    }
}
