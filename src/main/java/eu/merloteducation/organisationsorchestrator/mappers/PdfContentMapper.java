package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PdfContentMapper {
    @Mapping(target = "organizationName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString())")
    @Mapping(target = "organizationLegalName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumberLocal", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER.getValue()).getValueAsString())")
    @Mapping(target = "mailAddress", expression = "java(pDAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString())")
    @Mapping(target = "providerTncLink", expression = "java(pDAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString())")
    @Mapping(target = "providerTncHash", expression = "java(pDAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString())")
    @Mapping(target = "countryCode", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString())")
    @Mapping(target = "city", expression = "java(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString())")
    @Mapping(target = "postalCode", expression = "java(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString())")
    @Mapping(target = "street", expression = "java(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString())")
    @Mapping(target = "didWeb", source = "pDAcroForm", qualifiedByName = "didWebFromForm")
    RegistrationFormContent getRegistrationFormContentFromRegistrationForm(PDAcroForm pDAcroForm);

    @Named("didWebFromForm")
    default String getDidWebFromRegistrationForm(PDAcroForm pDAcroForm) {
        try {
            return pDAcroForm.getField(DocumentField.DIDWEB.getValue()).getValueAsString();
        } catch (Exception ignored) { // in case the form doesn't have the field
            return "";
        }
    }
}
