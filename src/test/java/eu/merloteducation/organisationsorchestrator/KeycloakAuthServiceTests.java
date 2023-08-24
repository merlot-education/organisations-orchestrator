package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTests {


    @Mock
    private WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec loginRequestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec loginRequestHeadersSpec;

    @Mock
    WebClient.RequestBodySpec loginRequestBodySpec;

    @Mock
    WebClient.ResponseSpec loginResponseSpec;

    @Mock
    WebClient.RequestBodyUriSpec logoutRequestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec logoutRequestHeadersSpec;

    @Mock
    WebClient.RequestBodySpec logoutRequestBodySpec;

    @Mock
    WebClient.ResponseSpec logoutResponseSpec;

    @Mock
    WebClient.RequestBodyUriSpec webRequestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec webRequestHeadersSpec;

    @Mock
    WebClient.RequestBodySpec webRequestBodySpec;

    @Mock
    WebClient.ResponseSpec webResponseSpec;

    private KeycloakAuthService keycloakAuthService;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;
    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;


    @BeforeEach
    public void setUp() throws JsonProcessingException {
        keycloakTokenUri = "http://example.com/token";
        keycloakLogoutUri = "http://example.com/logout";
        String loginResponse = """
                {
                    "access_token": "1234",
                    "refresh_token": "5678"
                }
                """;
        ObjectMapper mapper = new ObjectMapper();

        lenient().when(webClient.post()).thenReturn(loginRequestBodyUriSpec);
        lenient().when(loginRequestBodyUriSpec.uri(eq(keycloakTokenUri))).thenReturn(loginRequestBodySpec);
        lenient().when(loginRequestBodySpec.body(any())).thenReturn(loginRequestHeadersSpec);
        lenient().when(loginRequestHeadersSpec.retrieve()).thenReturn(loginResponseSpec);
        lenient().when(loginResponseSpec.bodyToMono(eq(JsonNode.class)))
                .thenReturn(Mono.just(mapper.readTree(loginResponse)));

        lenient().when(webClient.method(any())).thenReturn(webRequestBodyUriSpec);
        lenient().when(webRequestBodyUriSpec.uri(any(String.class))).thenReturn(webRequestBodySpec);
        lenient().when(webRequestBodySpec.contentType(any())).thenReturn(webRequestBodySpec);
        lenient().when(webRequestBodySpec.bodyValue(any())).thenReturn(webRequestHeadersSpec);
        lenient().when(webRequestHeadersSpec.headers(any())).thenReturn(webRequestHeadersSpec);
        lenient().when(webRequestHeadersSpec.retrieve()).thenReturn(webResponseSpec);
        lenient().when(webResponseSpec.bodyToMono(eq(String.class)))
                .thenReturn(Mono.just("{}"));

        lenient().when(loginRequestBodyUriSpec.uri(eq(keycloakLogoutUri))).thenReturn(logoutRequestBodySpec);
        lenient().when(logoutRequestBodySpec.body(any())).thenReturn(logoutRequestHeadersSpec);
        lenient().when(logoutRequestHeadersSpec.retrieve()).thenReturn(logoutResponseSpec);
        lenient().when(logoutResponseSpec.toBodilessEntity())
                .thenReturn(Mono.empty());

        keycloakAuthService = new KeycloakAuthService(webClient, keycloakTokenUri, keycloakLogoutUri, "", "", "", "", "");
    }

    @Test
    void testLogin(){
        String result = keycloakAuthService.webCallAuthenticated(HttpMethod.GET, "http://example.com", "", null);
        assertEquals("{}", result);
    }

    @Test
    void testRefresh(){
        keycloakAuthService.refreshLogin();
        String result = keycloakAuthService.webCallAuthenticated(HttpMethod.GET, "http://example.com", "", null);
        assertEquals("{}", result);
    }
}
