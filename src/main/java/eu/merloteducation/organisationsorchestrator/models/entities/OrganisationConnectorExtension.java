package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

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
    private String connectorEndpoint;
    private String ConnectorAccessToken;
    private List<String> bucketNames;
}
