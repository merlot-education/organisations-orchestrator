package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ParticipantsResponse {
    private int totalCount;
    private List<ParticipantItem> items;
}
