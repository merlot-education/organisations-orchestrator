package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", source = "selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "metadata", source = "metaData")
    @Mapping(target = "selfDescription", source = "selfDescription")
    MerlotParticipantDto selfDescriptionAndMetadataToMerlotParticipantDto(SelfDescription selfDescription,
        MerlotParticipantMetaDto metaData);

    @BeanMapping(ignoreByDefault = true)
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

    @Mapping(target = "mailAddress", source = "content.mailAddress")
    @Mapping(target = "membershipClass", constant = "PARTICIPANT")
    @Mapping(target = "active", constant = "true")
    MerlotParticipantMetaDto getOrganizationMetadataFromRegistrationForm(RegistrationFormContent content);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", source = "connectors", qualifiedByName = "connectorsForDto")
    MerlotParticipantMetaDto organizationMetadataToMerlotParticipantMetaDto(OrganizationMetadata metadata);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", source = "connectors", qualifiedByName = "connectorsForEntity")
    OrganizationMetadata merlotParticipantMetaDtoToOrganizationMetadata(MerlotParticipantMetaDto metadataDto);

    default void updateOrganizationMetadataWithMerlotParticipantMetaDto(MerlotParticipantMetaDto source,
        @MappingTarget OrganizationMetadata target) {

        target.setMailAddress(source.getMailAddress());
        target.setMembershipClass(source.getMembershipClass());
        target.setActive(source.isActive());

        Set<OrganisationConnectorExtension> updatedConnectors = connectorsEntityMapper(source.getConnectors());
        // rectify if for some reason there was an inconsistency
        updatedConnectors.forEach(con -> con.setOrgaId(target.getOrgaId()));

        target.getConnectors().clear();
        target.getConnectors().addAll(updatedConnectors);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "connectors", source = "connectors")
    void updateMerlotParticipantMetaDtoAsParticipant(MerlotParticipantMetaDto source,
        @MappingTarget MerlotParticipantMetaDto target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    void updateMerlotParticipantMetaDtoAsFedAdmin(MerlotParticipantMetaDto source,
        @MappingTarget MerlotParticipantMetaDto target);

    OrganizationConnectorDto connectorExtensionToOrganizationConnectorDto(OrganisationConnectorExtension extension);

    OrganisationConnectorExtension organizationConnectorDtoToConnectorExtension(OrganizationConnectorDto dto);

    @Named("connectorsForDto")
    default Set<OrganizationConnectorDto> connectorsDtoMapper(Set<OrganisationConnectorExtension> connectors) {

        return connectors.stream().map(this::connectorExtensionToOrganizationConnectorDto).collect(Collectors.toSet());
    }

    @Named("connectorsForEntity")
    default Set<OrganisationConnectorExtension> connectorsEntityMapper(Set<OrganizationConnectorDto> connectors) {

        return connectors.stream().map(this::organizationConnectorDtoToConnectorExtension).collect(Collectors.toSet());
    }
}
