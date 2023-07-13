package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;

@RestController
@CrossOrigin
@RequestMapping("/organization/{orgaId}/Connectors/")
public class OrganisationConnectorsController {

    @Autowired
    private OrganisationConnectorsService connectorsService;

    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }

    // TODO refactor to library
    private Set<String> getMerlotRoles(Principal principal) {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // TODO refactor to library
    private Set<String> getRepresentedOrgaIds(Principal principal) {
        Set<String> roles = getMerlotRoles(principal);
        // extract all orgaIds from the OrgRep and OrgLegRep Roles
        return roles
                .stream()
                .filter(s -> s.startsWith("ROLE_OrgRep_") || s.startsWith("ROLE_OrgLegRep_"))
                .map(s -> s.replace("ROLE_OrgRep_", "").replace("ROLE_OrgLegRep_", ""))
                .collect(Collectors.toSet());
    }

    @GetMapping("")
    public List<OrganisationConnectorExtension> getAllConnectors(Principal principal,
                                                             @PathVariable(value="orgaId") String orgaId,
                                                             HttpServletResponse response) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

         return connectorsService.getAllConnectors(orgaId);
    }

    @GetMapping("Connector/{connectorId}")
    public OrganisationConnectorExtension getConnector(Principal principal,
                                                   @PathVariable(value="orgaId") String orgaId,
                                                   @PathVariable(value="orgaId") String connectorId,
                                                   HttpServletResponse response) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return connectorsService.getConnector(orgaId, connectorId);
    }

    @PostMapping("Connector")
    public OrganisationConnectorExtension postConnector(Principal principal,
                                                    @PathVariable(value="orgaId") String orgaId,
                                                    @Valid @RequestBody PostOrganisationConnectorModel postModel,
                                                    HttpServletResponse response) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return connectorsService.postConnector(orgaId, postModel);
    }

    @PatchMapping("Connector/{connectorId}")
    public OrganisationConnectorExtension updateConnector(Principal principal,
                                                          @PathVariable(value="orgaId") String orgaId,
                                                          @PathVariable(value="connectorId") String connectorId,
                                                          @Valid @RequestBody PatchOrganisationConnectorModel patchModel,
                                                          HttpServletResponse response) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return connectorsService.updateConnector(orgaId, connectorId, patchModel);
    }

    @DeleteMapping("Connector/{connectorId}")
    public void deleteConnector(Principal principal,
                                @PathVariable(value="orgaId") String orgaId,
                                @PathVariable(value="connectorId") String connectorId,
                                HttpServletResponse response) throws Exception {
        // if the requested organization id is not in the roles of this user,
        // the user is not allowed to request this endpoint
        if (!getRepresentedOrgaIds(principal).contains(orgaId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        connectorsService.deleteConnector(connectorId);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}