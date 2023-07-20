package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectorDetailsRequest {
    String connectorId;
    String orgaId;

    public ConnectorDetailsRequest() {}

    public ConnectorDetailsRequest(String connectorId, String orgaId) {
        this.connectorId = connectorId;
        this.orgaId = orgaId;
    }
}
