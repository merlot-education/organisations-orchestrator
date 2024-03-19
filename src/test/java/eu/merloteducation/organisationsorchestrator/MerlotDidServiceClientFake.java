package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.organisationsorchestrator.service.MerlotDidServiceClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MerlotDidServiceClientFake implements MerlotDidServiceClient {

    private final ParticipantDidPrivateKeyDto defaultResponse;

    public MerlotDidServiceClientFake() throws IOException {
        InputStream responseStream = MerlotDidServiceClientFake.class.getClassLoader()
                .getResourceAsStream("example-did-service-response.json");
        defaultResponse = new ObjectMapper().readValue(
                new String(responseStream.readAllBytes(), StandardCharsets.UTF_8),
                new TypeReference<>() {});
    }
    @Override
    public ParticipantDidPrivateKeyDto generateDidAndPrivateKey(ParticipantDidPrivateKeyCreateRequest request) {
        return defaultResponse;
    }
}
