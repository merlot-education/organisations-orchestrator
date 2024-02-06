package eu.merloteducation.organisationsorchestrator.models.entities;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


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

    public OrganizationMetadata() {

    }

    public OrganizationMetadata(String orgaId, String mailAddress, MembershipClass membershipClass, boolean active) {

        this.orgaId = orgaId;
        this.mailAddress = mailAddress;
        this.membershipClass = membershipClass;
        this.active = active;
    }
}