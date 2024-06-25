package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.DocumentField;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.mappers.ParticipantCredentialMapper;
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
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrganizationMapperTests {

    @MockBean
    private InitialDataLoader initialDataLoader;
    @Autowired
    OrganizationMapper organizationMapper;

    // TODO move this to its own test class
    @Autowired
    ParticipantCredentialMapper participantCredentialMapper;

    @Autowired
    PdfContentMapper pdfContentMapper;
    String mailAddress = "test@test.de";
    String organizationLegalName = "Organization Legal Name";
    String organizationLegalForm = "LLC";
    String registrationNumber = "894500MQZ65CN32S9A66";
    String countryCode = "DE";
    String countrySubdivisionCode = "DE-BE";
    String street = "Street 123";
    String providerTncHash = "hash1234567890";
    String providerTncLink = "abc.de";
    String city = "City";
    String organizationName = "Organization Name";
    String postalCode = "12345";

    String orgaId = "10";

    @Test
    void mapRegistrationFormToSelfDescriptionCorrectly() throws IOException {
        MerlotLegalParticipantCredentialSubject expectedMerlotParticipantCs = getExpectedMerlotParticipantCredentialSubject();
        GxLegalParticipantCredentialSubject expectedGxParticipantCs = getExpectedGxParticipantCredentialSubject();
        GxLegalRegistrationNumberCredentialSubject expectedGxRegistrationNumberCs = getExpectedRegistrationNumberCredentialSubject();
        PDAcroForm registrationForm = getTestRegistrationForm();

        RegistrationFormContent content = pdfContentMapper.getRegistrationFormContentFromRegistrationForm(registrationForm);
        MerlotLegalParticipantCredentialSubject mappedMerlotParticipantCs =
                participantCredentialMapper.getMerlotParticipantCsFromRegistrationForm(content);
        GxLegalParticipantCredentialSubject mappedGxParticipantCs =
                participantCredentialMapper.getLegalParticipantCsFromRegistrationForm(content);
        GxLegalRegistrationNumberCredentialSubject mappedGxRegistrationNumberCs =
                participantCredentialMapper.getLegalRegistrationNumberFromRegistrationForm(content);
        assertThat(mappedMerlotParticipantCs).usingRecursiveComparison().isEqualTo(expectedMerlotParticipantCs);
        assertThat(mappedGxParticipantCs).usingRecursiveComparison().ignoringFields("legalRegistrationNumber").isEqualTo(expectedGxParticipantCs);
        assertThat(mappedGxRegistrationNumberCs).usingRecursiveComparison().isEqualTo(expectedGxRegistrationNumberCs);
    }

    MerlotLegalParticipantCredentialSubject getExpectedMerlotParticipantCredentialSubject(){
        MerlotLegalParticipantCredentialSubject expected = new MerlotLegalParticipantCredentialSubject();
        expected.setLegalName(organizationLegalName);

        ParticipantTermsAndConditions termsAndConditions = new ParticipantTermsAndConditions();
        termsAndConditions.setUrl(providerTncLink);
        termsAndConditions.setHash(providerTncHash);
        expected.setTermsAndConditions(termsAndConditions);
        expected.setLegalForm(organizationLegalForm);

        return expected;
    }

    GxLegalParticipantCredentialSubject getExpectedGxParticipantCredentialSubject(){
        GxLegalParticipantCredentialSubject expected = new GxLegalParticipantCredentialSubject();
        expected.setName(organizationName);

        expected.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId(orgaId + "-regId")));
        GxVcard vCard = new GxVcard();
        vCard.setLocality(city);
        vCard.setPostalCode(postalCode);
        vCard.setCountryCode(countryCode);
        vCard.setCountrySubdivisionCode(countrySubdivisionCode);
        vCard.setStreetAddress(street);
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        return expected;
    }

    GxLegalRegistrationNumberCredentialSubject getExpectedRegistrationNumberCredentialSubject(){
        GxLegalRegistrationNumberCredentialSubject expected = new GxLegalRegistrationNumberCredentialSubject();

        expected.setLeiCode(registrationNumber);

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
        textField.setPartialName(DocumentField.MAILADDRESS.getValue());
        textField.setDefaultAppearance(defaultAppearance);
        form.getFields().add(textField);
        textField.setValue(mailAddress);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName(DocumentField.ORGANIZATIONLEGALNAME.getValue());
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);
        textField1.setValue(organizationLegalName);

        PDTextField textFieldLeiCode = new PDTextField(form);
        textFieldLeiCode.setPartialName(DocumentField.REGISTRATIONNUMBER_LEICODE.getValue());
        form.getFields().add(textFieldLeiCode);
        textFieldLeiCode.setDefaultAppearance(defaultAppearance);
        textFieldLeiCode.setValue(registrationNumber);

        PDTextField textFieldVatId = new PDTextField(form);
        textFieldVatId.setPartialName(DocumentField.REGISTRATIONNUMBER_VATID.getValue());
        form.getFields().add(textFieldVatId);
        textFieldVatId.setDefaultAppearance(defaultAppearance);
        textFieldVatId.setValue("");

        PDTextField textFieldEori = new PDTextField(form);
        textFieldEori.setPartialName(DocumentField.REGISTRATIONNUMBER_EORI.getValue());
        form.getFields().add(textFieldEori);
        textFieldEori.setDefaultAppearance(defaultAppearance);
        textFieldEori.setValue("");

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName(DocumentField.COUNTRYCODE.getValue());
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);
        textField3.setValue(countryCode);

        PDTextField textFieldCountrySubCode = new PDTextField(form);
        textFieldCountrySubCode.setPartialName(DocumentField.COUNTRYSUBDIVISIONCODE.getValue());
        form.getFields().add(textFieldCountrySubCode);
        textFieldCountrySubCode.setDefaultAppearance(defaultAppearance);
        textFieldCountrySubCode.setValue(countrySubdivisionCode);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName(DocumentField.ORGANIZATIONNAME.getValue());
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);
        textField4.setValue(organizationName);

        PDListBox choicelegalForm = new PDListBox(form);
        choicelegalForm.setPartialName(DocumentField.LEGALFORM.getValue());
        form.getFields().add(choicelegalForm);
        choicelegalForm.setDefaultAppearance(defaultAppearance);
        choicelegalForm.setValue(organizationLegalForm);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName(DocumentField.POSTALCODE.getValue());
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);
        textField5.setValue(postalCode);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName(DocumentField.CITY.getValue());
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);
        textField7.setValue(city);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName(DocumentField.TNCLINK.getValue());
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);
        textField8.setValue(providerTncLink);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName(DocumentField.STREET.getValue());
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);
        textField9.setValue(street);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName(DocumentField.TNCHASH.getValue());
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
        expected.setActive(false);

        OrganizationMetadata entity = new OrganizationMetadata(orgaId, mailAddress, MembershipClass.PARTICIPANT, false, Collections.emptySet());

        MerlotParticipantMetaDto mapped = organizationMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());
        assertEquals(expected.isActive(), mapped.isActive());

        expected.setMembershipClass(MembershipClass.FEDERATOR);
        expected.setActive(true);
        entity.setMembershipClass(MembershipClass.FEDERATOR);
        entity.setActive(true);

        mapped = organizationMapper.organizationMetadataToMerlotParticipantMetaDto(entity);
        assertEquals(expected.getOrgaId(), mapped.getOrgaId());
        assertEquals(expected.getMembershipClass(), mapped.getMembershipClass());
        assertEquals(expected.getMailAddress(), mapped.getMailAddress());
        assertEquals(expected.isActive(), mapped.isActive());
    }

    @Test
    void updateOrganizationMetadataWithMerlotParticipantMetaDtoCorrectly() {

        OrganizationMetadata expectedMetadata = new OrganizationMetadata(orgaId, mailAddress, MembershipClass.PARTICIPANT, true, Collections.emptySet());

        MerlotParticipantMetaDto dto = new MerlotParticipantMetaDto();
        dto.setOrgaId("changedId");
        dto.setMailAddress(mailAddress);
        dto.setMembershipClass(MembershipClass.PARTICIPANT);
        dto.setActive(true);

        OrganizationMetadata targetMetadata = new OrganizationMetadata(orgaId, null, null, false, Collections.emptySet());

        organizationMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getOrgaId(), targetMetadata.getOrgaId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());
        assertEquals(expectedMetadata.isActive(), targetMetadata.isActive());

        targetMetadata = new OrganizationMetadata(orgaId, null, null, false, Collections.emptySet());
        expectedMetadata.setMembershipClass(MembershipClass.FEDERATOR);
        dto.setMembershipClass(MembershipClass.FEDERATOR);

        organizationMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(dto, targetMetadata);
        assertEquals(expectedMetadata.getOrgaId(), targetMetadata.getOrgaId());
        assertEquals(expectedMetadata.getMailAddress(), targetMetadata.getMailAddress());
        assertEquals(expectedMetadata.getMembershipClass(), targetMetadata.getMembershipClass());
        assertEquals(expectedMetadata.isActive(), targetMetadata.isActive());
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
