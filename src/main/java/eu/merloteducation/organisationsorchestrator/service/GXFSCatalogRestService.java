package eu.merloteducation.organisationsorchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GXFSCatalogRestService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.logout}")
    private String keycloakLogout;

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

    private Map<String, Object> loginGXFScatalog() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username",keycloakGXFScatalogUser);
        map.add("password",keycloakGXFScatalogPass);
        map.add("client_id",clientId);
        map.add("client_secret",clientSecret);
        map.add("grant_type",grantType);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, new HttpHeaders());
        System.out.println(request);
        String response = restTemplate.postForObject(keycloakTokenUri, request, String.class);

        JsonParser parser = JsonParserFactory.getJsonParser();
        Map<String, Object> loginResult = parser.parseMap(response);
        return loginResult;
    }

    public String getParticipants() {
        Map<String, Object> gxfscatalogLoginResponse = loginGXFScatalog();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + gxfscatalogLoginResponse.get("access_token"));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

        String response = restTemplate.exchange("http://localhost:8081/participants", HttpMethod.GET, request, String.class).getBody();
        return response;
    }

}
