package eu.merloteducation.organisationsorchestrator.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class MerlotOrganizationCredentialSubject {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("gax-trust-framework:registrationNumber")
    private RegistrationNumber registrationNumber;
    private String legalName;
    private String merlotId;
    private String orgaName;
    private String addressCode;

    private String termsAndConditionsLink;

    @JsonProperty("gax-trust-framework:legalAddress")
    private VCard legalAddress;

    @JsonProperty("gax-trust-framework:headquarterAddress")
    private VCard headquarterAddress;

    @JsonProperty("gax-trust-framework:legalName")
    private void unpackLegalName(Map<String, String> m) {
        legalName = m.get("@value");
    }

    @JsonProperty("merlot:merlotId")
    private void unpackMerlotId(Map<String, String> m) {
        merlotId = m.get("@value");
    }

    @JsonProperty("merlot:orgaName")
    private void unpackOrgaName(Map<String, String> m) {
        orgaName = m.get("@value");
    }

    @JsonProperty("merlot:addressCode")
    private void unpackAddressCode(Map<String, String> m) {
        addressCode = m.get("@value");
    }
    @JsonProperty("merlot:termsConditionsLink")
    private void unpackTermsAndConditionsLink(Map<String, String> m) {
        termsAndConditionsLink = m.get("@value");
    }


}
