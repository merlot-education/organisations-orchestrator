package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.models.messagequeue.ConnectorDetailsRequest;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.MessageQueueService;
import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
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
    GXFSCatalogRestService gxfsCatalogRestService;

    @Mock
    OrganisationConnectorsService organisationConnectorsService;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "gxfsCatalogRestService", gxfsCatalogRestService);
        ReflectionTestUtils.setField(messageQueueService, "organisationConnectorsService", organisationConnectorsService);
        when(gxfsCatalogRestService.getParticipantById(any())).thenThrow(Exception.class);
        doReturn(new MerlotParticipantDto()).when(gxfsCatalogRestService).getParticipantById("10");
        doReturn(new OrganisationConnectorExtension()).when(organisationConnectorsService).getConnector("10", "1234");
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
        OrganisationConnectorExtension model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNotNull(model);
    }

    @Test
    void requestOrganizationConnectorNonExistent()  {
        ConnectorDetailsRequest connectorDetailsRequest = new ConnectorDetailsRequest("garbage", "10");
        OrganisationConnectorExtension model = messageQueueService.organizationConnectorRequest(connectorDetailsRequest);
        assertNull(model);
    }
}
