package eu.merloteducation.organisationsorchestrator.models.entities;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@IdClass(OrganisationConnectorExtensionId.class)
@Data
@NoArgsConstructor
public class OrganisationConnectorExtension {
    @Id
    private String orgaId;

    @Id
    private String connectorId;

    @NotNull
    private String connectorEndpoint;

    @NotNull
    private String connectorAccessToken;

    @Nullable
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private IonosS3ExtensionConfig ionosS3ExtensionConfig;

}
