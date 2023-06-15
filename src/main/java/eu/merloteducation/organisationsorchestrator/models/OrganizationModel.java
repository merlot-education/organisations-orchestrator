package eu.merloteducation.organisationsorchestrator.models;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationModel {
    @JsonView(OrganiationViews.PublicView.class)
    private String id;
    @JsonView(OrganiationViews.PublicView.class)
    private String merlotId;
    @JsonView(OrganiationViews.PublicView.class)
    private String organizationName;
    @JsonView(OrganiationViews.PublicView.class)
    private String organizationLegalName;
    @JsonView(OrganiationViews.PublicView.class)
    private String registrationNumber;
    @JsonView(OrganiationViews.PublicView.class)
    private String termsAndConditionsLink;
    @JsonView(OrganiationViews.PublicView.class)
    private AddressModel legalAddress;

    @JsonView(OrganiationViews.InternalView.class)
    private String connectorId;
    @JsonView(OrganiationViews.InternalView.class)
    private String connectorPublicKey;
    @JsonView(OrganiationViews.InternalView.class)
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
