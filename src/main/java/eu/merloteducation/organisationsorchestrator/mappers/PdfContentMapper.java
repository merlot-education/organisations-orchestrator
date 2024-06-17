package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {PDChoice.class})
public interface PdfContentMapper {
    @Mapping(target = "organizationName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString())")
    @Mapping(target = "organizationLegalName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString())")
    @Mapping(target = "mailAddress", expression = "java(pDAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString())")
    @Mapping(target = "providerTncLink", expression = "java(pDAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString())")
    @Mapping(target = "providerTncHash", expression = "java(pDAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString())")
    @Mapping(target = "countryCode", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString())")
    @Mapping(target = "countrySubdivisionCode", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYSUBDIVISIONCODE.getValue()).getValueAsString())")
    @Mapping(target = "city", expression = "java(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString())")
    @Mapping(target = "postalCode", expression = "java(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString())")
    @Mapping(target = "street", expression = "java(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString())")
    @Mapping(target = "legalForm", expression = "java(((PDChoice) pDAcroForm.getField(DocumentField.LEGALFORM.getValue())).getValue().get(0))")
    @Mapping(target = "registrationNumberLeiCode", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER_LEICODE.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumberTaxID", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER_TAXID.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumberEuid", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER_EUID.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumberEori", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER_EORI.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumberVatID", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER_VATID.getValue()).getValueAsString())")
    RegistrationFormContent getRegistrationFormContentFromRegistrationForm(PDAcroForm pDAcroForm);
}
