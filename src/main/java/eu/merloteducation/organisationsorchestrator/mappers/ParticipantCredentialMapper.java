package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ParticipantCredentialMapper {

    @Mapping(target = "name", source = "content.organizationName")
    @Mapping(target = "legalAddress.countryCode", source = "content.countryCode")
    @Mapping(target = "legalAddress.countrySubdivisionCode", source = "content.countryCode")
    @Mapping(target = "legalAddress.streetAddress", source = "content.street")
    @Mapping(target = "legalAddress.locality", source = "content.city")
    @Mapping(target = "legalAddress.postalCode", source = "content.postalCode")
    @Mapping(target = "headquarterAddress.countryCode", source = "content.countryCode")
    @Mapping(target = "headquarterAddress.countrySubdivisionCode", source = "content.countryCode")
    @Mapping(target = "headquarterAddress.streetAddress", source = "content.street")
    @Mapping(target = "headquarterAddress.locality", source = "content.city")
    @Mapping(target = "headquarterAddress.postalCode", source = "content.postalCode")
    GxLegalParticipantCredentialSubject getLegalParticipantCsFromRegistrationForm(RegistrationFormContent content);

    // TODO support more registration number types
    @Mapping(target = "leiCode", source = "content.registrationNumberLocal")
    GxLegalRegistrationNumberCredentialSubject getLegalRegistrationNumberFromRegistrationForm(RegistrationFormContent content);

    @Mapping(target = "legalName", source = "content.organizationLegalName")
    @Mapping(target = "legalForm", constant = "LLC") // TODO add to registration form
    @Mapping(target = "termsAndConditions.url", source = "content.providerTncLink")
    @Mapping(target = "termsAndConditions.hash", source = "content.providerTncHash")
    MerlotLegalParticipantCredentialSubject getMerlotParticipantCsFromRegistrationForm(RegistrationFormContent content);

    @BeanMapping(ignoreByDefault = true)
    // participant
    @Mapping(target = "description", source = "description")
    @Mapping(target = "legalAddress", source = "legalAddress")
    @Mapping(target = "headquarterAddress", source = "headquarterAddress")
    void updateCredentialSubjectAsParticipant(GxLegalParticipantCredentialSubject source,
                                              @MappingTarget GxLegalParticipantCredentialSubject target);

    @BeanMapping(ignoreByDefault = true)
    // participant
    @Mapping(target = "termsAndConditions", source = "termsAndConditions")
    void updateCredentialSubjectAsParticipant(MerlotLegalParticipantCredentialSubject source,
                                              @MappingTarget MerlotLegalParticipantCredentialSubject target);


    @BeanMapping(ignoreByDefault = true)
    // fedadmin
    @Mapping(target = "parentOrganization", source = "parentOrganization")
    @Mapping(target = "subOrganization", source = "subOrganization")
    @Mapping(target = "name", source = "name")
    // participant
    @Mapping(target = "description", source = "description")
    @Mapping(target = "legalAddress", source = "legalAddress")
    @Mapping(target = "headquarterAddress", source = "headquarterAddress")
    void updateCredentialSubjectAsFedAdmin(GxLegalParticipantCredentialSubject source,
                                           @MappingTarget GxLegalParticipantCredentialSubject target);

    @BeanMapping(ignoreByDefault = true)
    // fedadmin
    @Mapping(target = "taxID", source = "taxID")
    @Mapping(target = "euid", source = "euid")
    @Mapping(target = "eori", source = "eori")
    @Mapping(target = "vatID", source = "vatID")
    @Mapping(target = "leiCode", source = "leiCode")
    void updateCredentialSubjectAsFedAdmin(GxLegalRegistrationNumberCredentialSubject source,
                                           @MappingTarget GxLegalRegistrationNumberCredentialSubject target);

    @BeanMapping(ignoreByDefault = true)
    // fedadmin
    @Mapping(target = "legalName", source = "legalName")
    @Mapping(target = "legalForm", source = "legalForm")
    // participant
    @Mapping(target = "termsAndConditions", source = "termsAndConditions")
    void updateCredentialSubjectAsFedAdmin(MerlotLegalParticipantCredentialSubject source,
                                           @MappingTarget MerlotLegalParticipantCredentialSubject target);
}
