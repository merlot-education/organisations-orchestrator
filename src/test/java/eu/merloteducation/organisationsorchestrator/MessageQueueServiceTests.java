/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
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

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorTransferDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.service.MessageQueueService;
import eu.merloteducation.organisationsorchestrator.service.OrganizationMetadataService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageQueueServiceTests {
    @Autowired
    MessageQueueService messageQueueService;

    @Mock
    ParticipantService participantService;

    @Mock
    OrganizationMetadataService organizationMetadataService;

    @MockBean
    private InitialDataLoader initialDataLoader;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "participantService", participantService);
        ReflectionTestUtils.setField(messageQueueService, "organizationMetadataService", organizationMetadataService);
        when(participantService.getParticipantById(any())).thenThrow(RuntimeException.class);

        doReturn(new MerlotParticipantDto()).when(participantService).getParticipantById("10");
        doReturn(new OrganizationConnectorTransferDto()).when(organizationMetadataService).getConnectorForParticipant("10", "1234");
    }


    @Test
    void requestOrganizationExistent() throws Exception {
        MerlotParticipantDto model = messageQueueService.organizationRequest("10");
        assertNotNull(model);
    }

    @Test
    void requestOrganizationNonExistent() throws Exception {
        MerlotParticipantDto model = messageQueueService.organizationRequest("garbage");
        assertNull(model);
    }

    @Test
    void requestOrganizationConnectorExistent() {
        ConnectorDetailsRequest connectorDetailsRequest = new ConnectorDetailsRequest("1234", "10");
        OrganizationConnectorTransferDto model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNotNull(model);
    }

    @Test
    void requestOrganizationConnectorNonExistent()  {
        ConnectorDetailsRequest connectorDetailsRequest = new ConnectorDetailsRequest("garbage", "10");
        OrganizationConnectorTransferDto model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNull(model);
    }
}
