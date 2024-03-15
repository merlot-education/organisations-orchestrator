package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.merlotdidservice.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.merlotdidservice.ParticipantDidPrivateKeyDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface MerlotDidServiceClient {
    @PostExchange("/generateDidAndPrivateKey")
    ParticipantDidPrivateKeyDto generateDidAndPrivateKey(@RequestBody ParticipantDidPrivateKeyCreateRequest request);
}
