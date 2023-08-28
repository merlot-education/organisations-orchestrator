package eu.merloteducation.organisationsorchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GXFSWizardRestService {

    @Autowired
    private WebClient webClient;

    @Value("${gxfswizard.base-uri}")
    private String gxfswizardBaseUri;

    /**
     * Pass through the Merlot Participant shape from the GXFS Wizard API.
     *
     * @return shape file
     */
    public String getMerlotParticipantShape() {
        try {
            return webClient
                    .get()
                    .uri(gxfswizardBaseUri + "/getJSON?name=Merlot+Organization.json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        }

    }
}
