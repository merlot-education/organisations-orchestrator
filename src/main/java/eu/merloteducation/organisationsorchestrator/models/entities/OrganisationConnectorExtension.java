package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.Provider;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
