package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMetadataMapper {
    MerlotParticipantMetaDto organizationMetadataToMerlotParticipantMetaDto(OrganizationMetadata metadata);

    OrganizationMetadata merlotParticipantMetaDtoToOrganizationMetadata(MerlotParticipantMetaDto metadataDto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    void updateOrganizationMetadataWithMerlotParticipantMetaDto(MerlotParticipantMetaDto source, @MappingTarget OrganizationMetadata target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    void updateMerlotParticipantMetaDtoAsParticipant(MerlotParticipantMetaDto source, @MappingTarget MerlotParticipantMetaDto target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    void updateMerlotParticipantMetaDtoAsFedAdmin(MerlotParticipantMetaDto source, @MappingTarget MerlotParticipantMetaDto target);

}
