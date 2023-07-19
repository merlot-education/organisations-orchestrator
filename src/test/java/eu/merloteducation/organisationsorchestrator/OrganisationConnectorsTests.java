package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import eu.merloteducation.organisationsorchestrator.service.OrganisationConnectorsService;
import org.springframework.test.util.ReflectionTestUtils;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;

import java.util.ArrayList;
import java.util.List;
import eu.merloteducation.organisationsorchestrator.models.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.PatchOrganisationConnectorModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@SpringBootTest
public class OrganisationConnectorsTests {
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
        postModel.setId("Connector:112");
        postModel.setConnectorEndpoint("https://google.de");
        postModel.setConnectorAccessToken("token$123?");
        postModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.postConnector("Oraga:110", postModel);
    }

    @Transactional
    @Test
    void postConnectorForOrganisation() throws Exception {
        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        PostOrganisationConnectorModel postModel = new PostOrganisationConnectorModel();
        postModel.setId("Connector:911");
        postModel.setConnectorEndpoint("https://google.de");
        postModel.setConnectorAccessToken("token$123?");
        postModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.postConnector("Oraga:110", postModel);
        assertNotNull(connector);
    }

    @Transactional
    @Test
    void patchConnectorForOrganisation() throws Exception {
        List<String> buckets = new ArrayList<String>();
        buckets.add("bucketA");
        buckets.add("bucketB");
        buckets.add("bucketC");

        PatchOrganisationConnectorModel patchModel = new PatchOrganisationConnectorModel();
        patchModel.setConnectorEndpoint("https://google.com");
        patchModel.setConnectorAccessToken("token$ABC?");
        patchModel.setBucketNames(buckets);
        OrganisationConnectorExtension connector = connectorsService.patchConnector("Oraga:110", "Connector:112", patchModel);
        assertNotNull(connector);
    }

    @Test
    void getConnectorForOrganisation() throws Exception {
        OrganisationConnectorExtension connector = connectorsService.getConnector("Oraga:110", "Connector:112");
    }

    @Transactional
    @Test
    void getAllConnectorsForOrganisation() throws Exception {
        List<OrganisationConnectorExtension> connectors = connectorsService.getAllConnectors("Oraga:110");
        assertNotNull(connectors);
    }

    @Transactional
    @Test
    void deleteConnector() throws Exception {
        connectorsService.deleteConnector("Connector:112");
    }

}
