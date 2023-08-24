package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantItem;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantsResponse;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GXFSCatalogRestService {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private KeycloakAuthService keycloakAuthService;

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

        // TODO check if the gxfs catalog returns multiple entries if their id starts with the same characters
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
        // log in as the gxfscatalog user and add the token to the header

        // get on the participants endpoint of the gxfs catalog to get all enrolled participants
        String response = keycloakAuthService.webCallAuthenticated(HttpMethod.GET,
                gxfscatalogParticipantsUri + "?offset=" + pageable.getOffset() + "&limit=" + pageable.getPageSize(),
                "", null);

        // create a mapper to map the response to the ParticipantsResponse class
        ObjectMapper mapper = new ObjectMapper();
        ParticipantsResponse participantsResponse = mapper.readValue(response, ParticipantsResponse.class);
        List<MerlotParticipantDto> selfDescriptions = participantsResponse.getItems().stream()
                .map(item -> organizationMapper.selfDescriptionToMerlotParticipantDto(item.getSelfDescription())).toList();

        return new PageImpl<>(selfDescriptions, pageable, participantsResponse.getTotalCount());
    }

}
