package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.MessageQueueService;
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

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(messageQueueService, "gxfsCatalogRestService", gxfsCatalogRestService);
        when(gxfsCatalogRestService.getParticipantById("10")).thenReturn(new OrganizationModel());
    }


    @Test
    void requestOrganizationExistent() throws Exception {
        OrganizationModel model = messageQueueService.organizationRequest("10");
        assertNotNull(model);
    }

    @Test
    void requestOrganizationNonExistent() throws Exception {
        OrganizationModel model = messageQueueService.organizationRequest("garbage");
        assertNull(model);
    }
}
