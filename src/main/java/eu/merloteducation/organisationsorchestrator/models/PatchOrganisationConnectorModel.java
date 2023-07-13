package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatchOrganisationConnectorModel {
    private String connectorEndpoint;
    private String ConnectorAccessToken;
    private List<String> bucketNames;
}