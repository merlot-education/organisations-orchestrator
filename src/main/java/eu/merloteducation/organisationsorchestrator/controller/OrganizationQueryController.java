package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.organisationsorchestrator.models.OrganisationViews;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.validation.Valid;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    private static final String PARTICIPANT = "Participant:";

    /**
     * GET endpoint for retrieving all enrolled organizations.
     *
     * @return list of all enrolled organizations
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("")
    @JsonView(OrganisationViews.PublicView.class)
    public Page<MerlotParticipantDto> getAllOrganizations(@RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "9") int size) throws Exception {

        return gxfsCatalogRestService.getParticipants(PageRequest.of(page, size));
    }

    /**
     * POST endpoint for creating an organization.
     *
     * @return created organization
     * @throws Exception exception during participant creation
     */
    @PostMapping("/organization")
    @JsonView(OrganisationViews.PublicView.class)
    public MerlotParticipantDto createOrganization(
        @Valid @RequestPart("file") MultipartFile[] files) throws Exception {

        if (files.length != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files specified");
        }
        try (PDDocument pdDoc = Loader.loadPDF(files[0].getBytes())) {
            return gxfsCatalogRestService.createParticipant(pdDoc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file.");
        }
    }

    /**
     * PUT endpoint for updating an organization.
     *
     * @return updated organization
     * @throws Exception exception during participant update
     */
    @PutMapping("/organization/{orgaId}")
    @JsonView(OrganisationViews.PublicView.class)
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #credentialSubject.id) "
        + "and @authorityChecker.representsOrganization(authentication, #orgaId)")
    public MerlotParticipantDto updateOrganization(
        @Valid @RequestBody MerlotOrganizationCredentialSubject credentialSubject,
        @PathVariable(value = "orgaId") String orgaId) throws Exception {

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
    @JsonView(OrganisationViews.PublicView.class)
    public MerlotParticipantDto getOrganizationById(@PathVariable(value = "orgaId") String orgaId) throws Exception {

        try {
            return gxfsCatalogRestService.getParticipantById(orgaId.replace(PARTICIPANT, ""));
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }

    }

    /**
     * GET endpoint for retrieving all enrolled organizations that are federators.
     *
     * @return list of the enrolled organizations that are federators
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("/federators")
    @JsonView(OrganisationViews.PublicView.class)
    public Page<MerlotParticipantDto> getAllFederators() throws Exception {

        return gxfsCatalogRestService.getFederators(PageRequest.of(0, Integer.MAX_VALUE));
    }
}

