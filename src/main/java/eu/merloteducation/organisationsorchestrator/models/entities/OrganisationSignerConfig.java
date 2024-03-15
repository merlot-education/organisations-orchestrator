package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.organisationsorchestrator.models.AttributeEncryptor;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    private String privateKey;

    @NotNull
    private String verificationMethod;

}
