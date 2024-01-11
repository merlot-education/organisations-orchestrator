package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class OrganizationMetadataMapperTests {
    @Autowired
    private OrganizationMetadataMapper metadataMapper;

    private final String MERLOT_ID = "10";

    private final String PARTICIPANT = "PARTICIPANT";

    private final String FEDERATOR = "FEDERATOR";

    private final String MAIL_ADDRESS = "abd@de.fg";

    @Test
    void mapOrganizationMetadataToMerlotParticipantMetaDtoCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(MERLOT_ID);
        expected.setMailAddress(MAIL_ADDRESS);
        expected.setMembershipClass(PARTICIPANT);

        OrganizationMetadata entity = new OrganizationMetadata(MERLOT_ID, MAIL_ADDRESS, MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto mapped = metadataMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());

        expected.setMembershipClass(FEDERATOR);
        entity.setMembershipClass(MembershipClass.FEDERATOR);

        mapped = metadataMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());
    }

    @Test
    void updateOrganizationMetadataWithMerlotParticipantMetaDtoCorrectly() {

        OrganizationMetadata expectedMetadata = new OrganizationMetadata(MERLOT_ID, MAIL_ADDRESS, MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto dto = new MerlotParticipantMetaDto();
        dto.setOrgaId("changedId");
        dto.setMailAddress(MAIL_ADDRESS);
        dto.setMembershipClass(" " + PARTICIPANT.toLowerCase() + " ");

        OrganizationMetadata targetMetadata = new OrganizationMetadata(MERLOT_ID, null, null);

        metadataMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getMerlotId(), targetMetadata.getMerlotId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());

        targetMetadata = new OrganizationMetadata(MERLOT_ID, null, null);
        expectedMetadata.setMembershipClass(MembershipClass.FEDERATOR);
        dto.setMembershipClass(" " + FEDERATOR.toLowerCase() + " ");

        metadataMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getMerlotId(), targetMetadata.getMerlotId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());
    }

    @Test
    void updateOrganizationMetadataAsParticipantCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(MERLOT_ID);
        expected.setMailAddress("changedMail");
        expected.setMembershipClass(PARTICIPANT);

        MerlotParticipantMetaDto target = new MerlotParticipantMetaDto();
        target.setOrgaId(MERLOT_ID);
        target.setMailAddress(MAIL_ADDRESS);
        target.setMembershipClass(PARTICIPANT);

        MerlotParticipantMetaDto edited = new MerlotParticipantMetaDto();
        edited.setOrgaId("Participant:foo");
        edited.setMailAddress("changedMail");
        edited.setMembershipClass(FEDERATOR);

        metadataMapper.updateMerlotParticipantMetaDtoAsParticipant(edited, target);
        assertEquals(expected.getOrgaId(), target.getOrgaId());
        assertEquals(expected.getMembershipClass(), target.getMembershipClass());
        assertEquals(expected.getMailAddress(), target.getMailAddress());
    }

    @Test
    void updateOrganizationMetadataAsFedAdminCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(MERLOT_ID);
        expected.setMailAddress("changedMail");
        expected.setMembershipClass(FEDERATOR);

        MerlotParticipantMetaDto target = new MerlotParticipantMetaDto();
        target.setOrgaId(MERLOT_ID);
        target.setMailAddress(MAIL_ADDRESS);
        target.setMembershipClass(PARTICIPANT);

        MerlotParticipantMetaDto edited = new MerlotParticipantMetaDto();
        edited.setOrgaId("20");
        edited.setMailAddress("changedMail");
        edited.setMembershipClass(FEDERATOR);

        metadataMapper.updateMerlotParticipantMetaDtoAsFedAdmin(edited, target);
        assertEquals(expected.getOrgaId(), target.getOrgaId());
        assertEquals(expected.getMembershipClass(), target.getMembershipClass());
        assertEquals(expected.getMailAddress(), target.getMailAddress());
    }

}
