package eu.merloteducation.organisationsorchestrator.models;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;

@Getter
public class AddressModel {
    @JsonView(OrganiationViews.PublicView.class)
    private String countryCode;
    @JsonView(OrganiationViews.PublicView.class)
    private String postalCode;
    @JsonView(OrganiationViews.PublicView.class)
    private String addressCode;
    @JsonView(OrganiationViews.PublicView.class)
    private String city;
    @JsonView(OrganiationViews.PublicView.class)
    private String street;

    public AddressModel(VCard vcard, String addressCode) {
        this.countryCode = vcard.getCountryName().getValue();
        this.postalCode = vcard.getPostalCode().getValue();
        this.addressCode = addressCode;
        this.city = vcard.getLocality().getValue();
        this.street = vcard.getStreetAddress().getValue();
    }
}
