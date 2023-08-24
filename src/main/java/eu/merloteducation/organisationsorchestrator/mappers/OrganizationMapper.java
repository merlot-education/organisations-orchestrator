package eu.merloteducation.organisationsorchestrator.mappers;

import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantSelfDescription;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "selfDescription", source = "selfDescription")
    MerlotParticipantDto selfDescriptionToMerlotParticipantDto(ParticipantSelfDescription selfDescription);

}
