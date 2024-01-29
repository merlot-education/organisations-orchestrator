package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryUriItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.DocumentField;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.*;

@Service
public class ParticipantService {

    private final Logger logger = LoggerFactory.getLogger(ParticipantService.class);

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private GxfsCatalogService gxfsCatalogService;

    @Value("${merlot-domain}")
    private String merlotDomain;
    @Autowired
    private OrganizationMetadataService organizationMetadataService;

    private static final String PARTICIPANT = "Participant:";

    /**
     * Given a participant ID, return the organization data from the GXFS catalog.
     *
     * @param id participant id
     * @return organization data
     */
    public MerlotParticipantDto getParticipantById(String id) throws JsonProcessingException {
        // input sanetization, for now we defined that ids must either only consist of numbers or be uuids
        String regex = "did:web:[-A-Za-z0-9]*." + merlotDomain;
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a valid did-web.");
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
        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(response.getSelfDescription(),
            metaDto);
    }

    /**
     * Return all participants enrolled in the GXFS catalog (including participants that are also federators).
     *
     * @return page of organizations
     */
    public Page<MerlotParticipantDto> getParticipants(Pageable pageable) throws JsonProcessingException {
        // post a query to get a paginated and sorted list of participants
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = null;
        try {
            uriResponse = gxfsCatalogService.getSortedParticipantUriPage(
                    "MerlotOrganization", "orgaName",
                    pageable.getOffset(), pageable.getPageSize());
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
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
        List<MerlotParticipantDto> selfDescriptions = sdResponse.getItems().stream()
            .map(item -> {
                SelfDescription selfDescription = item.getMeta().getContent();

                String id = item.getMeta().getContent().getVerifiableCredential().getCredentialSubject().getId();
                MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

                if (metaDto == null) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error while retrieving Participant with id: " + id);
                }

                return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription,
                    metaDto);
            }).sorted(
                Comparator.comparing(
                    p -> ((MerlotOrganizationCredentialSubject)
                            p.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                            .getOrgaName().toLowerCase())).toList();

        // wrap result into page
        return new PageImpl<>(selfDescriptions, pageable, uriResponse.getTotalCount());
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
     * Given a new credential subject, attempt to update the self-description in the GXFS catalog.
     *
     * @param participantDtoWithEdits dto with updated fields
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

        ParticipantItem participantItem;
        try {
            participantItem = gxfsCatalogService.updateParticipant(targetCredentialSubject);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No participant with this id was found in the catalog.");
        } catch (CredentialPresentationException | CredentialSignatureException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign participant credential subject.");
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
            orgaId = orgaId.startsWith(PARTICIPANT) ? orgaId : PARTICIPANT + orgaId;
            metadataMap.put(orgaId, metadata);
        });

        List<String> participantIds = metadataMap.keySet().stream().toList();

        List<SelfDescriptionItem> selfDescriptionItems = gxfsCatalogService.getSelfDescriptionsByIds(participantIds.toArray(String[]::new))
            .getItems();

        Map<String, SelfDescription> sdMap = new HashMap<>();

        selfDescriptionItems.forEach(sdItem -> {
            SelfDescription selfDescription = sdItem.getMeta().getContent();
            String orgaId = selfDescription.getVerifiableCredential().getCredentialSubject().getId();
            orgaId = orgaId.startsWith(PARTICIPANT) ? orgaId : PARTICIPANT + orgaId;

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
     * @return post response from catalog
     * @throws JsonProcessingException failed to read catalog response
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto createParticipant(RegistrationFormContent registrationFormContent)
            throws JsonProcessingException {


        MerlotOrganizationCredentialSubject credentialSubject;
        MerlotParticipantMetaDto metaData;
        try {
            validateMandatoryFields(registrationFormContent);
            credentialSubject = organizationMapper.getSelfDescriptionFromRegistrationForm(registrationFormContent);
            metaData = organizationMapper.getOrganizationMetadataFromRegistrationForm(registrationFormContent);
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file.");
        }

        String id = generateMerlotSubdomain(credentialSubject);
        metaData.setOrgaId(id);

        MerlotParticipantMetaDto metaDataDto;
        try {
            metaDataDto = organizationMetadataService.saveMerlotParticipantMeta(metaData);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be created.");
        }

        credentialSubject.setId(id);
        credentialSubject.setContext(getContext());
        credentialSubject.setType("merlot:MerlotOrganization");

        ParticipantItem participantItem = null;
        try {
            participantItem = gxfsCatalogService.addParticipant(credentialSubject);
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

    private String generateMerlotSubdomain(MerlotOrganizationCredentialSubject credentialSubject) {
        String subdomain = credentialSubject.getOrgaName()
                .replace(" ", "-") // replace whitespace with dash
                .replace("ä", "ae") // replace german umlauts
                .replace("ö", "oe")
                .replace("ü","ue")
                .replaceAll("[^-A-Za-z0-9]", "")  // remove any non-alphanumeric character (except dash)
                .toLowerCase(); // convert to lowercase
        return "did:web:" + subdomain + "." + merlotDomain;
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

        boolean anyFieldEmptyOrBlank =
                orgaName.isBlank() || orgaLegalName.isBlank() || registrationNumber.isBlank() || mailAddress.isBlank()
                || tncLink.isBlank() || tncHash.isBlank() || countryCode.isBlank()
                || city.isBlank() || postalCode.isBlank() || street.isBlank();

        if (anyFieldEmptyOrBlank) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid registration form: Empty or blank fields.");
        }
    }
}
