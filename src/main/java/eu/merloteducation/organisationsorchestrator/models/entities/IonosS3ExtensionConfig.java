package eu.merloteducation.organisationsorchestrator.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class IonosS3ExtensionConfig {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<IonosS3Bucket> buckets;
}
