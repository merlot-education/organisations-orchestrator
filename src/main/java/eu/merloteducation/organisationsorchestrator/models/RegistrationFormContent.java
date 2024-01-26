package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationFormContent {
    private String organizationName;
    private String organizationLegalName;
    private String mailAddress;
    private String street;
    private String city;
    private String postalCode;
    private String countryCode;
    private String providerTncLink;
    private String providerTncHash;
    private String registrationNumberLocal;
}
