package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Entity
@IdClass(OrganisationConnectorExtensionId.class)
@Getter
@Setter
@NoArgsConstructor
public class OrganisationConnectorExtension {
    @Id
    private String orgaId;

    @Id
    private String connectorId;

    private String connectorEndpoint;

    private String connectorAccessToken;

    private List<String> bucketNames;

}
