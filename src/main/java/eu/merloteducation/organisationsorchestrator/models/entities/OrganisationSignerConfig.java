package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.organisationsorchestrator.models.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class OrganisationSignerConfig {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Convert(converter = AttributeEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String privateKey;

    @NotNull
    private String verificationMethod;

    private String merlotVerificationMethod;

}
