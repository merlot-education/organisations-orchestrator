package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
public class OrganisationConnectorExtension {
    @Id
    private String id;

    private String orgaId;

    private String connectorEndpoint;

    private String connectorAccessToken;

    private List<String> bucketNames;
}
