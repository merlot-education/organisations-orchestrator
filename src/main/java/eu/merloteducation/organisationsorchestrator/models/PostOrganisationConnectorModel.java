package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostOrganisationConnectorModel {
    private String id;
    private String connectorEndpoint;
    private String connectorAccessToken;
    private List<String> bucketNames;
}
