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
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.participants.GaxTrustLegalPersonCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.DocumentField;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private OrganizationMetadataService organizationMetadataService;

    @Autowired
    private OrganizationMetadataMapper organizationMetadataMapper;

    /**
     * Given a participant ID, return the organization data from the GXFS catalog.
     *
     * @param id participant id
     * @return organization data
     */
    public MerlotParticipantDto getParticipantById(String id) throws JsonProcessingException {
        // input sanitization, for now we defined that ids must either only consist of numbers or be uuids
        String regex = "(^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)|(^\\d+$)";
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a number or a uuid.");
        }

        // retrieve participant's meta information from db
        MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(id);

        if (metaDto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be found.");
        }

        // get on the participants endpoint of the gxfs catalog at the specified id to get all enrolled participants
        ParticipantItem response = null;
        try {
            response = gxfsCatalogService.getParticipantById("Participant:" + id);
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

                String merlotId = ((MerlotOrganizationCredentialSubject) selfDescription.getVerifiableCredential()
                    .getCredentialSubject()).getMerlotId();
                MerlotParticipantMetaDto metaDto = organizationMetadataService.getMerlotParticipantMetaDto(merlotId);

                if (metaDto == null) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error while retrieving Participant:" + merlotId);
                }

                return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(item.getMeta().getContent(),
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
     * Given a new credential subject, attempt to update the self description in the gxfs catalog.
     *
     * @param id id of the participant to update
     * @param participantDtoWithEdits dto with updated fields
     * @return update response from catalog
     * @throws Exception mapping exception
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto updateParticipant(MerlotParticipantDto participantDtoWithEdits,
        OrganizationRoleGrantedAuthority activeRole, String id) throws JsonProcessingException {

        MerlotParticipantDto participantDto = getParticipantById(id);
        MerlotOrganizationCredentialSubject targetCredentialSubject =
            (MerlotOrganizationCredentialSubject) participantDto.getSelfDescription().getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto targetMetadata = participantDto.getMetadata();

        MerlotOrganizationCredentialSubject editedCredentialSubject =
            (MerlotOrganizationCredentialSubject) participantDtoWithEdits.getSelfDescription().getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        if (activeRole.isRepresentative()) {
            organizationMapper.updateSelfDescriptionAsParticipant(editedCredentialSubject, targetCredentialSubject);
            organizationMetadataMapper.updateMerlotParticipantMetaDtoAsParticipant(editedMetadata, targetMetadata);
        } else if (activeRole.isFedAdmin()) {
            organizationMapper.updateSelfDescriptionAsFedAdmin(editedCredentialSubject, targetCredentialSubject);
            organizationMetadataMapper.updateMerlotParticipantMetaDtoAsFedAdmin(editedMetadata, targetMetadata);
        }

        MerlotParticipantMetaDto participantMetadata = null;
        try {
            participantMetadata = organizationMetadataService.updateMerlotParticipantMeta(targetMetadata);

            if (participantMetadata == null) {
                throw new Exception();
            }
        } catch (Exception e) {
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
     * Return all participants/organizations enrolled in the GXFS catalog that are federators.
     *
     * @return list of organizations that are federators
     */
    public List<MerlotParticipantDto> getFederators() throws JsonProcessingException {
        List<MerlotParticipantMetaDto> orgaMetadataList = organizationMetadataService.getParticipantsByMembershipClass(MembershipClass.FEDERATOR);

        List<String> participantIds = orgaMetadataList.stream()
            .map(metadata -> metadata.getOrgaId().startsWith("Participant:") ? metadata.getOrgaId() : "Participant:" + metadata.getOrgaId()).toList();

        List<SelfDescriptionItem> selfDescriptionItems = gxfsCatalogService.getSelfDescriptionsByIds(participantIds.toArray(String[]::new))
            .getItems();

        return selfDescriptionItems.stream().map(item -> {
            SelfDescription selfDescription = item.getMeta().getContent();
            String orgaId = selfDescription.getVerifiableCredential().getCredentialSubject().getId();

            MerlotParticipantMetaDto metadata = orgaMetadataList.stream()
                .filter(orgaMetadata -> orgaMetadata.getOrgaId().equals(orgaId)).findFirst().orElse(null);

            return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription, metadata);
        }).toList();
    }

    /**
     * Given a registration PDF form, attempt to create the self description of the participant in the gxfs catalog.
     *
     * @param pdDoc in-memory representation of the PDF document
     * @return post response from catalog
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto createParticipant(PDDocument pdDoc) {

        PDDocumentCatalog pdCatalog = pdDoc.getDocumentCatalog();
        PDAcroForm pdAcroForm = pdCatalog.getAcroForm();

        String uuid = UUID.randomUUID().toString();
        String id = "Participant:" + uuid;

        MerlotOrganizationCredentialSubject credentialSubject;
        MerlotParticipantMetaDto metaData;
        try {
            validateMandatoryFields(pdAcroForm);
            credentialSubject = organizationMapper.getSelfDescriptionFromRegistrationForm(pdAcroForm);
            metaData= organizationMapper.getOrganizationMetadataFromRegistrationFormAndId(pdAcroForm);
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file.");
        }

        metaData.setOrgaId(uuid);

        MerlotParticipantMetaDto metaDataDto = null;
        try {
            metaDataDto = organizationMetadataService.saveMerlotParticipantMeta(metaData);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be created.");
        }

        credentialSubject.setId(id);
        credentialSubject.setMerlotId(uuid);
        credentialSubject.setContext(getContext());
        credentialSubject.setType("merlot:MerlotOrganization");

        ParticipantItem participantItem;

        try {
            participantItem = gxfsCatalogService.addParticipant(credentialSubject);
        } catch (CredentialPresentationException | CredentialSignatureException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign participant credential subject.");
        }

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(participantItem.getSelfDescription(),
            metaDataDto);
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

    private void validateMandatoryFields(PDAcroForm pdAcroForm) {

        String orgaName = pdAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString();
        String orgaLegalName = pdAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString();
        String registrationNumber = pdAcroForm.getField(DocumentField.REGISTRATIONNUMBER.getValue()).getValueAsString();
        String mailAddress = pdAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString();
        String tncLink = pdAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString();
        String tncHash = pdAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString();
        String countryCode = pdAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString();
        String city = pdAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString();
        String postalCode = pdAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString();
        String street = pdAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString();

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
