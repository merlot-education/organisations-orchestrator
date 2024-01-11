package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.StringTypeValue;
import eu.merloteducation.modelslib.gxfscatalog.participants.ParticipantItem;
import eu.merloteducation.modelslib.gxfscatalog.query.GXFSQueryUriItem;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.mappers.DocumentField;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import jakarta.transaction.Transactional;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.*;

@Service
public class GXFSCatalogRestService {

    private final Logger logger = LoggerFactory.getLogger(GXFSCatalogRestService.class);

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private GXFSSignerService gxfsSignerService;

    @Autowired
    private OrganisationMetadataService organisationMetadataService;

    @Autowired
    private OrganizationMetadataMapper organizationMetadataMapper;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.authorization-grant-type}")
    private String grantType;

    @Value("${keycloak.gxfscatalog-user}")
    private String keycloakGXFScatalogUser;

    @Value("${keycloak.gxfscatalog-pass}")
    private String keycloakGXFScatalogPass;

    @Value("${gxfscatalog.participants-uri}")
    private String gxfscatalogParticipantsUri;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;

    @Value("${gxfscatalog.query-uri}")
    private String gxfscatalogQueryUri;

    /**
     * Given a participant ID, return the organization data from the GXFS catalog.
     *
     * @param id participant id
     * @return organization data
     * @throws Exception mapping exception
     */
    public MerlotParticipantDto getParticipantById(String id) throws Exception {
        // input sanitization, for now we defined that ids must either only consist of numbers or be uuids
        String regex = "(^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)|(^\\d+$)";
        if (!id.matches(regex)) {
            throw new IllegalArgumentException("Provided id is invalid. It has to be a number or a uuid.");
        }

        // get on the participants endpoint of the gxfs catalog at the specified id to get all enrolled participants
        String response = keycloakAuthService.webCallAuthenticated(HttpMethod.GET,
            URI.create(gxfscatalogParticipantsUri + "/Participant:" + id).toString(), "", null);
        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response).replace("\"{", "{").replace("}\"", "}");

        // create a mapper to the ParticipantItem class
        ObjectMapper mapper = new ObjectMapper();
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);

        // retrieve participant's meta information from db
        MerlotParticipantMetaDto metaDto = organisationMetadataService.getMerlotParticipantMetaDto(id);

        if (metaDto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be found.");
        }

        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(participantItem.getSelfDescription(),
            metaDto);
    }

    /**
     * Return all participants/organizations enrolled in the GXFS catalog (including organizations that are
     * federators).
     *
     * @return list of organizations
     * @throws Exception mapping exception
     */
    public Page<MerlotParticipantDto> getParticipants(Pageable pageable) throws Exception {
        // post a query to get a paginated and sorted list of participants
        String queryResponse = keycloakAuthService.webCallAuthenticated(HttpMethod.POST, gxfscatalogQueryUri, """
            {
                "statement": "MATCH (p:MerlotOrganization) return p.uri ORDER BY toLower(p.orgaName)""" + " SKIP "
            + pageable.getOffset() + " LIMIT " + pageable.getPageSize() + """
            "
            }
            """, MediaType.APPLICATION_JSON);

        // create a mapper for the responses
        ObjectMapper mapper = new ObjectMapper();

        // map query response containing the ids to objects and create a string of ids joined by commas
        GXFSCatalogListResponse<GXFSQueryUriItem> uriResponse = mapper.readValue(queryResponse, new TypeReference<>() {
        });
        String urisString = Joiner.on(",").join(uriResponse.getItems().stream().map(GXFSQueryUriItem::getUri).toList());

        // request the ids from the self-description endpoint to get full SDs and map the result to objects
        String sdResponseString = keycloakAuthService.webCallAuthenticated(HttpMethod.GET,
            gxfscatalogSelfdescriptionsUri + "?statuses=ACTIVE&withContent=true&ids=" + urisString, "", null);
        GXFSCatalogListResponse<SelfDescriptionItem<MerlotOrganizationCredentialSubject>> sdResponse = mapper.readValue(
            sdResponseString, new TypeReference<>() {
            });

        // from the SDs create DTO objects. Also sort by name again since the catalog does not respect argument order
        List<MerlotParticipantDto> selfDescriptions = sdResponse.getItems().stream().map(item -> {
            SelfDescription<MerlotOrganizationCredentialSubject> selfDescription = item.getMeta().getContent();

            String merlotId = selfDescription.getVerifiableCredential().getCredentialSubject().getMerlotId().getValue();
            MerlotParticipantMetaDto metaDto = organisationMetadataService.getMerlotParticipantMetaDto(merlotId);

            if (metaDto == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while retrieving Participant:" + merlotId);
            }

            return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(selfDescription, metaDto);
        }).sorted(Comparator.comparing(
            p -> p.getSelfDescription().getVerifiableCredential().getCredentialSubject().getOrgaName().getValue()
                .toLowerCase())).toList();

        // wrap result into page
        return new PageImpl<>(selfDescriptions, pageable, uriResponse.getTotalCount());
    }

    private String presentAndSign(String credentialSubjectJson, String issuer) throws Exception {

        String vp = """
            {
                "@context": ["https://www.w3.org/2018/credentials/v1"],
                "@id": "http://example.edu/verifiablePresentation/self-description1",
                "type": ["VerifiablePresentation"],
                "verifiableCredential": {
                    "@context": ["https://www.w3.org/2018/credentials/v1"],
                    "@id": "https://www.example.org/ServiceOffering.json",
                    "@type": ["VerifiableCredential"],
                    "issuer": \"""" + issuer + """
            ",
            "issuanceDate": \"""" + OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT) + """
            ",
            "credentialSubject":\s""" + credentialSubjectJson + """
                }
            }
            """;

        return gxfsSignerService.signVerifiablePresentation(vp);
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
        OrganizationRoleGrantedAuthority activeRole, String id) throws Exception {

        MerlotOrganizationCredentialSubject targetCredentialSubject = getParticipantById(id).getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        MerlotOrganizationCredentialSubject editedCredentialSubject = participantDtoWithEdits.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();

        MerlotParticipantMetaDto targetMeta = organisationMetadataService.getMerlotParticipantMetaDto(id);

        if (targetMeta == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant does not exist.");
        }

        MerlotParticipantMetaDto editedMeta = participantDtoWithEdits.getMetadata();

        if (activeRole.isRepresentative()) {
            organizationMapper.updateSelfDescriptionAsParticipant(editedCredentialSubject, targetCredentialSubject);
            organizationMetadataMapper.updateMerlotParticipantMetaDtoAsParticipant(editedMeta, targetMeta);

        } else if (activeRole.isFedAdmin()) {
            organizationMapper.updateSelfDescriptionAsFedAdmin(editedCredentialSubject, targetCredentialSubject);
            organizationMetadataMapper.updateMerlotParticipantMetaDtoAsFedAdmin(editedMeta, targetMeta);
        }

        MerlotParticipantMetaDto updatedMetaDto = null;
        try {
            updatedMetaDto = organisationMetadataService.updateMerlotParticipantMeta(targetMeta);

            if (updatedMetaDto == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Participant could not be updated.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be updated.");
        }

        // prepare a json to send to the gxfs catalog, sign it and read the response
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(targetCredentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, targetCredentialSubject.getId());

        String response = "";
        try {
            response = keycloakAuthService.webCallAuthenticated(HttpMethod.PUT,
                gxfscatalogParticipantsUri + "/" + targetCredentialSubject.getId(), signedVp,
                MediaType.APPLICATION_JSON);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        mapper = new ObjectMapper();
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);
        return organizationMapper.selfDescriptionAndMetadataToMerlotParticipantDto(participantItem.getSelfDescription(),
            updatedMetaDto);
    }

    /**
     * Return all participants/organizations enrolled in the GXFS catalog that are federators.
     *
     * @return list of organizations
     * @throws Exception mapping exception
     */
    public List<MerlotParticipantDto> getFederators() throws Exception {

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);

        Page<MerlotParticipantDto> participantPage = getParticipants(pageable);
        List<MerlotParticipantDto> participantList = participantPage.getContent();

        return participantList.stream().filter(participant -> {
            String membershipClassString = participant.getMetadata().getMembershipClass().strip().toUpperCase();
            MembershipClass membershipClassOfParticipant = MembershipClass.valueOf(membershipClassString);
            return MembershipClass.FEDERATOR.equals(membershipClassOfParticipant);
        }).toList();
    }

    /**
     * Given a registration PDF form, attempt to create the self description of the participant in the gxfs catalog.
     *
     * @param pdDoc in-memory representation of the PDF document
     * @return post response from catalog
     * @throws Exception mapping exception
     */
    @Transactional(rollbackOn = { ResponseStatusException.class })
    public MerlotParticipantDto createParticipant(PDDocument pdDoc) throws Exception {

        PDDocumentCatalog pdCatalog = pdDoc.getDocumentCatalog();
        PDAcroForm pdAcroForm = pdCatalog.getAcroForm();

        MerlotOrganizationCredentialSubject credentialSubject;
        String mailAddress;

        try {
            validateMandatoryFields(pdAcroForm);
            credentialSubject = organizationMapper.getSelfDescriptionFromRegistrationForm(pdAcroForm);
            mailAddress = pdAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString();
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration form file.");
        }

        String uuid = UUID.randomUUID().toString();
        String id = "Participant:" + uuid;
        credentialSubject.setId(id);
        credentialSubject.setMerlotId(new StringTypeValue(uuid));
        credentialSubject.setContext(getContext());
        credentialSubject.setType("merlot:MerlotOrganization");

        MembershipClass defaultMembership = MembershipClass.PARTICIPANT;
        OrganizationMetadata metaData = new OrganizationMetadata(uuid, mailAddress, defaultMembership);

        MerlotParticipantMetaDto metaDataDto = null;
        try {
            metaDataDto = organisationMetadataService.saveMerlotParticipantMeta(metaData);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Participant could not be created.");
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(credentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, id);

        String response = "";
        try {
            response = keycloakAuthService.webCallAuthenticated(HttpMethod.POST, gxfscatalogParticipantsUri, signedVp,
                MediaType.APPLICATION_JSON);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        mapper = new ObjectMapper();
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);
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
                || tncLink.isBlank() || tncHash.isBlank() || countryCode.isBlank() || city.isBlank()
                || postalCode.isBlank() || street.isBlank();

        if (anyFieldEmptyOrBlank) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid registration form: Empty or blank fields.");
        }
    }
}
