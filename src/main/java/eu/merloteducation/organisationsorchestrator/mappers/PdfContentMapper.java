package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PdfContentMapper {
    @Mapping(target = "organizationName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString())")
    @Mapping(target = "organizationLegalName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString())")
    @Mapping(target = "mailAddress", expression = "java(pDAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString())")
    @Mapping(target = "providerTncLink", expression = "java(pDAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString())")
    @Mapping(target = "providerTncHash", expression = "java(pDAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString())")
    @Mapping(target = "countryCode", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString())")
    @Mapping(target = "city", expression = "java(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString())")
    @Mapping(target = "postalCode", expression = "java(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString())")
    @Mapping(target = "street", expression = "java(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString())")
    @Mapping(target = "countrySubdivisionCode", constant = "DE-BE") // TODO add to form and remove constant
    @Mapping(target = "legalForm", constant = "LLC") // TODO add to form and remove constant
    @Mapping(target = "registrationNumberLeiCode", constant = "894500MQZ65CN32S9A66") // TODO add to form and remove constant
    /*@Mapping(target = "registrationNumberTaxID", constant = "") // TODO add to form and remove constant
    @Mapping(target = "registrationNumberEuid", constant = "") // TODO add to form and remove constant
    @Mapping(target = "registrationNumberEori", constant = "") // TODO add to form and remove constant
    @Mapping(target = "registrationNumberVatID", constant = "") // TODO add to form and remove constant*/
    RegistrationFormContent getRegistrationFormContentFromRegistrationForm(PDAcroForm pDAcroForm);
}
