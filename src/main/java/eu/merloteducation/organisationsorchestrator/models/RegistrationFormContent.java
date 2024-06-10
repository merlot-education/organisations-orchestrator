package eu.merloteducation.organisationsorchestrator.models;

import lombok.Data;

@Data
public class RegistrationFormContent {
    private String organizationName;
    private String organizationLegalName;
    private String mailAddress;
    private String street;
    private String city;
    private String postalCode;
    private String countryCode;
    private String countrySubdivisionCode;
    private String providerTncLink;
    private String providerTncHash;
    private String registrationNumberTaxID;
    private String registrationNumberEuid;
    private String registrationNumberEori;
    private String registrationNumberVatID;
    private String registrationNumberLeiCode;
    private String legalForm;
}
