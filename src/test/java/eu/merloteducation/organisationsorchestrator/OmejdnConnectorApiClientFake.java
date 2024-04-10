package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateDto;
import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateRequest;
import eu.merloteducation.organisationsorchestrator.service.OmejdnConnectorApiClient;
import io.netty.util.internal.StringUtil;

import java.util.UUID;

public class OmejdnConnectorApiClientFake implements OmejdnConnectorApiClient {
    @Override
    public OmejdnConnectorCertificateDto addConnector(OmejdnConnectorCertificateRequest request) {
        OmejdnConnectorCertificateDto dto = new OmejdnConnectorCertificateDto();
        dto.setClientId("12:34:56");
        dto.setClientName((request == null || StringUtil.isNullOrEmpty(request.getClientName()))
                ? UUID.randomUUID().toString()
                : request.getClientName());
        dto.setKeystore("keystore123");
        dto.setPassword("password1234");
        return dto;
    }
}
