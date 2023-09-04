package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.service.GXFSWizardRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GXFSWizardRestServiceTests {

    private GXFSWizardRestService gxfsWizardRestService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersSpec webRequestHeadersSpec;
    @Mock
    private WebClient.RequestHeadersUriSpec webRequestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec webResponseSpec;
    @Value("${gxfswizard.base-uri}")
    private String gxfsWizardBaseUri;


    @BeforeEach
    public void setUp() {
        lenient().when(webClient.get()).thenReturn(webRequestHeadersUriSpec);
        lenient().when(webRequestHeadersUriSpec.uri(eq(gxfsWizardBaseUri + "/getJSON?name=Merlot+Organization.json"))).thenReturn(webRequestHeadersSpec);
        lenient().when(webRequestHeadersSpec.retrieve()).thenReturn(webResponseSpec);
        lenient().when(webResponseSpec.bodyToMono(eq(String.class))).thenReturn(Mono.just("shape"));

        gxfsWizardRestService = new GXFSWizardRestService();
        ReflectionTestUtils.setField(gxfsWizardRestService, "webClient", webClient);
    }

    @Test
    void getParticipantShape() {
        String shape = gxfsWizardRestService.getMerlotParticipantShape();
        assertEquals("shape", shape);
    }
}
