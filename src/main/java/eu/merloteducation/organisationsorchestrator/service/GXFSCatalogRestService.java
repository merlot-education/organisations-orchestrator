package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.*;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.Comparator;
import java.util.List;

@Service
public class GXFSCatalogRestService {

    private final Logger logger = LoggerFactory.getLogger(GXFSCatalogRestService.class);

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private GXFSSignerService gxfsSignerService;

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

        // input sanetization, for now we defined that ids must only consist of numbers
        if (!id.matches("\\d+")) {
            throw new IllegalArgumentException("Provided id is not a number.");
        }

        // get on the participants endpoint of the gxfs catalog at the specified id to get all enrolled participants
        String response = keycloakAuthService.webCallAuthenticated(HttpMethod.GET,
                URI.create(gxfscatalogParticipantsUri + "/Participant:" + id).toString(),
                "", null);
        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

        // create a mapper to the ParticipantItem class
        ObjectMapper mapper = new ObjectMapper();
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);
        return organizationMapper.selfDescriptionToMerlotParticipantDto(participantItem.getSelfDescription());
    }

    /**
     * Return all participants enrolled in the GXFS catalog.
     *
     * @return list of organizations
     * @throws Exception mapping exception
     */
    public Page<MerlotParticipantDto> getParticipants(Pageable pageable) throws Exception {
        // post a query to get a paginated and sorted list of participants
        String queryResponse = keycloakAuthService.webCallAuthenticated(HttpMethod.POST,
                gxfscatalogQueryUri,
                """
                        {
                            "statement": "MATCH (p:MerlotOrganization) return p.uri ORDER BY toLower(p.orgaName)"""
                        + " SKIP " + pageable.getOffset() + " LIMIT " + pageable.getPageSize() + """
                        "
                        }
                        """, MediaType.APPLICATION_JSON);

        // create a mapper for the responses
        ObjectMapper mapper = new ObjectMapper();

        // map query response containing the ids to objects and create a string of ids joined by commas
        GXFSCatalogResponse<GXFSQueryUriItem> uriResponse = mapper.readValue(queryResponse, new TypeReference<>() {});
        String urisString = Joiner.on(",")
                .join(uriResponse.getItems().stream().map(GXFSQueryUriItem::getUri).toList());

        // request the ids from the self-description endpoint to get full SDs and map the result to objects
        String sdResponseString = keycloakAuthService.webCallAuthenticated(HttpMethod.GET,
                gxfscatalogSelfdescriptionsUri + "?statuses=ACTIVE&withContent=true&ids=" + urisString,
                "", null);
        GXFSCatalogResponse<SelfDescriptionResponseItem> sdResponse = mapper.readValue(sdResponseString, new TypeReference<>() {});

        // from the SDs create DTO objects. Also sort by name again since the catalog does not respect argument order
        List<MerlotParticipantDto> selfDescriptions = sdResponse.getItems().stream()
                .map(item -> organizationMapper.selfDescriptionToMerlotParticipantDto(item.getMeta().getContent()))
                .sorted(Comparator.comparing(p -> p.getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject().getOrgaName().getValue().toLowerCase()))
                .toList();

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
        throw new ResponseStatusException(e.getStatusCode(), errorMessage.get("message").asText());
    }

    /**
     * Given a new credential subject, attempt to update the self description in the gxfs catalog.
     *
     * @param id id of the participant to update
     * @param editedCredentialSubject subject with updated fields
     * @return update response from catalog
     * @throws Exception mapping exception
     */
    public MerlotParticipantDto updateParticipant(MerlotOrganizationCredentialSubject editedCredentialSubject, String id) throws Exception {
        MerlotOrganizationCredentialSubject targetCredentialSubject = getParticipantById(id)
                .getSelfDescription().getVerifiableCredential().getCredentialSubject();
        organizationMapper.updateSelfDescriptionAsParticipant(editedCredentialSubject, targetCredentialSubject);
        // prepare a json to send to the gxfs catalog, sign it and read the response
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String credentialSubjectJson = mapper.writeValueAsString(targetCredentialSubject);

        String signedVp = presentAndSign(credentialSubjectJson, targetCredentialSubject.getId());

        String response = "";
        try {
            response = keycloakAuthService.webCallAuthenticated(
                    HttpMethod.PUT,
                    gxfscatalogParticipantsUri + "/" + targetCredentialSubject.getId(),
                    signedVp,
                    MediaType.APPLICATION_JSON);
        } catch (WebClientResponseException e) {
            handleCatalogError(e);
        }
        mapper = new ObjectMapper();
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);
        return organizationMapper.selfDescriptionToMerlotParticipantDto(participantItem.getSelfDescription());
    }

    /**
     * Return all participants/organizations enrolled in the GXFS catalog that are federators.
     *
     * @return list of organizations
     * @throws Exception mapping exception
     */
    public Page<MerlotParticipantDto> getFederators(Pageable pageable) throws Exception {
        return getParticipants(pageable);
    }
}
