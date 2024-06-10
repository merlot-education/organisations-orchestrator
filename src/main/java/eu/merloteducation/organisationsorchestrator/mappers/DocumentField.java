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
    REGISTRATIONNUMBER_TAXID("RegistrationNumberTaxId"),
    REGISTRATIONNUMBER_EUID("RegistrationNumberEuid"),
    REGISTRATIONNUMBER_EORI("RegistrationNumberEori"),
    REGISTRATIONNUMBER_VATID("RegistrationNumberVatId"),
    REGISTRATIONNUMBER_LEICODE("RegistrationNumberLeiCode");

    private final String value;

    DocumentField(String value){
        this.value = value;
    }

}
