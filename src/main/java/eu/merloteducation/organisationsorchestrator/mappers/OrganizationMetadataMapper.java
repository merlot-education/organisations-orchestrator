package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.models.entities.MembershipClass;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMetadataMapper {
    @Mapping(target = "orgaId", source = "merlotId")
    MerlotParticipantMetaDto organizationMetadataToMerlotParticipantMetaDto(OrganizationMetadata metadata);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass", qualifiedByName = "membershipClass")
    void updateOrganizationMetadataWithMerlotParticipantMetaDto(MerlotParticipantMetaDto source, @MappingTarget OrganizationMetadata target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    void updateMerlotParticipantMetaDtoAsParticipant(MerlotParticipantMetaDto source, @MappingTarget MerlotParticipantMetaDto target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "mailAddress", source = "mailAddress")
    @Mapping(target = "membershipClass", source = "membershipClass")
    void updateMerlotParticipantMetaDtoAsFedAdmin(MerlotParticipantMetaDto source, @MappingTarget MerlotParticipantMetaDto target);

    @Named("membershipClass")
    default MembershipClass membershipClassMapper(String membershipClassString) {

        return MembershipClass.valueOf(membershipClassString.strip().toUpperCase());
    }
}
