/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.modelslib.api.organization.*;
import eu.merloteducation.modelslib.daps.OmejdnConnectorCertificateDto;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.entities.*;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "mailAddress", source = "content.mailAddress")
    @Mapping(target = "membershipClass", constant = "PARTICIPANT")
    @Mapping(target = "active", constant = "true")
    MerlotParticipantMetaDto getOrganizationMetadataFromRegistrationForm(RegistrationFormContent content);

    @Mapping(target = "privateKey", source = "privateKey")
    @Mapping(target = "verificationMethod", source = "verificationMethod")
    @Mapping(target = "merlotVerificationMethod", source = "merlotVerificationMethod")
    OrganisationSignerConfigDto getSignerConfigDtoFromDidPrivateKeyDto(ParticipantDidPrivateKeyDto prk);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "clientName", source = "clientName")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "keystore", source = "keystore")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "scope", source = "scope")
    DapsCertificateDto omejdnCertificateToDapsCertificateDto(OmejdnConnectorCertificateDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "agentDid", source = "agentDid")
    ParticipantAgentSettingsDto agentSettingsToAgentSettingsDto(OcmAgentSettings settings);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "agentDid", source = "agentDid")
    OcmAgentSettings agentSettingsDtoToAgentSettings(ParticipantAgentSettingsDto settings);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", expression = "java(connectorsEntityMapper(metadataDto.getConnectors(), metadataDto.getOrgaId()))")
    @Mapping(target = "organisationSignerConfig", source = "organisationSignerConfigDto")
    @Mapping(target = "dapsCertificates", source = "dapsCertificates")
    @Mapping(target = "ocmAgentSettings", source = "ocmAgentSettings")
    OrganizationMetadata merlotParticipantMetaDtoToOrganizationMetadata(MerlotParticipantMetaDto metadataDto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgaId", source = "orgaId")
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "connectors", source = "connectors", qualifiedByName = "connectorsForDto")
    @Mapping(target = "organisationSignerConfigDto", source = "organisationSignerConfig")
    @Mapping(target = "dapsCertificates", source = "dapsCertificates")
    @Mapping(target = "ocmAgentSettings", source = "ocmAgentSettings")
    MerlotParticipantMetaDto organizationMetadataToMerlotParticipantMetaDto(OrganizationMetadata metadata);


    default ParticipantAgentDidsDto agentSettingsSetToDidDto(Set<ParticipantAgentSettingsDto> agentSettings) {
        ParticipantAgentDidsDto dto = new ParticipantAgentDidsDto();
        dto.setAgentDids(agentSettings.stream().map(ParticipantAgentSettingsDto::getAgentDid).collect(Collectors.toSet()));
        return dto;
    }

    @Named("connectorsForDto")
    default Set<OrganizationConnectorDto> connectorsDtoMapper(Set<OrganisationConnectorExtension> connectors) {

        return connectors.stream().map(this::connectorExtensionToOrganizationConnectorDto).collect(Collectors.toSet());
    }

    OrganizationConnectorDto connectorExtensionToOrganizationConnectorDto(OrganisationConnectorExtension extension);

    default Set<OrganisationConnectorExtension> connectorsEntityMapper(Set<OrganizationConnectorDto> connectorDtos,
                                                                       String orgaId) {

        return connectorDtos.stream()
                .map(connectorDto -> organizationConnectorDtoToConnectorExtension(connectorDto, orgaId))
                .collect(Collectors.toSet());
    }

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

    @Mapping(target = "id", source = "metaData.orgaId")
    @Mapping(target = "metadata", source = "metaData")
    @Mapping(target = "selfDescription", source = "selfDescription")
    MerlotParticipantDto selfDescriptionAndMetadataToMerlotParticipantDto(ExtendedVerifiablePresentation selfDescription,
                                                                          MerlotParticipantMetaDto metaData);

    default void updateOrganizationMetadataWithMerlotParticipantMetaDto(MerlotParticipantMetaDto source,
                                                                        @MappingTarget OrganizationMetadata target) {
        target.setMailAddress(source.getMailAddress());
        target.setMembershipClass(source.getMembershipClass());
        target.setActive(source.isActive());

        Set<OrganisationConnectorExtension> updatedConnectors = connectorsEntityMapper(source.getConnectors(),
                target.getOrgaId());

        target.getConnectors().clear();
        target.getConnectors().addAll(updatedConnectors);
        target.setOrganisationSignerConfig(
                organisationSignerConfigDtoToOrganisationSignerConfig(source.getOrganisationSignerConfigDto()));
        target.getOcmAgentSettings().clear();
        if (source.getOcmAgentSettings() != null) {
            target.getOcmAgentSettings().addAll(source.getOcmAgentSettings()
                    .stream().map(this::agentSettingsDtoToAgentSettings).collect(Collectors.toSet()));
        }
    }

    @Mapping(target = "privateKey", source = "privateKey")
    @Mapping(target = "verificationMethod", source = "verificationMethod")
    @Mapping(target = "merlotVerificationMethod", source = "merlotVerificationMethod")
    OrganisationSignerConfig organisationSignerConfigDtoToOrganisationSignerConfig(OrganisationSignerConfigDto signerConfigDto);

    OrganizationConnectorTransferDto connectorExtensionToOrganizationConnectorTransferDto(OrganisationConnectorExtension extension);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "connectors", source = "connectors")
    @Mapping(target = "ocmAgentSettings", source = "ocmAgentSettings")
    void updateMerlotParticipantMetaDtoAsParticipant(MerlotParticipantMetaDto source,
                                                     @MappingTarget MerlotParticipantMetaDto target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "ocmAgentSettings", source = "ocmAgentSettings")
    void updateMerlotParticipantMetaDtoAsFedAdmin(MerlotParticipantMetaDto source,
                                                  @MappingTarget MerlotParticipantMetaDto target);


}
