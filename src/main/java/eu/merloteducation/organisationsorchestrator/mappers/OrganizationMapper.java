package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.gxfscatalog.participant.ParticipantSelfDescription;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.participant.MerlotOrganizationCredentialSubject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", source = "selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "selfDescription", source = "selfDescription")
    MerlotParticipantDto selfDescriptionToMerlotParticipantDto(ParticipantSelfDescription selfDescription);

    @BeanMapping(ignoreByDefault = true)
    // allow to edit mail
    @Mapping(target = "mailAddress.value", source = "mailAddress.value")
    // allow to edit tnc
    @Mapping(target = "termsAndConditions.content.value", source = "termsAndConditions.content.value")
    @Mapping(target = "termsAndConditions.hash.value", source = "termsAndConditions.hash.value")
    // allow to edit address
    @Mapping(target = "addressCode.value", source = "addressCode.value")
    @Mapping(target = "legalAddress.countryName.value", source = "legalAddress.countryName.value")
    @Mapping(target = "legalAddress.locality.value", source = "legalAddress.locality.value")
    @Mapping(target = "legalAddress.postalCode.value", source = "legalAddress.postalCode.value")
    @Mapping(target = "legalAddress.streetAddress.value", source = "legalAddress.streetAddress.value")
    // copy legal address to headquarter
    @Mapping(target = "headquarterAddress.countryName.value", source = "legalAddress.countryName.value")
    @Mapping(target = "headquarterAddress.locality.value", source = "legalAddress.locality.value")
    @Mapping(target = "headquarterAddress.postalCode.value", source = "legalAddress.postalCode.value")
    @Mapping(target = "headquarterAddress.streetAddress.value", source = "legalAddress.streetAddress.value")
    void updateSelfDescriptionAsParticipant(MerlotOrganizationCredentialSubject source,
                                            @MappingTarget MerlotOrganizationCredentialSubject target);

    @Mapping(target = "orgaName", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString()))")
    @Mapping(target = "legalName", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString()))")
    @Mapping(target = "registrationNumber.local", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER.getValue()).getValueAsString()))")
    @Mapping(target = "registrationNumber.type", constant = "gax-trust-framework:RegistrationNumber")
    @Mapping(target = "mailAddress", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString()))")
    @Mapping(target = "termsAndConditions.content.value", expression = "java(pDAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString())")
    @Mapping(target = "termsAndConditions.content.type", constant = "xsd:anyURI")
    @Mapping(target = "termsAndConditions.hash", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString()))")
    @Mapping(target = "termsAndConditions.type", constant = "gax-trust-framework:TermsAndConditions")
    @Mapping(target = "addressCode", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.ADDRESSCODE.getValue()).getValueAsString()))")
    @Mapping(target = "legalAddress.countryName", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString()))")
    @Mapping(target = "legalAddress.locality", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString()))")
    @Mapping(target = "legalAddress.postalCode", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString()))")
    @Mapping(target = "legalAddress.streetAddress", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString()))")
    @Mapping(target = "legalAddress.type", constant = "vcard:Address")
    @Mapping(target = "headquarterAddress.countryName", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString()))")
    @Mapping(target = "headquarterAddress.locality", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString()))")
    @Mapping(target = "headquarterAddress.postalCode", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString()))")
    @Mapping(target = "headquarterAddress.streetAddress", expression = "java(new StringTypeValue(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString()))")
    @Mapping(target = "headquarterAddress.type", constant = "vcard:Address")
    MerlotOrganizationCredentialSubject getSelfDescriptionFromRegistrationForm(PDAcroForm pDAcroForm);
}
