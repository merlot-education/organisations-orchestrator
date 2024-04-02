package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.client.SelfDescriptionStatus;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryUriItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganisationSignerConfigDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.models.exceptions.ParticipantConflictException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class ParticipantService {

    private final Logger logger = LoggerFactory.getLogger(ParticipantService.class);

    private final OrganizationMapper organizationMapper;
    private final GxfsCatalogService gxfsCatalogService;
    private final OrganizationMetadataService organizationMetadataService;
    private final OutgoingMessageService outgoingMessageService;

    private final String PARTICIPANT_TYPE = "MerlotOrganization";

    public ParticipantService(@Autowired OrganizationMapper organizationMapper,
                              @Autowired GxfsCatalogService gxfsCatalogService,
                              @Autowired OrganizationMetadataService organizationMetadataService,
                              @Autowired OutgoingMessageService outgoingMessageService) {
        this.organizationMapper = organizationMapper;
        this.gxfsCatalogService = gxfsCatalogService;
        this.organizationMetadataService = organizationMetadataService;
        this.outgoingMessageService = outgoingMessageService;
    }

    /**
     * Given a participant ID, return the organization data from the GXFS catalog.
     *
     * @param id participant id
     * @return organization data
     */
    public MerlotParticipantDto getParticipantById(String id) throws JsonProcessingException {
        // input sanitization, must be a did:web
        String regex = "did:web:[-.A-Za-z0-9:%#]*";
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a valid did:web.");
        }

        // retrieve participant's meta information from db
        MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

        if (metaDto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be found.");
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

        SelfDescription selfDescription = response.getSelfDescription();

        String signerLegalName = null;
        try {
            signerLegalName = getSignerLegalNameFromCatalog(selfDescription);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }

        metaDto.setSignedBy(signerLegalName);

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription,
            metaDto);
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
                    SelfDescription selfDescription = item.getMeta().getContent();

                    String id = selfDescription.getVerifiableCredential().getCredentialSubject().getId();
                    MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

                    if (metaDto == null) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error while retrieving Participant with id: " + id);
                    }

                    String signerLegalName = getSignerLegalNameFromCatalog(selfDescription);

                    metaDto.setSignedBy(signerLegalName);

                    return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription, metaDto);
                }).sorted(Comparator.comparing(
                    p -> ((MerlotOrganizationCredentialSubject) p.getSelfDescription().getVerifiableCredential()
                        .getCredentialSubject()).getOrgaName().toLowerCase())).toList();
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        // wrap result into page
        return new PageImpl<>(selfDescriptions, pageable, uriResponse.getTotalCount());
    }

    private String getSignerLegalNameFromCatalog(SelfDescription selfDescription) {

        String proofVerificationMethod = selfDescription.getProof().getVerificationMethod();

        String signerId = proofVerificationMethod.replaceFirst("#.*", "");

        GXFSCatalogListResponse<GXFSQueryLegalNameItem>
            response = gxfsCatalogService.getParticipantLegalNameByUri(PARTICIPANT_TYPE, signerId);

        
        // if we do not get exactly one item, we did not find the signer participant and the corresponding legal name
        if (response.getTotalCount() != 1) {
            return null;
        } else {
            return response.getItems().get(0).getLegalName();
        }
    }

    private GXFSCatalogListResponse<GXFSQueryUriItem> getActiveParticipantsUris(Pageable pageable) throws JsonProcessingException {
        List<String> inactiveOrgasIds = organizationMetadataService.getInactiveParticipantsIds();

        // post a query to get a paginated and sorted list of active participants
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = null;
        try {
            uriResponse = gxfsCatalogService.getSortedParticipantUriPageWithExcludedUris(
                PARTICIPANT_TYPE, "orgaName", inactiveOrgasIds,
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
            uriResponse = gxfsCatalogService.getSortedParticipantUriPage(
                PARTICIPANT_TYPE, "orgaName",
                    pageable.getOffset(), pageable.getPageSize());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
       return uriResponse;
    }

    private void handleCatalogError(WebClientResponseException e)
        throws ResponseStatusException, JsonProcessingException {

        logger.warn("Error in communication with catalog: {}", e.getResponseBodyAsString());
        ObjectMapper objectMapper = new ObjectMapper();
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
        MerlotOrganizationCredentialSubject targetCredentialSubject =
            (MerlotOrganizationCredentialSubject) participantDto.getSelfDescription()
                    .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto targetMetadata = participantDto.getMetadata();

        boolean initialOrgaActiveValue = targetMetadata.isActive();

        MerlotOrganizationCredentialSubject editedCredentialSubject =
            (MerlotOrganizationCredentialSubject) participantDtoWithEdits.getSelfDescription()
                    .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        if (activeRole.isRepresentative()) {
            organizationMapper.updateSelfDescriptionAsParticipant(editedCredentialSubject, targetCredentialSubject);
            organizationMapper.updateMerlotParticipantMetaDtoAsParticipant(editedMetadata, targetMetadata);
        } else if (activeRole.isFedAdmin()) {
            organizationMapper.updateSelfDescriptionAsFedAdmin(editedCredentialSubject, targetCredentialSubject);
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

        // fetch the corresponding  signer config for the performing role
        OrganisationSignerConfigDto activeRoleSignerConfig =
                (participantMetadata.getOrgaId().equals(activeRole.getOrganizationId()))
                        ? participantMetadata.getOrganisationSignerConfigDto()
                        : organizationMetadataService.getMerlotParticipantMetaDto(activeRole.getOrganizationId())
                        .getOrganisationSignerConfigDto();

        ParticipantItem participantItem;
        try {
            participantItem = gxfsCatalogService.updateParticipant(targetCredentialSubject,
                    activeRoleSignerConfig.getVerificationMethod(),
                    activeRoleSignerConfig.getPrivateKey());
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign participant credential subject.");
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

        Map<String, SelfDescription> sdMap = new HashMap<>();

        selfDescriptionItems.forEach(sdItem -> {
            SelfDescription selfDescription = sdItem.getMeta().getContent();
            String orgaId = selfDescription.getVerifiableCredential().getCredentialSubject().getId();

            sdMap.put(orgaId, selfDescription);
        });

        return participantIds.stream().map(participantId -> {
            SelfDescription sd = sdMap.get(participantId);
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

        MerlotOrganizationCredentialSubject credentialSubject;
        MerlotParticipantMetaDto metaData;
        try {
            validateMandatoryFields(registrationFormContent);
            credentialSubject = organizationMapper.getSelfDescriptionFromRegistrationForm(registrationFormContent);
            metaData = organizationMapper.getOrganizationMetadataFromRegistrationForm(registrationFormContent);

            // if the user did not specify a did, we can generate the private key and did for them
            if (metaData.getOrgaId().isBlank()) {

                // request did and private key
                ParticipantDidPrivateKeyDto didPrivateKeyDto =
                        outgoingMessageService.requestNewDidPrivateKey(
                                new ParticipantDidPrivateKeyCreateRequest(credentialSubject.getLegalName()));

               // update metadata signer config
                metaData.setOrganisationSignerConfigDto(
                        organizationMapper.getSignerConfigDtoFromDidPrivateKeyDto(didPrivateKeyDto));

                // update orga id with received did
                metaData.setOrgaId(didPrivateKeyDto.getDid());
            }
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

        // set credential subject id to did from metadata (self-assigned or received from did service)
        credentialSubject.setId(metaDataDto.getOrgaId());
        credentialSubject.setContext(getContext());
        credentialSubject.setType("merlot:MerlotOrganization");

        // fetch the corresponding  signer config for the performing role
        OrganisationSignerConfigDto activeRoleSignerConfig =
                (metaDataDto.getOrgaId().equals(activeRole.getOrganizationId()))
                        ? metaDataDto.getOrganisationSignerConfigDto()
                        : organizationMetadataService.getMerlotParticipantMetaDto(activeRole.getOrganizationId())
                        .getOrganisationSignerConfigDto();

        ParticipantItem participantItem = null;
        try {
            participantItem = gxfsCatalogService.addParticipant(credentialSubject,
                    activeRoleSignerConfig.getVerificationMethod(),
                    activeRoleSignerConfig.getPrivateKey());
        } catch (CredentialPresentationException | CredentialSignatureException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign participant credential subject.");
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

    private Map<String, String> getContext() {

        Map<String, String> context = new HashMap<>();
        context.put("gax-trust-framework", "http://w3id.org/gaia-x/gax-trust-framework#");
        context.put("gax-validation", "http://w3id.org/gaia-x/validation#");
        context.put("merlot", "http://w3id.org/gaia-x/merlot#");
        context.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        context.put("sh", "http://www.w3.org/ns/shacl#");
        context.put("skos", "http://www.w3.org/2004/02/skos/core#");
        context.put("vcard", "http://www.w3.org/2006/vcard/ns#");
        context.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        return context;
    }

    private void validateMandatoryFields(RegistrationFormContent registrationFormContent) {

        String orgaName = registrationFormContent.getOrganizationName();
        String orgaLegalName = registrationFormContent.getOrganizationLegalName();
        String registrationNumber = registrationFormContent.getRegistrationNumberLocal();
        String mailAddress = registrationFormContent.getMailAddress();
        String tncLink = registrationFormContent.getProviderTncLink();
        String tncHash = registrationFormContent.getProviderTncHash();
        String countryCode = registrationFormContent.getCountryCode();
        String city = registrationFormContent.getCity();
        String postalCode = registrationFormContent.getPostalCode();
        String street = registrationFormContent.getStreet();
        String didWeb = registrationFormContent.getDidWeb();

        boolean invalidDidWeb = (didWeb != null && !didWeb.isBlank() && !didWeb.startsWith("did:web:"));

        boolean anyFieldEmptyOrBlank =
                orgaName.isBlank() || orgaLegalName.isBlank() || registrationNumber.isBlank() || mailAddress.isBlank()
                || tncLink.isBlank() || tncHash.isBlank() || countryCode.isBlank()
                || city.isBlank() || postalCode.isBlank() || street.isBlank();

        if (anyFieldEmptyOrBlank) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid registration form: Empty or blank fields.");
        }
        if (invalidDidWeb) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid registration form: Invalid did:web specified.");
        }
    }
}
