package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrganizationMetadata {
    @Id
    private String orgaId;

    private String mailAddress;

    @Enumerated(EnumType.STRING)
    private MembershipClass membershipClass;

    private boolean active;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "orgaId", referencedColumnName = "orgaId", updatable = false)
    private Set<OrganisationConnectorExtension> connectors = new HashSet<>();;

    public OrganizationMetadata(String orgaId, String mailAddress, MembershipClass membershipClass, boolean active) {

        this.orgaId = orgaId;
        this.mailAddress = mailAddress;
        this.membershipClass = membershipClass;
        this.active = active;
    }
}