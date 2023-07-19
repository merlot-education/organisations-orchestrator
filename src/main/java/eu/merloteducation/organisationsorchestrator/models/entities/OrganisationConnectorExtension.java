package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class OrganisationConnectorExtension {
    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    private String orgaId;

    private String connectorId;

    private String connectorEndpoint;

    private String connectorAccessToken;

    private List<String> bucketNames;

    public OrganisationConnectorExtension() {
        this.id = "Connector:" + UUID.randomUUID();
    }
}
