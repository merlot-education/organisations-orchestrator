package eu.merloteducation.organisationsorchestrator.models.gxfscatalog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantItem {
    private String id;
    private String name;
    private PublicKey publicKey;
    private ParticipantSelfDescription selfDescription;
}
