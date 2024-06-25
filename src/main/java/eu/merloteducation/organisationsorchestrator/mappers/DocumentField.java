package eu.merloteducation.organisationsorchestrator.mappers;

import lombok.Getter;

@Getter
public enum DocumentField {
    ORGANIZATIONNAME("OrganizationName"),
    ORGANIZATIONLEGALNAME("OrganizationLegalName"),
    LEGALFORM("LegalForm"),
    MAILADDRESS("MailAddress"),
    STREET("Street"),
    CITY("City"),
    POSTALCODE("PostalCode"),
    COUNTRYCODE("CountryCode"),
    COUNTRYSUBDIVISIONCODE("Country Subdivision Code"),
    TNCLINK("ProviderTncLink"),
    TNCHASH("ProviderTncHash"),
    REGISTRATIONNUMBER_EORI("EORI"),
    REGISTRATIONNUMBER_VATID("VatID"),
    REGISTRATIONNUMBER_LEICODE("leiCode");

    private final String value;

    DocumentField(String value){
        this.value = value;
    }

}
