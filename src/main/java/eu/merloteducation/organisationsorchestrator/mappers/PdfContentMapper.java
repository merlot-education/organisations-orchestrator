package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
    //@Mapping(target = "didWeb", expression = "java(pDAcroForm.getField(DocumentField.DIDWEB.getValue()).getValueAsString())") // TODO add this once form supports it
    RegistrationFormContent getRegistrationFormContentFromRegistrationForm(PDAcroForm pDAcroForm);
}
