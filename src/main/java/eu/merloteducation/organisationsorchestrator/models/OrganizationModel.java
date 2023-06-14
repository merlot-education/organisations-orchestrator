package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class OrganizationModel {
    private String id;
    private String merlotId;
    private String organizationName;
    private String organizationLegalName;
    private String registrationNumber;
    private String termsAndConditionsLink;
    private AddressModel legalAddress;
    private String connectorId;
    private String connectorPublicKey;
    private String connectorBaseUrl;


    public OrganizationModel(ParticipantItem participantItem) {
        this.id = participantItem.getId();
        MerlotOrganizationCredentialSubject sub = participantItem.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        this.merlotId = sub.getMerlotId().getValue();
        this.organizationName = sub.getOrgaName().getValue();
        this.organizationLegalName = sub.getLegalName().getValue();
        this.registrationNumber = sub.getRegistrationNumber().getLocal().getValue();
        this.termsAndConditionsLink = sub.getTermsAndConditionsLink().getValue();
        this.legalAddress = new AddressModel(sub.getLegalAddress(), sub.getAddressCode().getValue());
        this.connectorId = sub.getConnectorId().getValue();
        this.connectorPublicKey = sub.getConnectorPublicKey().getValue();
        this.connectorBaseUrl = sub.getConnectorBaseUrl().getValue();
    }
}
