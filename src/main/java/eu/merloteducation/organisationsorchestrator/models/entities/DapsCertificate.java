package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.organisationsorchestrator.models.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DapsCertificate {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String clientName;

    @NotNull
    private String clientId;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String keystore;

    @NotNull
    @Convert(converter = AttributeEncryptor.class)
    private String password;

    @NotNull
    private String scope;
}
