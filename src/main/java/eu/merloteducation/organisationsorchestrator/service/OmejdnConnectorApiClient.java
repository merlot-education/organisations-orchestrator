package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateDto;
import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface OmejdnConnectorApiClient {
    @PostExchange("/add")
    OmejdnConnectorCertificateDto addConnector(@RequestBody OmejdnConnectorCertificateRequest request);
}
