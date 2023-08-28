package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.organisationsorchestrator.models.OrganiationViews;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantSelfDescription;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    private static final String PARTICIPANT = "Participant:";

    /**
     * GET health endpoint.
     */
    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }

    // TODO refactor to library
    private Set<String> getMerlotRoles() {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

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
     * GET endpoint for retrieving all enrolled organizations.
     *
     * @return list of all enrolled organizations
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("")
    @JsonView(OrganiationViews.PublicView.class)
    public Page<MerlotParticipantDto> getAllOrganizations(@RequestParam(value = "page", defaultValue = "0") int page,
                                                          @RequestParam(value = "size", defaultValue = "9") int size) throws Exception {
        return gxfsCatalogRestService.getParticipants(PageRequest.of(page, size));
    }

    /**
     * PUT endpoint for updating an organization.
     *
     * @throws Exception exception during participant update
     * @return updated organization
     */
    @PutMapping("/organization/{orgaId}")
    @JsonView(OrganiationViews.PublicView.class)
    public MerlotParticipantDto updateOrganization(@Valid @RequestBody MerlotOrganizationCredentialSubject credentialSubject,
                                                   @PathVariable(value = "orgaId") String orgaId) throws Exception {
        if (!getRepresentedOrgaIds().contains(credentialSubject.getId().replace(PARTICIPANT, ""))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return gxfsCatalogRestService.updateParticipant(credentialSubject, orgaId.replace(PARTICIPANT, ""));
    }

    /**
     * GET endpoint for retrieving a specific organization by its id.
     *
     * @param orgaId organization id
     * @return organization data
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("/organization/{orgaId}")
    @JsonView(OrganiationViews.PublicView.class)
    public MerlotParticipantDto getOrganizationById(@PathVariable(value = "orgaId") String orgaId) throws Exception {
        try {
            return gxfsCatalogRestService.getParticipantById(orgaId.replace(PARTICIPANT, ""));
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }

    }


}

