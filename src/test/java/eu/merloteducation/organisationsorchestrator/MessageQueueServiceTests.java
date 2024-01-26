package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import eu.merloteducation.organisationsorchestrator.service.MessageQueueService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantConnectorsService;
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
    ParticipantConnectorsService participantConnectorsService;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "participantService", participantService);
        ReflectionTestUtils.setField(messageQueueService, "organisationConnectorsService", participantConnectorsService);
        when(participantService.getParticipantById(any())).thenThrow(RuntimeException.class);
        doReturn(new MerlotParticipantDto()).when(participantService).getParticipantById("10");
        doReturn(new OrganizationConnectorDto()).when(participantConnectorsService).getConnector("10", "1234");
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
        OrganizationConnectorDto model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNotNull(model);
    }

    @Test
    void requestOrganizationConnectorNonExistent()  {
        ConnectorDetailsRequest connectorDetailsRequest = new ConnectorDetailsRequest("garbage", "10");
        OrganizationConnectorDto model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNull(model);
    }
}
