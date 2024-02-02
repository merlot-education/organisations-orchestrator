package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.RegistrationNumber;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.mappers.PdfContentMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrganizationMapperTests {

    @MockBean
    private InitialDataLoader initialDataLoader;
    @Autowired
    OrganizationMapper organizationMapper;
    @Autowired
    PdfContentMapper pdfContentMapper;
    String mailAddress = "test@test.de";
    String organizationLegalName = "Organization Legal Name";
    String registrationNumber = "DE123456789";
    String countryCode = "DE";
    String street = "Street 123";
    String providerTncHash = "hash1234567890";
    String providerTncLink = "abc.de";
    String city = "City";
    String organizationName = "Organization Name";
    String postalCode = "12345";

    String orgaId = "10";

    @Test
    void mapRegistrationFormToSelfDescriptionCorrectly() throws IOException {
        MerlotOrganizationCredentialSubject expected = getExpectedCredentialSubject();
        PDAcroForm registrationForm = getTestRegistrationForm();

        RegistrationFormContent content = pdfContentMapper.getRegistrationFormContentFromRegistrationForm(registrationForm);
        MerlotOrganizationCredentialSubject mapped = organizationMapper.getSelfDescriptionFromRegistrationForm(content);
        assertThat(mapped).usingRecursiveComparison().isEqualTo(expected);
    }

    MerlotOrganizationCredentialSubject getExpectedCredentialSubject(){
        MerlotOrganizationCredentialSubject expected = new MerlotOrganizationCredentialSubject();
        expected.setOrgaName(organizationName);
        expected.setLegalName(organizationLegalName);

        RegistrationNumber registrationNumberObj = new RegistrationNumber();
        registrationNumberObj.setType("gax-trust-framework:RegistrationNumber");
        registrationNumberObj.setLocal(registrationNumber);
        expected.setRegistrationNumber(registrationNumberObj);

        VCard vCard = new VCard();
        vCard.setLocality(city);
        vCard.setPostalCode(postalCode);
        vCard.setCountryName(countryCode);
        vCard.setStreetAddress(street);
        vCard.setType("vcard:Address");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(providerTncLink);
        termsAndConditions.setHash(providerTncHash);
        termsAndConditions.setType("gax-trust-framework:TermsAndConditions");
        expected.setTermsAndConditions(termsAndConditions);

        return expected;
    }

    PDAcroForm getTestRegistrationForm() throws IOException {
        PDDocument pdDocument = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        pdDocument.addPage(page);

        PDAcroForm form = new PDAcroForm(pdDocument);
        pdDocument.getDocumentCatalog().setAcroForm(form);

        PDFont font = new PDType1Font(HELVETICA);
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);
        form.setDefaultResources(resources);

        String defaultAppearance = "/Helv 12 Tf 0 0 1 rg";

        PDTextField textField = new PDTextField(form);
        textField.setPartialName("MailAddress");
        textField.setDefaultAppearance(defaultAppearance);
        form.getFields().add(textField);
        textField.setValue(mailAddress);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName("OrganizationLegalName");
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);
        textField1.setValue(organizationLegalName);

        PDTextField textField2 = new PDTextField(form);
        textField2.setPartialName("RegistrationNumber");
        form.getFields().add(textField2);
        textField2.setDefaultAppearance(defaultAppearance);
        textField2.setValue(registrationNumber);

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName("CountryCode");
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);
        textField3.setValue(countryCode);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName("OrganizationName");
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);
        textField4.setValue(organizationName);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName("PostalCode");
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);
        textField5.setValue(postalCode);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName("City");
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);
        textField7.setValue(city);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName("ProviderTncLink");
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);
        textField8.setValue(providerTncLink);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName("Street");
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);
        textField9.setValue(street);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName("ProviderTncHash");
        form.getFields().add(textField10);
        textField10.setDefaultAppearance(defaultAppearance);
        textField10.setValue(providerTncHash);
        return form;
    }

    @Test
    void mapOrganizationMetadataToMerlotParticipantMetaDtoCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(orgaId);
        expected.setMailAddress(mailAddress);
        expected.setMembershipClass(MembershipClass.PARTICIPANT);

        OrganizationMetadata entity = new OrganizationMetadata(orgaId, mailAddress, MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto mapped = organizationMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());

        expected.setMembershipClass(MembershipClass.FEDERATOR);
        entity.setMembershipClass(MembershipClass.FEDERATOR);

        mapped = organizationMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());
    }

    @Test
    void updateOrganizationMetadataWithMerlotParticipantMetaDtoCorrectly() {

        OrganizationMetadata expectedMetadata = new OrganizationMetadata(orgaId, mailAddress, MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto dto = new MerlotParticipantMetaDto();
        dto.setOrgaId("changedId");
        dto.setMailAddress(mailAddress);
        dto.setMembershipClass(MembershipClass.PARTICIPANT);

        OrganizationMetadata targetMetadata = new OrganizationMetadata(orgaId, null, null);

        organizationMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getOrgaId(), targetMetadata.getOrgaId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());

        targetMetadata = new OrganizationMetadata(orgaId, null, null);
        expectedMetadata.setMembershipClass(MembershipClass.FEDERATOR);
        dto.setMembershipClass(MembershipClass.FEDERATOR);

        organizationMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getOrgaId(), targetMetadata.getOrgaId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());
    }

    @Test
    void updateOrganizationMetadataAsParticipantCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(orgaId);
        expected.setMailAddress("changedMail");
        expected.setMembershipClass(MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto target = new MerlotParticipantMetaDto();
        target.setOrgaId(orgaId);
        target.setMailAddress(mailAddress);
        target.setMembershipClass(MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto edited = new MerlotParticipantMetaDto();
        edited.setOrgaId("Participant:foo");
        edited.setMailAddress("changedMail");
        edited.setMembershipClass(MembershipClass.FEDERATOR);

        organizationMapper.updateMerlotParticipantMetaDtoAsParticipant(edited, target);
        assertEquals(expected.getOrgaId(), target.getOrgaId());
        assertEquals(expected.getMembershipClass(), target.getMembershipClass());
        assertEquals(expected.getMailAddress(), target.getMailAddress());
    }

    @Test
    void updateOrganizationMetadataAsFedAdminCorrectly() {

        MerlotParticipantMetaDto expected = new MerlotParticipantMetaDto();
        expected.setOrgaId(orgaId);
        expected.setMailAddress("changedMail");
        expected.setMembershipClass(MembershipClass.FEDERATOR);

        MerlotParticipantMetaDto target = new MerlotParticipantMetaDto();
        target.setOrgaId(orgaId);
        target.setMailAddress(mailAddress);
        target.setMembershipClass(MembershipClass.PARTICIPANT);

        MerlotParticipantMetaDto edited = new MerlotParticipantMetaDto();
        edited.setOrgaId("20");
        edited.setMailAddress("changedMail");
        edited.setMembershipClass(MembershipClass.FEDERATOR);

        organizationMapper.updateMerlotParticipantMetaDtoAsFedAdmin(edited, target);
        assertEquals(expected.getOrgaId(), target.getOrgaId());
        assertEquals(expected.getMembershipClass(), target.getMembershipClass());
        assertEquals(expected.getMailAddress(), target.getMailAddress());
    }
}
