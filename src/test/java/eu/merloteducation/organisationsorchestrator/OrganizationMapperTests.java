package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.modelslib.gxfscatalog.datatypes.RegistrationNumber;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.StringTypeValue;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.TermsAndConditions;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.VCard;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
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

import java.io.IOException;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

@SpringBootTest
class OrganizationMapperTests {
    @Autowired
    OrganizationMapper organizationMapper;

    private final String MAIL_ADDRESS = "test@test.de";

    private final String ORGANIZATION_LEGAL_NAME = "Organization Legal Name";

    private final String REGISTRATION_NUMBER = "DE123456789";

    private final String COUNTRY_CODE = "DE";

    private final String STREET = "Street 123";

    private final String PROVIDER_TNC_HASH = "hash1234567890";

    private final String PROVIDER_TNC_LINK = "abc.de";

    private final String CITY = "City";

    private final String ORGANIZATION_NAME = "Organization Name";

    private final String POSTAL_CODE = "12345";

    @Test
    void mapRegistrationFormToSelfDescriptionCorrectly() throws IOException {

        MerlotOrganizationCredentialSubject expected = getExpectedCredentialSubject();
        PDAcroForm registrationForm = getTestRegistrationForm();

        MerlotOrganizationCredentialSubject mapped = organizationMapper.getSelfDescriptionFromRegistrationForm(
            registrationForm);
        assertThat(mapped).usingRecursiveComparison().isEqualTo(expected);
    }

    MerlotOrganizationCredentialSubject getExpectedCredentialSubject() {

        MerlotOrganizationCredentialSubject expected = new MerlotOrganizationCredentialSubject();
        expected.setOrgaName(new StringTypeValue(ORGANIZATION_NAME));
        expected.setLegalName(new StringTypeValue(ORGANIZATION_LEGAL_NAME));

        RegistrationNumber registrationNumberObj = new RegistrationNumber();
        registrationNumberObj.setType("gax-trust-framework:RegistrationNumber");
        registrationNumberObj.setLocal(new StringTypeValue(REGISTRATION_NUMBER));
        expected.setRegistrationNumber(registrationNumberObj);

        VCard vCard = new VCard();
        vCard.setLocality(new StringTypeValue(CITY));
        vCard.setPostalCode(new StringTypeValue(POSTAL_CODE));
        vCard.setCountryName(new StringTypeValue(COUNTRY_CODE));
        vCard.setStreetAddress(new StringTypeValue(STREET));
        vCard.setType("vcard:Address");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue(PROVIDER_TNC_LINK, "xsd:anyURI"));
        termsAndConditions.setHash(new StringTypeValue(PROVIDER_TNC_HASH));
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
        textField.setValue(MAIL_ADDRESS);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName("OrganizationLegalName");
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);
        textField1.setValue(ORGANIZATION_LEGAL_NAME);

        PDTextField textField2 = new PDTextField(form);
        textField2.setPartialName("RegistrationNumber");
        form.getFields().add(textField2);
        textField2.setDefaultAppearance(defaultAppearance);
        textField2.setValue(REGISTRATION_NUMBER);

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName("CountryCode");
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);
        textField3.setValue(COUNTRY_CODE);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName("OrganizationName");
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);
        textField4.setValue(ORGANIZATION_NAME);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName("PostalCode");
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);
        textField5.setValue(POSTAL_CODE);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName("City");
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);
        textField7.setValue(CITY);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName("ProviderTncLink");
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);
        textField8.setValue(PROVIDER_TNC_LINK);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName("Street");
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);
        textField9.setValue(STREET);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName("ProviderTncHash");
        form.getFields().add(textField10);
        textField10.setDefaultAppearance(defaultAppearance);
        textField10.setValue(PROVIDER_TNC_HASH);
        return form;
    }
}
