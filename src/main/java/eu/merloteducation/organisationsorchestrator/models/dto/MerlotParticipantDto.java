package eu.merloteducation.organisationsorchestrator.models.dto;

import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.ParticipantSelfDescription;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MerlotParticipantDto {
    private MerlotParticipantMetaDto metadata;
    private ParticipantSelfDescription selfDescription;
}
