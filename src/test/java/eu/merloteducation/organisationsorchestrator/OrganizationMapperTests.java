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
    String addressCode = "DE-C";

    @Test
    void mapRegistrationFormToSelfDescriptionCorrectly() throws IOException {
        MerlotOrganizationCredentialSubject expected = getExpectedCredentialSubject();
        PDAcroForm registrationForm = getTestRegistrationForm();

        MerlotOrganizationCredentialSubject mapped = organizationMapper.getSelfDescriptionFromRegistrationForm(registrationForm);
        assertThat(mapped).usingRecursiveComparison().isEqualTo(expected);
    }

    MerlotOrganizationCredentialSubject getExpectedCredentialSubject(){
        MerlotOrganizationCredentialSubject expected = new MerlotOrganizationCredentialSubject();
        expected.setAddressCode(new StringTypeValue(addressCode));
        expected.setMailAddress(new StringTypeValue(mailAddress));
        expected.setOrgaName(new StringTypeValue(organizationName));
        expected.setLegalName(new StringTypeValue(organizationLegalName));

        RegistrationNumber registrationNumberObj = new RegistrationNumber();
        registrationNumberObj.setType("gax-trust-framework:RegistrationNumber");
        registrationNumberObj.setLocal(new StringTypeValue(registrationNumber));
        expected.setRegistrationNumber(registrationNumberObj);

        VCard vCard = new VCard();
        vCard.setLocality(new StringTypeValue(city));
        vCard.setPostalCode(new StringTypeValue(postalCode));
        vCard.setCountryName(new StringTypeValue(countryCode));
        vCard.setStreetAddress(new StringTypeValue(street));
        vCard.setType("vcard:Address");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue(providerTncLink, "xsd:anyURI"));
        termsAndConditions.setHash(new StringTypeValue(providerTncHash));
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

        PDTextField textField6 = new PDTextField(form);
        textField6.setPartialName("AddressCode");
        form.getFields().add(textField6);
        textField6.setDefaultAppearance(defaultAppearance);
        textField6.setValue(addressCode);

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
}
