package eu.merloteducation.organisationsorchestrator.models.messagequeue;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConnectorDetailsRequest {
    String connectorId;
    String orgaId;

    public ConnectorDetailsRequest(String connectorId, String orgaId) {
        this.connectorId = connectorId;
        this.orgaId = orgaId;
    }
}
