package eu.merloteducation.organisationsorchestrator.models.entities;

import lombok.Getter;

@Getter
public enum MembershipClass {
    FEDERATOR("FEDERATOR"), PARTICIPANT("PARTICIPANT");

    private final String value;

    MembershipClass(String value) {

        this.value = value;
    }
}
