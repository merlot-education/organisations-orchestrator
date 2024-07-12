/*
 *  Copyright 2023-2024 Dataport AöR
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
