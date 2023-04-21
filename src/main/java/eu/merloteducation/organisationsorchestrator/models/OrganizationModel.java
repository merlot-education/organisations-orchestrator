package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;

@Getter
public class OrganizationModel {
    private String id;
    private String merlotId;
    private String organizationName;
    private String organizationLegalName;
    private String registrationNumber;
    private String termsAndConditionsLink;

    private AddressModel legalAddress;


    public OrganizationModel(ParticipantItem participantItem) {
        this.id = participantItem.getId();
        MerlotOrganizationCredentialSubject sub = participantItem.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        this.merlotId = sub.getMerlotId();
        this.organizationName = sub.getOrgaName();
        this.organizationLegalName = sub.getLegalName();
        this.registrationNumber = sub.getRegistrationNumber();
        this.termsAndConditionsLink = sub.getTermsAndConditionsLink();
        this.legalAddress = new AddressModel(sub.getLegalAddress(), sub.getAddressCode());
    }
}
