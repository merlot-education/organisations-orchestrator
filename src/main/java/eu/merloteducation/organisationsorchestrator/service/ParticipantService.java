/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
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

package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.client.SelfDescriptionStatus;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryUriItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.*;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.modelslib.api.organization.*;
import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateDto;
import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateRequest;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.mappers.ParticipantCredentialMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.exceptions.ParticipantConflictException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
@Slf4j
public class ParticipantService {

    private final OrganizationMapper organizationMapper;
    private final ParticipantCredentialMapper participantCredentialMapper;
    private final GxfsCatalogService gxfsCatalogService;
    private final OrganizationMetadataService organizationMetadataService;
    private final OutgoingMessageService outgoingMessageService;
    private final OmejdnConnectorApiClient omejdnConnectorApiClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<OrganizationConnectorDto>> initialOrgaConnectors;
    private final String ocmAgentDid;

    public ParticipantService(@Autowired OrganizationMapper organizationMapper,
                              @Autowired ParticipantCredentialMapper participantCredentialMapper,
                              @Autowired GxfsCatalogService gxfsCatalogService,
                              @Autowired OrganizationMetadataService organizationMetadataService,
                              @Autowired OutgoingMessageService outgoingMessageService,
                              @Autowired OmejdnConnectorApiClient omejdnConnectorApiClient,
                              @Autowired ObjectMapper objectMapper,
                              @Value("${init-data.connectors:#{null}}") File initialOrgaConnectorsResource,
                              @Value("${init-data.ocm-agent-did:#{null}}") String ocmAgentDid) {
        this.organizationMapper = organizationMapper;
        this.participantCredentialMapper = participantCredentialMapper;
        this.gxfsCatalogService = gxfsCatalogService;
        this.organizationMetadataService = organizationMetadataService;
        this.outgoingMessageService = outgoingMessageService;
        this.omejdnConnectorApiClient = omejdnConnectorApiClient;
        this.objectMapper = objectMapper;
        Map<String, Set<OrganizationConnectorDto>> foundInitialOrgaConnectors = Collections.emptyMap();
        if (initialOrgaConnectorsResource != null) {
            try {
                foundInitialOrgaConnectors =
                        this.objectMapper.readValue(initialOrgaConnectorsResource, new TypeReference<>(){});
            } catch (IOException ignored) {
                // ignore, keep empty map
            }
        }
        this.initialOrgaConnectors = foundInitialOrgaConnectors;
        this.ocmAgentDid = ocmAgentDid;
    }

    /**
     * Given a participant ID, return the organization data from the GXFS catalog.
     *
     * @param id participant id
     * @return organization data
     */
    public MerlotParticipantDto getParticipantById(String id) throws JsonProcessingException {
        MerlotParticipantMetaDto metaDto = getParticipantMetaById(id);
        ExtendedVerifiablePresentation selfDescription = getParticipantSdById(id);

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription,
            metaDto);
    }

    private MerlotParticipantMetaDto getParticipantMetaById(String id) {
        String regex = "did:web:[-.A-Za-z0-9:%#]*";
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a valid did:web.");
        }

        // retrieve participant's meta information from db
        MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

        if (metaDto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be found.");
        }
        return metaDto;
    }

    private ExtendedVerifiablePresentation getParticipantSdById(String id) throws JsonProcessingException {
        String regex = "did:web:[-.A-Za-z0-9:%#]*";
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a valid did:web.");
        }

        // get on the participants endpoint of the gxfs catalog at the specified id to get all enrolled participants
        ParticipantItem response = null;
        try {
            response = gxfsCatalogService.getParticipantById(id);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No participant with this id was found.");
        }

        return response.getSelfDescription();
    }

    /**
     * Return all participants enrolled in the GXFS catalog (including participants that are also federators).
     *
     * @return page of organizations
     */
    public Page<MerlotParticipantDto> getParticipants(Pageable pageable, OrganizationRoleGrantedAuthority activeRole) throws JsonProcessingException {
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = null;

        if (activeRole != null && activeRole.isFedAdmin()) {
            uriResponse = getAllParticipantsUris(pageable);
        } else {
            uriResponse = getActiveParticipantsUris(pageable);
        }

        if (uriResponse == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch uris from catalog.");
        }

        String[] participantUris = uriResponse.getItems().stream().map(GXFSQueryUriItem::getUri).toArray(String[]::new);

        if (participantUris.length == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // request the ids from the self-description endpoint to get full SDs and map the result to objects
        GXFSCatalogListResponse<SelfDescriptionItem> sdResponse = null;
        try {
            sdResponse = gxfsCatalogService.getSelfDescriptionsByIds(participantUris);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        // from the SDs create DTO objects. Also sort by name again since the catalog does not respect argument order
        List<MerlotParticipantDto> selfDescriptions = null;
        try {
            selfDescriptions = sdResponse.getItems().stream()
                .map(item -> {
                    ExtendedVerifiablePresentation selfDescription = item.getMeta().getContent();
                    String id = selfDescription
                            .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId();
                    MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

                    if (metaDto == null) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error while retrieving Participant with id: " + id);
                    }

                    return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription, metaDto);
                })
                    .sorted(Comparator.comparing(
                    p ->  {
                        GxLegalParticipantCredentialSubject legalParticipant =
                                p.getSelfDescription()
                                        .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
                        if (legalParticipant != null) {
                            return legalParticipant.getName().toLowerCase();
                        }
                        return "";
                    }))
                    .toList();
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        // wrap result into page
        return new PageImpl<>(selfDescriptions, pageable, uriResponse.getTotalCount());
    }

    private GXFSCatalogListResponse<GXFSQueryUriItem> getActiveParticipantsUris(Pageable pageable) throws JsonProcessingException {
        List<String> inactiveOrgasIds = organizationMetadataService.getInactiveParticipantsIds();

        // post a query to get a paginated and sorted list of active participants
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = null;
        try {
            uriResponse = gxfsCatalogService.getSortedParticipantUriPageWithExcludedUris(GxLegalParticipantCredentialSubject.TYPE_CLASS, "name", inactiveOrgasIds,
                pageable.getOffset(), pageable.getPageSize());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        return uriResponse;
    }

    private GXFSCatalogListResponse<GXFSQueryUriItem> getAllParticipantsUris(Pageable pageable) throws JsonProcessingException {
        // post a query to get a paginated and sorted list of participants
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = null;
        try {
            uriResponse = gxfsCatalogService.getSortedParticipantUriPage(GxLegalParticipantCredentialSubject.TYPE_CLASS, "name",
                    pageable.getOffset(), pageable.getPageSize());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
       return uriResponse;
    }

    /**
     * Return the set of allowed OCM agent DIDs that can be used to log in for this participant.
     *
     * @param id id of the participant
     * @return dto with set of whitelisted agent DIDs
     */
    public ParticipantAgentDidsDto getAgentDidsByParticipantId(String id) {
        MerlotParticipantMetaDto metaDto = getParticipantMetaById(id);
        return organizationMapper.agentSettingsSetToDidDto(metaDto.getOcmAgentSettings());
    }

    private void handleCatalogError(WebClientResponseException e)
        throws ResponseStatusException, JsonProcessingException {

        log.warn("Error in communication with catalog: {}", e.getResponseBodyAsString());
        JsonNode errorMessage = objectMapper.readTree(e.getResponseBodyAsString());
        throw new ResponseStatusException(e.getStatusCode(),
                errorMessage.get("message") == null ? "Unknown Error" : errorMessage.get("message").asText());
    }

    /**
     * Given a new credential subject and the edited metadata for a participant, attempt to update the self-description
     * in the GXFS catalog and update the metadata in the database.
     *
     * @param participantDtoWithEdits dto with updated fields
     * @param activeRole currently acting role
     * @return update response from catalog
     * @throws JsonProcessingException mapping exception
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto updateParticipant(MerlotParticipantDto participantDtoWithEdits,
        OrganizationRoleGrantedAuthority activeRole) throws JsonProcessingException {

        MerlotParticipantDto participantDto = getParticipantById(participantDtoWithEdits.getId());
        ExtendedVerifiablePresentation targetVp = participantDto.getSelfDescription();

        GxLegalParticipantCredentialSubject targetLegalParticipantCs =
                targetVp.findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject targetRegistrationNumberCs =
                targetVp.findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject targetMerlotLegalParticipantCs =
                targetVp.findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);

        MerlotParticipantMetaDto targetMetadata = participantDto.getMetadata();

        boolean initialOrgaActiveValue = targetMetadata.isActive();

        ExtendedVerifiablePresentation editedVp = participantDtoWithEdits.getSelfDescription();
        GxLegalParticipantCredentialSubject editedLegalParticipantCs =
                editedVp.findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject editedRegistrationNumberCs =
                editedVp.findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject editedMerlotLegalParticipantCs =
                editedVp.findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);

        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        if (activeRole.isRepresentative()) {
            participantCredentialMapper.updateCredentialSubjectAsParticipant(editedLegalParticipantCs,
                    targetLegalParticipantCs);
            participantCredentialMapper.updateCredentialSubjectAsParticipant(editedMerlotLegalParticipantCs,
                    targetMerlotLegalParticipantCs);
            organizationMapper.updateMerlotParticipantMetaDtoAsParticipant(editedMetadata, targetMetadata);
        } else if (activeRole.isFedAdmin()) {
            participantCredentialMapper.updateCredentialSubjectAsFedAdmin(editedLegalParticipantCs,
                    targetLegalParticipantCs);
            participantCredentialMapper.updateCredentialSubjectAsFedAdmin(editedRegistrationNumberCs,
                    targetRegistrationNumberCs);
            participantCredentialMapper.updateCredentialSubjectAsFedAdmin(editedMerlotLegalParticipantCs,
                    targetMerlotLegalParticipantCs);
            organizationMapper.updateMerlotParticipantMetaDtoAsFedAdmin(editedMetadata, targetMetadata);
        }

        MerlotParticipantMetaDto participantMetadata;
        try {
            participantMetadata = organizationMetadataService.updateMerlotParticipantMeta(targetMetadata);

            if (participantMetadata == null) {
                throw new NullPointerException();
            }
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be updated.");
        }

        // fetch the corresponding signer config for the performing role
        OrganisationSignerConfigDto activeRoleSignerConfig =
                (participantMetadata.getOrgaId().equals(activeRole.getOrganizationId()))
                        ? participantMetadata.getOrganisationSignerConfigDto()
                        : organizationMetadataService.getMerlotParticipantMetaDto(activeRole.getOrganizationId())
                        .getOrganisationSignerConfigDto();

        if (!isSignerConfigValid(activeRoleSignerConfig)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                "Participant cannot be updated: No private key and/or verification method found for the executing participant.");
        }

        ParticipantItem participantItem;
        try {
            // sign SD using verification method referencing the merlot certificate and the default/merlot private key
            participantItem = gxfsCatalogService.updateParticipant(List.of(targetLegalParticipantCs,
                            targetRegistrationNumberCs, targetMerlotLegalParticipantCs),
                activeRoleSignerConfig.getMerlotVerificationMethod());

            // clean up old SDs, remove these lines if you need the history of participant SDs
            GXFSCatalogListResponse<SelfDescriptionItem> deprecatedParticipantSds =
                    gxfsCatalogService.getSelfDescriptionsByIds(new String[]{participantItem.getId()},
                    new SelfDescriptionStatus[]{SelfDescriptionStatus.DEPRECATED});
            for (SelfDescriptionItem item : deprecatedParticipantSds.getItems()) {
                gxfsCatalogService.deleteSelfDescriptionByHash(item.getMeta().getSdHash());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No participant with this id was found in the catalog.");
        } catch (CredentialPresentationException | CredentialSignatureException e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, e.getMessage());
        }

        if(!participantMetadata.isActive() && participantMetadata.isActive() != initialOrgaActiveValue) {
            outgoingMessageService.sendOrganizationMembershipRevokedMessage(participantMetadata.getOrgaId());
        }

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(participantItem.getSelfDescription(),
            participantMetadata);
    }

    /**
     * Return all participants enrolled in the GXFS catalog that are federators.
     *
     * @return list of organizations that are federators
     */
    public List<MerlotParticipantDto> getFederators() {
        List<MerlotParticipantMetaDto> metadataList = organizationMetadataService.getParticipantsByMembershipClass(MembershipClass.FEDERATOR);

        Map<String, MerlotParticipantMetaDto> metadataMap = new HashMap<>();

        metadataList.forEach(metadata -> {
            String orgaId = metadata.getOrgaId();
            metadataMap.put(orgaId, metadata);
        });

        List<String> participantIds = metadataMap.keySet().stream().toList();

        List<SelfDescriptionItem> selfDescriptionItems = gxfsCatalogService.getSelfDescriptionsByIds(participantIds.toArray(String[]::new))
            .getItems();

        Map<String, ExtendedVerifiablePresentation> sdMap = new HashMap<>();

        selfDescriptionItems.forEach(sdItem -> {
            ExtendedVerifiablePresentation selfDescription = sdItem.getMeta().getContent();
            String orgaId = selfDescription
                    .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId();

            sdMap.put(orgaId, selfDescription);
        });

        return participantIds.stream().map(participantId -> {
            ExtendedVerifiablePresentation sd = sdMap.get(participantId);
            MerlotParticipantMetaDto metadata = metadataMap.get(participantId);

            return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(sd, metadata);
        }).toList();
    }

    /**
     * Given the content of a registration form, attempt to create the self-description of the participant in the GXFS catalog.
     *
     * @param registrationFormContent content of the registration form
     * @param activeRole currently acting role
     * @return post response from catalog
     * @throws JsonProcessingException failed to read catalog response
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto createParticipant(RegistrationFormContent registrationFormContent,
                                                  OrganizationRoleGrantedAuthority activeRole)
            throws JsonProcessingException {

        MerlotParticipantMetaDto metaData;
        GxLegalParticipantCredentialSubject participantCs;
        GxLegalRegistrationNumberCredentialSubject registrationNumberCs;
        MerlotLegalParticipantCredentialSubject merlotParticipantCs;
        try {
            validateMandatoryFields(registrationFormContent);
            participantCs = participantCredentialMapper
                    .getLegalParticipantCsFromRegistrationForm(registrationFormContent);
            registrationNumberCs = participantCredentialMapper
                    .getLegalRegistrationNumberFromRegistrationForm(registrationFormContent);
            merlotParticipantCs = participantCredentialMapper
                    .getMerlotParticipantCsFromRegistrationForm(registrationFormContent);
            metaData = organizationMapper.getOrganizationMetadataFromRegistrationForm(registrationFormContent);

            // request did and private key
            ParticipantDidPrivateKeyDto didPrivateKeyDto =
                    outgoingMessageService.requestNewDidPrivateKey(
                            new ParticipantDidPrivateKeyCreateRequest(merlotParticipantCs.getLegalName()));

           // update metadata signer config
            metaData.setOrganisationSignerConfigDto(
                    organizationMapper.getSignerConfigDtoFromDidPrivateKeyDto(didPrivateKeyDto));

            // update orga id with received did
            metaData.setOrgaId(didPrivateKeyDto.getDid());

            // request a new DAPS certificate for organization and store it
            OmejdnConnectorCertificateDto omejdnCertificate
                    = omejdnConnectorApiClient.addConnector(
                    new OmejdnConnectorCertificateRequest(metaData.getOrgaId()));
            metaData.setDapsCertificates(
                    List.of(organizationMapper.omejdnCertificateToDapsCertificateDto(omejdnCertificate)));

            // if initial connectors are defined, add them to the participant
            Set<OrganizationConnectorDto> connectors = initialOrgaConnectors.getOrDefault(merlotParticipantCs.getLegalName(),
                    Collections.emptySet());
            metaData.setConnectors(new HashSet<>());
            metaData.getConnectors().addAll(connectors);

            // also set default OCM DID
            ParticipantAgentSettingsDto agentSettingsDto = new ParticipantAgentSettingsDto();
            agentSettingsDto.setAgentDid(ocmAgentDid);
            metaData.setOcmAgentSettings(Set.of(agentSettingsDto));

        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file.");
        }

        MerlotParticipantMetaDto metaDataDto;
        try {
            metaDataDto = organizationMetadataService.saveMerlotParticipantMeta(metaData);
        } catch (ParticipantConflictException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Participant with this legal name already exists.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be created.");
        }

        // set credential subject id to did from metadata (received from did service)
        String[] orgaIdSplit = metaDataDto.getOrgaId().split(":");
        String orgaUuid = orgaIdSplit[orgaIdSplit.length-1];
        String registrationNumberId = "urn:uuid:" + orgaUuid;
        merlotParticipantCs.setId(metaDataDto.getOrgaId());
        participantCs.setId(metaDataDto.getOrgaId());
        registrationNumberCs.setId(registrationNumberId);
        participantCs.setLegalRegistrationNumber(
                List.of(new NodeKindIRITypeId(registrationNumberId)));

        // fetch the corresponding signer config for the performing role
        OrganisationSignerConfigDto activeRoleSignerConfig =
                (metaDataDto.getOrgaId().equals(activeRole.getOrganizationId()))
                        ? metaDataDto.getOrganisationSignerConfigDto()
                        : organizationMetadataService.getMerlotParticipantMetaDto(activeRole.getOrganizationId())
                        .getOrganisationSignerConfigDto();

        if (!isSignerConfigValid(activeRoleSignerConfig)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                "Participant cannot be created: No private key and/or verification method found for the executing participant.");
        }

        ParticipantItem participantItem = null;
        try {
            // sign SD using verification method referencing the merlot certificate and the default/merlot private key
            participantItem = gxfsCatalogService.addParticipant(
                    List.of(participantCs, registrationNumberCs, merlotParticipantCs),
                    activeRoleSignerConfig.getMerlotVerificationMethod());
        } catch (CredentialPresentationException | CredentialSignatureException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        if (participantItem == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create participant");
        }

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(participantItem.getSelfDescription(),
                metaDataDto);
    }

    /**
     * Return the list of trusted dids. In the context of MERLOT the dids of the federators are trusted.
     *
     * @return list of trusted dids
     */
    public List<String> getTrustedDids() {
        return organizationMetadataService.getParticipantIdsByMembershipClass(MembershipClass.FEDERATOR);
    }

    private void validateMandatoryFields(RegistrationFormContent registrationFormContent) {

        String orgaName = registrationFormContent.getOrganizationName();
        String orgaLegalName = registrationFormContent.getOrganizationLegalName();
        String mailAddress = registrationFormContent.getMailAddress();
        String tncLink = registrationFormContent.getProviderTncLink();
        String tncHash = registrationFormContent.getProviderTncHash();
        String countryCode = registrationFormContent.getCountryCode();
        String city = registrationFormContent.getCity();
        String postalCode = registrationFormContent.getPostalCode();
        String street = registrationFormContent.getStreet();

        boolean anyFieldEmptyOrBlank =
                orgaName.isBlank() || orgaLegalName.isBlank() || mailAddress.isBlank()
                || tncLink.isBlank() || tncHash.isBlank() || countryCode.isBlank()
                || city.isBlank() || postalCode.isBlank() || street.isBlank();

        String leiCode = registrationFormContent.getRegistrationNumberLeiCode();
        String eori = registrationFormContent.getRegistrationNumberEori();
        String vatId = registrationFormContent.getRegistrationNumberVatID();

        boolean registrationNumberMissing =
                (leiCode == null || leiCode.isBlank())
                && (eori == null || eori.isBlank())
                && (vatId == null || vatId.isBlank());

        if (anyFieldEmptyOrBlank || registrationNumberMissing) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid registration form: Empty or blank fields.");
        }
    }

    private boolean isSignerConfigValid(OrganisationSignerConfigDto signerConfig) {
        if (signerConfig == null) {
            return false;
        }

        boolean privateKeyValid = signerConfig.getPrivateKey() != null
            && !signerConfig.getPrivateKey().isBlank();

        boolean verificationMethodValid = signerConfig.getVerificationMethod() != null
            && !signerConfig.getVerificationMethod().isBlank();

        boolean merlotVerificationMethodValid = signerConfig.getMerlotVerificationMethod() != null
            && !signerConfig.getMerlotVerificationMethod().isBlank();

        return privateKeyValid && verificationMethodValid && merlotVerificationMethodValid;
    }
}
