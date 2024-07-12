/*
 *  Copyright 2023-2024 Dataport AöR
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.ParticipantAgentDidsDto;
import eu.merloteducation.modelslib.api.organization.views.OrganisationViews;
import eu.merloteducation.organisationsorchestrator.mappers.PdfContentMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
@Slf4j
public class OrganizationQueryController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private PdfContentMapper pdfContentMapper;

    /**
     * GET endpoint for retrieving all enrolled organizations.
     *
     * @return list of all enrolled organizations
     * @throws JsonProcessingException exception during participant retrieval
     */
    @GetMapping("")
    @JsonView(OrganisationViews.PublicView.class)
    public Page<MerlotParticipantDto> getAllOrganizations(@RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "9") int size,
        @RequestHeader(value = "Active-Role", required = false) OrganizationRoleGrantedAuthority activeRole) throws JsonProcessingException {
        return participantService.getParticipants(PageRequest.of(page, size), activeRole);
    }

    /**
     * POST endpoint for creating an organization.
     *
     * @return created organization
     */
    @PostMapping("/organization")
    @JsonView(OrganisationViews.PublicView.class)
    @PreAuthorize("#activeRole.isFedAdmin()")
    public MerlotParticipantDto createOrganization(@Valid @RequestPart("file") MultipartFile[] files,
        @RequestHeader("Active-Role") OrganizationRoleGrantedAuthority activeRole) {

        if (files.length != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files specified");
        }
        try (PDDocument pdDoc = Loader.loadPDF(files[0].getBytes())) {
            PDDocumentCatalog pdCatalog = pdDoc.getDocumentCatalog();
            PDAcroForm pdAcroForm = pdCatalog.getAcroForm();
            RegistrationFormContent content =
                    pdfContentMapper.getRegistrationFormContentFromRegistrationForm(pdAcroForm);
            return participantService.createParticipant(content, activeRole);
        } catch (Exception e) {
            log.error("Message: {}", e.getMessage());
            log.error("Stack Trace: {}",Arrays.toString(e.getStackTrace()));
            log.error("Class: {}", e.getClass());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file. " + e.getMessage());
        }
    }

    /**
     * PUT endpoint for updating an organization.
     *
     * @return updated organization
     * @throws Exception exception during participant update
     */
    @PutMapping("/organization")
    @JsonView(OrganisationViews.PublicView.class)
    @PreAuthorize("@participantAuthorityChecker.representsParticipantFromDto(#activeRole, #participantDtoWithEdits)" +
            " or @participantAuthorityChecker.isFedAdminOfDifferentParticipant(#activeRole, #participantDtoWithEdits)")
    public MerlotParticipantDto updateOrganization(
        @Valid @RequestBody MerlotParticipantDto participantDtoWithEdits,
        @RequestHeader("Active-Role") OrganizationRoleGrantedAuthority activeRole)
        throws Exception {
        return participantService.updateParticipant(participantDtoWithEdits, activeRole);
    }

    /**
     * GET endpoint for retrieving a specific organization by its id.
     *
     * @param orgaId organization id
     * @return organization data
     */
    @GetMapping("/organization/{orgaId}")
    @JsonView(OrganisationViews.PublicView.class)
    public MerlotParticipantDto getOrganizationById(@PathVariable(value = "orgaId") String orgaId){
        try {
            return participantService.getParticipantById(orgaId);
        } catch (HttpClientErrorException.NotFound | JsonProcessingException e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }
    }

    /**
     * GET endpoint for retrieving all whitelisted OCM agent DIDs that allow login for this participant.
     *
     * @return dto with list of whitelisted agents
     */
    @GetMapping("/organization/agentDids/{orgaId}")
    @JsonView(OrganisationViews.PublicView.class)
    public ParticipantAgentDidsDto getAgentDidsByParticipantId(@PathVariable(value = "orgaId") String orgaId) {
        return participantService.getAgentDidsByParticipantId(orgaId);
    }

    /**
     * GET endpoint for retrieving all enrolled organizations that are federators.
     *
     * @return list of the enrolled organizations that are federators
     */
    @GetMapping("/federators")
    @JsonView(OrganisationViews.PublicView.class)
    public List<MerlotParticipantDto> getAllFederators() {
        return participantService.getFederators();
    }

    /**
     * GET endpoint for retrieving trusted dids.
     *
     * @return list of trusted dids
     */
    @GetMapping("/trustedDids")
    public List<String> getTrustedDids() {
        return participantService.getTrustedDids();
    }
}

