package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class OrganizationMetadata {
    @Id
    @Setter(AccessLevel.NONE)
    private String orgaId;

    private String mailAddress;

    @Enumerated(EnumType.STRING)
    private MembershipClass membershipClass;

    private boolean active;

    @OneToMany(mappedBy = "orgaMetadata", cascade = CascadeType.ALL,fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<OrganisationConnectorExtension> connectors = new HashSet<>();;

    public OrganizationMetadata() {

    }

    public OrganizationMetadata(String orgaId, String mailAddress, MembershipClass membershipClass, boolean active) {

        this.orgaId = orgaId;
        this.mailAddress = mailAddress;
        this.membershipClass = membershipClass;
        this.active = active;
    }
}