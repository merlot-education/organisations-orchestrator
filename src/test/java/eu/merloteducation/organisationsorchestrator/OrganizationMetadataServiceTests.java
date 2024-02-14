package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import eu.merloteducation.organisationsorchestrator.service.OrganizationMetadataService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationMetadataServiceTests {
    @Autowired
    private OrganizationMetadataService metadataService;

    @Autowired
    private OrganizationMetadataRepository metadataRepository;

    @Autowired
    private OrganisationConnectorsExtensionRepository connectorRepository;

    @Autowired
    private OrganizationMapper metadataMapper;

    @MockBean
    private InitialDataLoader initialDataLoader;

    @Value("${merlot-domain}")
    private String merlotDomain;

    private String someOrgaId;

    private String otherOrgaId;

    @BeforeAll
    void beforeAll() {

        ReflectionTestUtils.setField(metadataService, "repository", metadataRepository);
        ReflectionTestUtils.setField(metadataService, "mapper", metadataMapper);
        ReflectionTestUtils.setField(metadataService, "connectorRepository", connectorRepository);

        someOrgaId = "did:web:" + merlotDomain + "#" + "someorga";
        otherOrgaId = "did:web:" + merlotDomain + "#" + "otherorga";
    }

    @BeforeEach
    void setUpData() {

        OrganizationMetadata metadata1 = new OrganizationMetadata(someOrgaId, "abd@de.fg", MembershipClass.FEDERATOR,
            true);
        OrganizationMetadata metadata2 = new OrganizationMetadata(otherOrgaId, "hij@kl.mn", MembershipClass.PARTICIPANT,
            false);

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        OrganisationConnectorExtension connector = new OrganisationConnectorExtension();
        connector.setOrgaId(someOrgaId);
        connector.setConnectorId("edc1");
        connector.setConnectorEndpoint("https://edc1.edchub.dev");
        connector.setConnectorAccessToken("token$123?");
        connector.setBucketNames(buckets);
        metadata1.setConnectors(Set.of(connector));

        metadataRepository.save(metadata1);
        metadataRepository.save(metadata2);
    }

    @AfterEach
    void cleanUpData() {

        metadataRepository.deleteById(someOrgaId);
        metadataRepository.deleteById(otherOrgaId);
    }

    @Transactional
    @Test
    void getMerlotParticipantMetaDtoCorrectly() {

        MerlotParticipantMetaDto expected1 = new MerlotParticipantMetaDto();
        expected1.setOrgaId(someOrgaId);
        expected1.setMailAddress("abd@de.fg");
        expected1.setMembershipClass(MembershipClass.FEDERATOR);
        expected1.setActive(true);

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        OrganizationConnectorDto connector = new OrganizationConnectorDto();
        connector.setOrgaId(someOrgaId);
        connector.setConnectorId("edc1");
        connector.setConnectorEndpoint("https://edc1.edchub.dev");
        connector.setConnectorAccessToken("token$123?");
        connector.setBucketNames(buckets);
        expected1.setConnectors(Set.of(connector));

        MerlotParticipantMetaDto actual1 = metadataService.getMerlotParticipantMetaDto(someOrgaId);
        assertEquals(expected1.getOrgaId(), actual1.getOrgaId());
        assertEquals(expected1.getMembershipClass(), actual1.getMembershipClass());
        assertEquals(expected1.getMailAddress(), actual1.getMailAddress());
        assertEquals(expected1.isActive(), actual1.isActive());
        assertEquals(1, actual1.getConnectors().size());

        OrganizationConnectorDto actualDto = actual1.getConnectors().stream().findFirst().orElse(null);
        assertNotNull(actualDto);
        assertEquals(someOrgaId, actualDto.getOrgaId());
        assertEquals("edc1", actualDto.getConnectorId());
        assertEquals("https://edc1.edchub.dev", actualDto.getConnectorEndpoint());
        assertEquals("token$123?", actualDto.getConnectorAccessToken());
        assertEquals(buckets, actualDto.getBucketNames());

        MerlotParticipantMetaDto expected2 = new MerlotParticipantMetaDto();
        expected2.setOrgaId(otherOrgaId);
        expected2.setMailAddress("hij@kl.mn");
        expected2.setMembershipClass(MembershipClass.PARTICIPANT);
        expected2.setActive(false);
        expected2.setConnectors(new HashSet<>());

        MerlotParticipantMetaDto actual2 = metadataService.getMerlotParticipantMetaDto(otherOrgaId);
        assertEquals(expected2.getOrgaId(), actual2.getOrgaId());
        assertEquals(expected2.getMembershipClass(), actual2.getMembershipClass());
        assertEquals(expected2.getMailAddress(), actual2.getMailAddress());
        assertEquals(expected2.isActive(), actual2.isActive());
        assertEquals(new HashSet<>(), actual2.getConnectors());
    }

    @Transactional
    @Test
    void saveMerlotParticipantMetaCorrectly() {

        String id = "did:web:" + merlotDomain + "#" + "7d0ad7ce-cb1f-479f-9b7d-33b0d7d6f347";

        MerlotParticipantMetaDto metadataToSave = new MerlotParticipantMetaDto();
        metadataToSave.setOrgaId(id);
        metadataToSave.setMailAddress("foo@bar.de");
        metadataToSave.setMembershipClass(MembershipClass.FEDERATOR);
        metadataToSave.setActive(true);

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(id);
        expected.setMailAddress("foo@bar.de");
        expected.setMembershipClass(MembershipClass.FEDERATOR);
        expected.setActive(true);
        expected.setConnectors(new HashSet<>());

        MerlotParticipantMetaDto actual = metadataService.saveMerlotParticipantMeta(metadataToSave);

        assertEquals(expected.getOrgaId(), actual.getOrgaId());
        assertEquals(expected.getMembershipClass(), actual.getMembershipClass());
        assertEquals(expected.getMailAddress(), actual.getMailAddress());
        assertEquals(expected.isActive(), actual.isActive());
        assertEquals(expected.getConnectors(), actual.getConnectors());

        // clean-up
        metadataRepository.deleteById(id);
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaFail() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("did:web:" + merlotDomain + "#" + "fail");
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(false);

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertNull(actual);
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaCorrectlyNoPreexistingConnectors() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(otherOrgaId);
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(true);

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");

        OrganizationConnectorDto connector = new OrganizationConnectorDto();
        connector.setOrgaId("garbage");
        connector.setConnectorId("edctest");
        connector.setConnectorEndpoint("https://edc1.edchub.dev");
        connector.setConnectorAccessToken("token$123?");
        connector.setBucketNames(buckets);

        metaDto.setConnectors(Set.of(connector));

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
        assertEquals(1, actual.getConnectors().size());

        OrganizationConnectorDto actualDto = actual.getConnectors().stream().findFirst().orElse(null);
        assertNotNull(actualDto);
        assertEquals(otherOrgaId, actualDto.getOrgaId());
        assertEquals("edctest", actualDto.getConnectorId());
        assertEquals("https://edc1.edchub.dev", actualDto.getConnectorEndpoint());
        assertEquals("token$123?", actualDto.getConnectorAccessToken());
        assertEquals(buckets, actualDto.getBucketNames());
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaCorrectlyPreexistingConnectors() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(someOrgaId);
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(true);

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");

        OrganizationConnectorDto connector = new OrganizationConnectorDto();
        connector.setOrgaId("garbage");
        connector.setConnectorId("edctest");
        connector.setConnectorEndpoint("https://edc1.edchub.dev");
        connector.setConnectorAccessToken("token$123?");
        connector.setBucketNames(buckets);

        metaDto.setConnectors(Set.of(connector));

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
        assertEquals(1, actual.getConnectors().size());

        OrganizationConnectorDto actualDto = actual.getConnectors().stream().findFirst().orElse(null);
        assertNotNull(actualDto);
        assertEquals(someOrgaId, actualDto.getOrgaId());
        assertEquals("edctest", actualDto.getConnectorId());
        assertEquals("https://edc1.edchub.dev", actualDto.getConnectorEndpoint());
        assertEquals("token$123?", actualDto.getConnectorAccessToken());
        assertEquals(buckets, actualDto.getBucketNames());
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaCorrectlyUpdatePreexistingConnectors() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(someOrgaId);
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(true);

        List<String> buckets1 = new ArrayList<String>();
        buckets1.add("bucket1");
        buckets1.add("bucket2");

        List<String> buckets2 = new ArrayList<String>();
        buckets2.add("bucket1");
        buckets2.add("bucket2");
        buckets2.add("bucket3");
        buckets2.add("bucket4");
        buckets2.add("bucket5");

        OrganizationConnectorDto connector1 = new OrganizationConnectorDto();
        connector1.setOrgaId(someOrgaId);
        connector1.setConnectorId("edctest");
        connector1.setConnectorEndpoint("https://edc1.edchub.dev");
        connector1.setConnectorAccessToken("token$123?");
        connector1.setBucketNames(buckets1);

        OrganizationConnectorDto connector2 = new OrganizationConnectorDto();
        connector2.setOrgaId("garbage");
        connector2.setConnectorId("edc1");
        connector2.setConnectorEndpoint("https://edc1.edchub.dev#new");
        connector2.setConnectorAccessToken("token$123?");
        connector2.setBucketNames(buckets2);

        metaDto.setConnectors(Set.of(connector1, connector2));

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
        assertEquals(2, actual.getConnectors().size());

        OrganizationConnectorDto actualConnectorDto1 = actual.getConnectors().stream()
            .filter(con -> con.getConnectorId().equals("edctest")).findFirst().orElse(null);
        assertNotNull(actualConnectorDto1);
        assertEquals(someOrgaId, actualConnectorDto1.getOrgaId());
        assertEquals("edctest", actualConnectorDto1.getConnectorId());
        assertEquals("https://edc1.edchub.dev", actualConnectorDto1.getConnectorEndpoint());
        assertEquals("token$123?", actualConnectorDto1.getConnectorAccessToken());
        assertEquals(buckets1, actualConnectorDto1.getBucketNames());

        OrganizationConnectorDto actualConnectorDto2 = actual.getConnectors().stream()
            .filter(con -> con.getConnectorId().equals("edc1")).findFirst().orElse(null);
        assertNotNull(actualConnectorDto2);
        assertEquals(someOrgaId, actualConnectorDto2.getOrgaId());
        assertEquals("edc1", actualConnectorDto2.getConnectorId());
        assertEquals("https://edc1.edchub.dev#new", actualConnectorDto2.getConnectorEndpoint());
        assertEquals("token$123?", actualConnectorDto2.getConnectorAccessToken());
        assertEquals(buckets2, actualConnectorDto2.getBucketNames());
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaCorrectlyReplacePreexistingConnectors() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(someOrgaId);
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(true);

        List<String> buckets1 = new ArrayList<String>();
        buckets1.add("bucket1");
        buckets1.add("bucket2");

        OrganizationConnectorDto connector1 = new OrganizationConnectorDto();
        connector1.setOrgaId(someOrgaId);
        connector1.setConnectorId("edctest");
        connector1.setConnectorEndpoint("https://edc1.edchub.dev");
        connector1.setConnectorAccessToken("token$123?");
        connector1.setBucketNames(buckets1);

        metaDto.setConnectors(Set.of(connector1));

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
        assertEquals(1, actual.getConnectors().size());

        OrganizationConnectorDto actualDto1 = actual.getConnectors().stream()
            .filter(con -> con.getConnectorId().equals("edctest")).findFirst().orElse(null);
        assertNotNull(actualDto1);
        assertEquals(someOrgaId, actualDto1.getOrgaId());
        assertEquals("edctest", actualDto1.getConnectorId());
        assertEquals("https://edc1.edchub.dev", actualDto1.getConnectorEndpoint());
        assertEquals("token$123?", actualDto1.getConnectorAccessToken());
        assertEquals(buckets1, actualDto1.getBucketNames());

        metaDto.setConnectors(new HashSet<>());

        actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
        assertEquals(0, actual.getConnectors().size());

    }

    @Transactional
    @Test
    void getIdsOfInactiveParticipantsCorrectly() {

        List<String> inactiveOrgas = metadataService.getInactiveParticipantsIds();

        assertEquals(1, inactiveOrgas.size());
        assertEquals(otherOrgaId, inactiveOrgas.get(0));

        MerlotParticipantMetaDto metadata = new MerlotParticipantMetaDto();
        metadata.setOrgaId(someOrgaId);
        metadata.setMailAddress("abd@de.fg");
        metadata.setMembershipClass(MembershipClass.PARTICIPANT);
        metadata.setActive(false);

        metadataService.updateMerlotParticipantMeta(metadata);
        inactiveOrgas = metadataService.getInactiveParticipantsIds();
        assertEquals(2, inactiveOrgas.size());
        assertTrue(inactiveOrgas.contains(someOrgaId));
        assertTrue(inactiveOrgas.contains(otherOrgaId));
    }

    @Transactional
    @Test
    void getConnectorForParticipantCorrectly() {

        List<String> buckets = new ArrayList<String>();
        buckets.add("bucket1");
        buckets.add("bucket2");
        buckets.add("bucket3");

        OrganizationConnectorDto expected = new OrganizationConnectorDto();
        expected.setOrgaId(someOrgaId);
        expected.setConnectorId("edc1");
        expected.setConnectorEndpoint("https://edc1.edchub.dev");
        expected.setConnectorAccessToken("token$123?");
        expected.setBucketNames(buckets);

        OrganizationConnectorDto actual= metadataService.getConnectorForParticipant(someOrgaId, "edc1");

        assertNotNull(actual);
        assertEquals(expected.getOrgaId(), actual.getOrgaId());
        assertEquals(expected.getConnectorId(), actual.getConnectorId());
        assertEquals(expected.getConnectorEndpoint(), actual.getConnectorEndpoint());
        assertEquals(expected.getConnectorAccessToken(), actual.getConnectorAccessToken());
        assertEquals(buckets, actual.getBucketNames());

    }
}
