package eu.merloteducation.organisationsorchestrator.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ParticipantSelfDescription {

    @JsonProperty("@id")
    private String id;

    private List<String> type;

    @JsonProperty("@context")
    private List<String> context;

    private MerlotOrganizationVerifiableCredential verifiableCredential;



}
