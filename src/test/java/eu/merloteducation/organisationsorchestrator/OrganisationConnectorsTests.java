package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import org.springframework.test.util.ReflectionTestUtils;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@SpringBootTest
class OrganisationConnectorsTests {

    @Autowired
    private OrganisationConnectorsService connectorsService;

    @Autowired
    private OrganisationConnectorsExtensionRepository connectorsRepo;

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectionTestUtils.setField(connectorsService, "connectorsRepo", connectorsRepo);

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        PostOrganisationConnectorModel postModel = new PostOrganisationConnectorModel();
        postModel.setConnectorId("Connector:112");
        postModel.setConnectorEndpoint("https://edc1.edchub.dev");
        postModel.setConnectorAccessToken("token$123?");
        postModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.postConnector("Orga:110", postModel);
    }

    @Transactional
    @Test
    void postConnectorForOrganisation() throws Exception {
        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        PostOrganisationConnectorModel postModel = new PostOrganisationConnectorModel();
        postModel.setConnectorId("Connector:911");
        postModel.setConnectorEndpoint("https://edc1.edchub.dev");
        postModel.setConnectorAccessToken("token$123?");
        postModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.postConnector("Orga:110", postModel);

        assertNotNull(connector);
        assertEquals("Connector:911", connector.getConnectorId());
        assertEquals("Orga:110", connector.getOrgaId());
        assertEquals("https://edc1.edchub.dev", connector.getConnectorEndpoint());
        assertEquals("token$123?", connector.getConnectorAccessToken());
        assertEquals(buckets, connector.getBucketNames());
    }

    @Transactional
    @Test
    void patchConnectorForOrganisation() throws Exception {
        List<String> buckets = new ArrayList<String>();
        buckets.add("bucketA");
        buckets.add("bucketB");
        buckets.add("bucketC");

        PatchOrganisationConnectorModel patchModel = new PatchOrganisationConnectorModel();
        patchModel.setConnectorEndpoint("https://edc1.edchub.dev");
        patchModel.setConnectorAccessToken("token$ABC?");
        patchModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.patchConnector("Orga:110", "Connector:112", patchModel);

        assertNotNull(connector);
        assertEquals("Connector:112", connector.getConnectorId());
        assertEquals("Orga:110", connector.getOrgaId());
        assertEquals("https://edc1.edchub.dev", connector.getConnectorEndpoint());
        assertEquals("token$ABC?", connector.getConnectorAccessToken());
        assertEquals(buckets, connector.getBucketNames());
    }

    @Test
    void getConnectorForOrganisation() throws Exception {
        OrganisationConnectorExtension connector = connectorsService.getConnector("Orga:110", "Connector:112");

        assertNotNull(connector);
        assertEquals("Connector:112", connector.getConnectorId());
        assertEquals("Orga:110", connector.getOrgaId());
        assertEquals("https://edc1.edchub.dev", connector.getConnectorEndpoint());
        assertEquals("token$123?", connector.getConnectorAccessToken());
    }

    @Transactional
    @Test
    void getAllConnectorsForOrganisation() throws Exception {
        List<OrganisationConnectorExtension> connectors = connectorsService.getAllConnectors("Orga:110");

        assertNotNull(connectors);
        assertEquals(1, connectors.size());

        var connector = connectors.get(0);
        assertEquals("Connector:112", connector.getConnectorId());
        assertEquals("Orga:110", connector.getOrgaId());
        assertEquals("https://edc1.edchub.dev", connector.getConnectorEndpoint());
        assertEquals("token$123?", connector.getConnectorAccessToken());
    }

    @Transactional
    @Test
    void deleteConnector() throws Exception {
        connectorsService.deleteConnector("Connector:112");

    }

}
