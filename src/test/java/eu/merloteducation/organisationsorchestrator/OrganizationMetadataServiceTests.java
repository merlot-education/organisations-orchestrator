package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
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

import java.util.List;

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

        someOrgaId = "did:web:" + merlotDomain + "#" + "someorga";
        otherOrgaId = "did:web:" + merlotDomain + "#" + "otherorga";
    }

    @BeforeEach
    void setUpData() {

        OrganizationMetadata metadata1 = new OrganizationMetadata(someOrgaId, "abd@de.fg",
            MembershipClass.FEDERATOR, true);
        OrganizationMetadata metadata2 = new OrganizationMetadata(otherOrgaId, "hij@kl.mn",
            MembershipClass.PARTICIPANT, false);

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

        MerlotParticipantMetaDto actual1 = metadataService.getMerlotParticipantMetaDto(someOrgaId);
        assertEquals(expected1.getOrgaId(), actual1.getOrgaId());
        assertEquals(expected1.getMembershipClass(), actual1.getMembershipClass());
        assertEquals(expected1.getMailAddress(), actual1.getMailAddress());
        assertEquals(expected1.isActive(), actual1.isActive());

        MerlotParticipantMetaDto expected2 = new MerlotParticipantMetaDto();
        expected2.setOrgaId(otherOrgaId);
        expected2.setMailAddress("hij@kl.mn");
        expected2.setMembershipClass(MembershipClass.PARTICIPANT);
        expected2.setActive(false);

        MerlotParticipantMetaDto actual2 = metadataService.getMerlotParticipantMetaDto(otherOrgaId);
        assertEquals(expected2.getOrgaId(), actual2.getOrgaId());
        assertEquals(expected2.getMembershipClass(), actual2.getMembershipClass());
        assertEquals(expected2.getMailAddress(), actual2.getMailAddress());
        assertEquals(expected2.isActive(), actual2.isActive());
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

        MerlotParticipantMetaDto actual = metadataService.saveMerlotParticipantMeta(metadataToSave);

        assertEquals(expected.getOrgaId(), actual.getOrgaId());
        assertEquals(expected.getMembershipClass(), actual.getMembershipClass());
        assertEquals(expected.getMailAddress(), actual.getMailAddress());
        assertEquals(expected.isActive(), actual.isActive());

        // clean-up
        metadataRepository.deleteById(id);
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaFail() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("7d0ad7ce-cb1f-479f-9b7d-33b0d7d6f347");
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(false);

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertNull(actual);
    }

    @Transactional
    @Test
    void updateMerlotParticipantMetaCorrectly() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(otherOrgaId);
        metaDto.setMailAddress("foo@bar.de");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        metaDto.setActive(true);

        MerlotParticipantMetaDto actual = metadataService.updateMerlotParticipantMeta(metaDto);
        assertEquals(metaDto.getOrgaId(), actual.getOrgaId());
        assertEquals(metaDto.getMembershipClass(), actual.getMembershipClass());
        assertEquals(metaDto.getMailAddress(), actual.getMailAddress());
        assertEquals(metaDto.isActive(), actual.isActive());
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
}
