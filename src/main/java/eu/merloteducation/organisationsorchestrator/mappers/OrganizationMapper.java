package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.modelslib.api.organization.*;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.entities.*;
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
    @Mapping(target = "orgaId", source = "content.didWeb", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE )
    @Mapping(target = "membershipClass", constant = "PARTICIPANT")
    @Mapping(target = "active", constant = "true")
    MerlotParticipantMetaDto getOrganizationMetadataFromRegistrationForm(RegistrationFormContent content);

    @Mapping(target = "privateKey", source = "privateKey")
    @Mapping(target = "verificationMethod", source = "verificationMethod")
    OrganisationSignerConfigDto getSignerConfigDtoFromDidPrivateKeyDto(ParticipantDidPrivateKeyDto prk);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", source = "connectors", qualifiedByName = "connectorsForDto")
    @Mapping(target = "organisationSignerConfigDto", source = "organisationSignerConfig")
    MerlotParticipantMetaDto organizationMetadataToMerlotParticipantMetaDto(OrganizationMetadata metadata);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", expression = "java(connectorsEntityMapper(metadataDto.getConnectors(), metadataDto.getOrgaId()))")
    @Mapping(target = "organisationSignerConfig", source = "organisationSignerConfigDto")
    OrganizationMetadata merlotParticipantMetaDtoToOrganizationMetadata(MerlotParticipantMetaDto metadataDto);

    @Mapping(target = "privateKey", source = "privateKey")
    @Mapping(target = "verificationMethod", source = "verificationMethod")
    OrganisationSignerConfig organisationSignerConfigDtoToOrganisationSignerConfig(OrganisationSignerConfigDto signerConfigDto);

    default void updateOrganizationMetadataWithMerlotParticipantMetaDto(MerlotParticipantMetaDto source,
        @MappingTarget OrganizationMetadata target) {

        target.setMailAddress(source.getMailAddress());
        target.setMembershipClass(source.getMembershipClass());
        target.setActive(source.isActive());

        Set<OrganisationConnectorExtension> updatedConnectors = connectorsEntityMapper(source.getConnectors(),
            target.getOrgaId());

        target.getConnectors().clear();
        target.getConnectors().addAll(updatedConnectors);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "connectors", source = "connectors")
    @Mapping(target = "organisationSignerConfigDto", source = "organisationSignerConfigDto")
    void updateMerlotParticipantMetaDtoAsParticipant(MerlotParticipantMetaDto source,
        @MappingTarget MerlotParticipantMetaDto target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    void updateMerlotParticipantMetaDtoAsFedAdmin(MerlotParticipantMetaDto source,
        @MappingTarget MerlotParticipantMetaDto target);

    OrganizationConnectorDto connectorExtensionToOrganizationConnectorDto(OrganisationConnectorExtension extension);

    OrganizationConnectorTransferDto connectorExtensionToOrganizationConnectorTransferDto(OrganisationConnectorExtension extension);

    default OrganisationConnectorExtension organizationConnectorDtoToConnectorExtension(OrganizationConnectorDto dto,
        String orgaId) {

        OrganisationConnectorExtension connector = new OrganisationConnectorExtension();
        connector.setOrgaId(orgaId);
        connector.setConnectorId(dto.getConnectorId());
        connector.setConnectorEndpoint(dto.getConnectorEndpoint());
        connector.setConnectorAccessToken(dto.getConnectorAccessToken());
        connector.setIonosS3ExtensionConfig(
                ionosS3ExtensionConfigDtoToIonosS3ExtensionConfig(dto.getIonosS3ExtensionConfig()));

        return connector;
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "buckets", source = "buckets")
    IonosS3ExtensionConfig ionosS3ExtensionConfigDtoToIonosS3ExtensionConfig(IonosS3ExtensionConfigDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "storageEndpoint", source = "storageEndpoint")
    IonosS3Bucket ionosS3BucketDtoToIonosS3Bucket(IonosS3BucketDto dto);

    @Named("connectorsForDto")
    default Set<OrganizationConnectorDto> connectorsDtoMapper(Set<OrganisationConnectorExtension> connectors) {

        return connectors.stream().map(this::connectorExtensionToOrganizationConnectorDto).collect(Collectors.toSet());
    }

    default Set<OrganisationConnectorExtension> connectorsEntityMapper(Set<OrganizationConnectorDto> connectorDtos,
        String orgaId) {

        return connectorDtos.stream()
            .map(connectorDto -> organizationConnectorDtoToConnectorExtension(connectorDto, orgaId))
            .collect(Collectors.toSet());
    }
}
