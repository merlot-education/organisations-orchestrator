package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", source = "selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "selfDescription", source = "selfDescription")
    MerlotParticipantDto selfDescriptionToMerlotParticipantDto(SelfDescription selfDescription);

    @BeanMapping(ignoreByDefault = true)
    // allow to edit mail
    @Mapping(target = "mailAddress", source = "mailAddress")
    // allow to edit tnc
    @Mapping(target = "termsAndConditions.content", source = "termsAndConditions.content")
    @Mapping(target = "termsAndConditions.hash", source = "termsAndConditions.hash")
    // allow to edit address
    @Mapping(target = "legalAddress.countryName", source = "legalAddress.countryName")
    @Mapping(target = "legalAddress.locality", source = "legalAddress.locality")
    @Mapping(target = "legalAddress.postalCode", source = "legalAddress.postalCode")
    @Mapping(target = "legalAddress.streetAddress", source = "legalAddress.streetAddress")
    // copy legal address to headquarter
    @Mapping(target = "headquarterAddress.countryName", source = "legalAddress.countryName")
    @Mapping(target = "headquarterAddress.locality", source = "legalAddress.locality")
    @Mapping(target = "headquarterAddress.postalCode", source = "legalAddress.postalCode")
    @Mapping(target = "headquarterAddress.streetAddress", source = "legalAddress.streetAddress")
    void updateSelfDescriptionAsParticipant(MerlotOrganizationCredentialSubject source,
                                            @MappingTarget MerlotOrganizationCredentialSubject target);

    @BeanMapping(ignoreByDefault = true)
    // allow to edit name (orga and legal)
    @Mapping(target = "orgaName", source = "orgaName")
    @Mapping(target = "legalName", source = "legalName")
    // allow to edit registration number (local, euid, eori, vatId and leiCode)
    @Mapping(target = "registrationNumber.local", source = "registrationNumber.local")
    @Mapping(target = "registrationNumber.euid", source = "registrationNumber.euid")
    @Mapping(target = "registrationNumber.eori", source = "registrationNumber.eori")
    @Mapping(target = "registrationNumber.vatId", source = "registrationNumber.vatId")
    @Mapping(target = "registrationNumber.leiCode", source = "registrationNumber.leiCode")
    // allow to edit mail
    @Mapping(target = "mailAddress", source = "mailAddress")
    // allow to edit tnc
    @Mapping(target = "termsAndConditions.content", source = "termsAndConditions.content")
    @Mapping(target = "termsAndConditions.hash", source = "termsAndConditions.hash")
    // allow to edit address
    @Mapping(target = "legalAddress.countryName", source = "legalAddress.countryName")
    @Mapping(target = "legalAddress.locality", source = "legalAddress.locality")
    @Mapping(target = "legalAddress.postalCode", source = "legalAddress.postalCode")
    @Mapping(target = "legalAddress.streetAddress", source = "legalAddress.streetAddress")
    // copy legal address to headquarter
    @Mapping(target = "headquarterAddress.countryName", source = "legalAddress.countryName")
    @Mapping(target = "headquarterAddress.locality", source = "legalAddress.locality")
    @Mapping(target = "headquarterAddress.postalCode", source = "legalAddress.postalCode")
    @Mapping(target = "headquarterAddress.streetAddress", source = "legalAddress.streetAddress")
    void updateSelfDescriptionAsFedAdmin(MerlotOrganizationCredentialSubject source,
        @MappingTarget MerlotOrganizationCredentialSubject target);

    @Mapping(target = "orgaName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONNAME.getValue()).getValueAsString())")
    @Mapping(target = "legalName", expression = "java(pDAcroForm.getField(DocumentField.ORGANIZATIONLEGALNAME.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumber.local", expression = "java(pDAcroForm.getField(DocumentField.REGISTRATIONNUMBER.getValue()).getValueAsString())")
    @Mapping(target = "registrationNumber.type", constant = "gax-trust-framework:RegistrationNumber")
    @Mapping(target = "mailAddress", expression = "java(pDAcroForm.getField(DocumentField.MAILADDRESS.getValue()).getValueAsString())")
    @Mapping(target = "termsAndConditions.content", expression = "java(pDAcroForm.getField(DocumentField.TNCLINK.getValue()).getValueAsString())")
    @Mapping(target = "termsAndConditions.hash", expression = "java(pDAcroForm.getField(DocumentField.TNCHASH.getValue()).getValueAsString())")
    @Mapping(target = "termsAndConditions.type", constant = "gax-trust-framework:TermsAndConditions")
    @Mapping(target = "legalAddress.countryName", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString())")
    @Mapping(target = "legalAddress.locality", expression = "java(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString())")
    @Mapping(target = "legalAddress.postalCode", expression = "java(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString())")
    @Mapping(target = "legalAddress.streetAddress", expression = "java(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString())")
    @Mapping(target = "legalAddress.type", constant = "vcard:Address")
    @Mapping(target = "headquarterAddress.countryName", expression = "java(pDAcroForm.getField(DocumentField.COUNTRYCODE.getValue()).getValueAsString())")
    @Mapping(target = "headquarterAddress.locality", expression = "java(pDAcroForm.getField(DocumentField.CITY.getValue()).getValueAsString())")
    @Mapping(target = "headquarterAddress.postalCode", expression = "java(pDAcroForm.getField(DocumentField.POSTALCODE.getValue()).getValueAsString())")
    @Mapping(target = "headquarterAddress.streetAddress", expression = "java(pDAcroForm.getField(DocumentField.STREET.getValue()).getValueAsString())")
    @Mapping(target = "headquarterAddress.type", constant = "vcard:Address")
    MerlotOrganizationCredentialSubject getSelfDescriptionFromRegistrationForm(PDAcroForm pDAcroForm);
}
