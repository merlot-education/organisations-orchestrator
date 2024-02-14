package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

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

    @MockBean
    private InitialDataLoader initialDataLoader;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "participantService", participantService);
        when(participantService.getParticipantById(any())).thenThrow(RuntimeException.class);

        MerlotParticipantDto participantDto = new MerlotParticipantDto();
        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        Set<OrganizationConnectorDto> set = new HashSet<>();
        OrganizationConnectorDto connectorDto = new OrganizationConnectorDto();
        connectorDto.setConnectorId("1234");
        set.add(connectorDto);
        metaDto.setConnectors(set);
        participantDto.setMetadata(metaDto);
        doReturn(participantDto).when(participantService).getParticipantById("10");
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
