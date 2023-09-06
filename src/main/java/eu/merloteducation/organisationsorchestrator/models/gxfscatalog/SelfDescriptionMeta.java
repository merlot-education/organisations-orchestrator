package eu.merloteducation.organisationsorchestrator.models.gxfscatalog;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelfDescriptionMeta {
    private String expirationTime;
    private ParticipantSelfDescription content;
    private String subjectId;
    private List<String> validators;
    private String sdHash;
    private String id;
    private String status;
    private String issuer;
    private List<String> validatorDids;
    private String uploadDatetime;
    private String statusDatetime;
}
