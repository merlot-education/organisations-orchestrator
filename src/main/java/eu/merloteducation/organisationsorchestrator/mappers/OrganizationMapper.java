package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantSelfDescription;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

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
    void updateSelfDescriptionAsParticipant(MerlotOrganizationCredentialSubject source, @MappingTarget MerlotOrganizationCredentialSubject target);

}
