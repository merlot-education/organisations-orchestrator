package eu.merloteducation.organisationsorchestrator.mappers;

import lombok.Getter;

@Getter
public enum DocumentField {
    ORGANIZATIONNAME("OrganizationName"),
    ORGANIZATIONLEGALNAME("OrganizationLegalName"),
    MAILADDRESS("MailAddress"),
    STREET("Street"),
    CITY("City"),
    POSTALCODE("PostalCode"),
    COUNTRYCODE("CountryCode"),
    TNCLINK("ProviderTncLink"),
    TNCHASH("ProviderTncHash"),
    REGISTRATIONNUMBER("RegistrationNumber"),
    DIDWEB("DidWeb");

    private final String value;

    DocumentField(String value){
        this.value = value;
    }

}
