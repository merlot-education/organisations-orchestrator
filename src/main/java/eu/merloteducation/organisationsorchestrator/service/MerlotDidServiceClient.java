package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface MerlotDidServiceClient {
    @PostExchange("/generateDidAndPrivateKey")
    ParticipantDidPrivateKeyDto generateDidAndPrivateKey(@RequestBody ParticipantDidPrivateKeyCreateRequest request);
}
