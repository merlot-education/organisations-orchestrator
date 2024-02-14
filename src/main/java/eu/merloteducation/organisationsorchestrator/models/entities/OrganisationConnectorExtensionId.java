package eu.merloteducation.organisationsorchestrator.models.entities;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Setter
public class OrganisationConnectorExtensionId implements Serializable {
    private String orgaId;
    private String connectorId;

    public OrganisationConnectorExtensionId(String orgaId, String connectorId) {
        this.orgaId = orgaId;
        this.connectorId = connectorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganisationConnectorExtensionId that = (OrganisationConnectorExtensionId) o;
        return Objects.equals(orgaId, that.orgaId) &&
            Objects.equals(connectorId, that.connectorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgaId, connectorId);
    }
}
