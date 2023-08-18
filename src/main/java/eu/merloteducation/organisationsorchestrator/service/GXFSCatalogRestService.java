package eu.merloteducation.organisationsorchestrator.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.ParticipantItem;
import eu.merloteducation.organisationsorchestrator.models.ParticipantSelfDescription;
import eu.merloteducation.organisationsorchestrator.models.ParticipantsResponse;
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
import java.util.List;
import java.util.Map;

@Service
public class GXFSCatalogRestService {

    @Autowired
    private OrganizationMapper organizationMapper;
    @Autowired
    private RestTemplate restTemplate;

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

    private Map<String, Object> loginGXFScatalog() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", keycloakGXFScatalogUser);
        map.add("password", keycloakGXFScatalogPass);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", grantType);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, new HttpHeaders());
        String response = restTemplate.postForObject(keycloakTokenUri, request, String.class);

        JsonParser parser = JsonParserFactory.getJsonParser();
        Map<String, Object> loginResult = parser.parseMap(response);
        return loginResult;
    }

    private void logoutGXFScatalog(String refreshToken) throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, null);
        restTemplate.postForObject(keycloakLogoutUri, request, String.class);
    }

    public MerlotParticipantDto getParticipantById(String id) throws Exception {

        // input sanetization, for now we defined that ids must only consist of numbers
        if (!id.matches("\\d+")) {
            throw new IllegalArgumentException("Provided id is not a number.");
        }

        // log in as the gxfscatalog user and add the token to the header
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        // get on the participants endpoint of the gxfs catalog at the specified id to get all enrolled participants
        // since ids are required to be an uri in the gxfs catalog, we need to pad it with an encoded http://
        // TODO check if the gxfs catalog returns multiple entries if their id starts with the same characters
        String response = restTemplate.exchange(URI.create(gxfscatalogParticipantsUri + "/Participant:" + id),
                HttpMethod.GET, request, String.class).getBody();
        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

        // create a mapper to the ParticipantItem class
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ParticipantItem participantItem = mapper.readValue(response, ParticipantItem.class);

        // log out with the gxfscatalog user
        this.logoutGXFScatalog((String) gxfscatalogLoginResponse.get("refresh_token"));
        return organizationMapper.selfDescriptionToMerlotParticipantDto(participantItem.getSelfDescription());
    }

    public Page<MerlotParticipantDto> getParticipants(Pageable pageable) throws Exception {
        // log in as the gxfscatalog user and add the token to the header
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        // get on the participants endpoint of the gxfs catalog to get all enrolled participants
        String response = restTemplate.exchange(gxfscatalogParticipantsUri + "?offset=" + pageable.getOffset() + "&limit=" + pageable.getPageSize(),
                HttpMethod.GET, request, String.class).getBody();
        // as the catalog returns nested but escaped jsons, we need to manually unescape to properly use it
        response = StringEscapeUtils.unescapeJson(response)
                .replace("\"{", "{")
                .replace("}\"", "}");

        // create a mapper to map the response to the ParticipantsResponse class
        ObjectMapper mapper = new ObjectMapper();//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ParticipantsResponse participantsResponse = mapper.readValue(response, ParticipantsResponse.class);
        List<MerlotParticipantDto> selfDescriptions = participantsResponse.getItems().stream()
                .map(item -> organizationMapper.selfDescriptionToMerlotParticipantDto(item.getSelfDescription())).toList();

        // log out with the gxfscatalog user
        this.logoutGXFScatalog((String) gxfscatalogLoginResponse.get("refresh_token"));
        return new PageImpl<>(selfDescriptions, pageable, participantsResponse.getTotalCount());
    }

}
