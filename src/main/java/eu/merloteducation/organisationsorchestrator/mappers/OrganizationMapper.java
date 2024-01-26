package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
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

    @Mapping(target = "orgaName", source = "content.organizationName")
    @Mapping(target = "legalName", source = "content.organizationLegalName")
    @Mapping(target = "registrationNumber.local", source = "content.registrationNumberLocal")
    @Mapping(target = "registrationNumber.type", constant = "gax-trust-framework:RegistrationNumber")
    @Mapping(target = "mailAddress", source = "content.mailAddress")
    @Mapping(target = "termsAndConditions.content", source = "content.providerTncLink")
    @Mapping(target = "termsAndConditions.hash", source = "content.providerTncHash")
    @Mapping(target = "termsAndConditions.type", constant = "gax-trust-framework:TermsAndConditions")
    @Mapping(target = "legalAddress.countryName", source = "content.countryCode")
    @Mapping(target = "legalAddress.locality", source = "content.city")
    @Mapping(target = "legalAddress.postalCode", source = "content.postalCode")
    @Mapping(target = "legalAddress.streetAddress", source = "content.street")
    @Mapping(target = "legalAddress.type", constant = "vcard:Address")
    @Mapping(target = "headquarterAddress.countryName", source = "content.countryCode")
    @Mapping(target = "headquarterAddress.locality", source = "content.city")
    @Mapping(target = "headquarterAddress.postalCode", source = "content.postalCode")
    @Mapping(target = "headquarterAddress.streetAddress", source = "content.street")
    @Mapping(target = "headquarterAddress.type", constant = "vcard:Address")
    MerlotOrganizationCredentialSubject getSelfDescriptionFromRegistrationForm(RegistrationFormContent content);
}
