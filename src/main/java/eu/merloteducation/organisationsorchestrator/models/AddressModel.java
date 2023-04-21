package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;

@Getter
public class AddressModel {
    private String countryCode;
    private String postalCode;
    private String addressCode;
    private String city;
    private String street;

    public AddressModel(VCard vcard, String addressCode) {
        this.countryCode = vcard.getCountryName().getValue();
        this.postalCode = vcard.getPostalCode().getValue();
        this.addressCode = addressCode;
        this.city = vcard.getLocality().getValue();
        this.street = vcard.getStreetAddress().getValue();
    }
}
