package eu.merloteducation.organisationsorchestrator.models.gxfscatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MerlotOrganizationCredentialSubject {

    // base fields

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("@context")
    private Map<String, String> context;

    // inherited from gax-trust-framework:LegalPerson
    @JsonProperty("gax-trust-framework:legalName")
    private StringTypeValue legalName;

    @JsonProperty("gax-trust-framework:legalForm")
    private StringTypeValue legalForm;

    @JsonProperty("gax-trust-framework:description")
    private StringTypeValue description;

    @JsonProperty("gax-trust-framework:registrationNumber")
    @NotNull
    private RegistrationNumber registrationNumber;

    @JsonProperty("gax-trust-framework:legalAddress")
    @NotNull
    private VCard legalAddress;

    @JsonProperty("gax-trust-framework:headquarterAddress")
    @NotNull
    private VCard headquarterAddress;

    // inherited from merlot:MerlotOrganization

    @JsonProperty("merlot:orgaName")
    @NotNull
    private StringTypeValue orgaName;

    @JsonProperty("merlot:merlotId")
    @NotNull
    private StringTypeValue merlotId;

    @JsonProperty("merlot:addressCode")
    @NotNull
    private StringTypeValue addressCode;

    @JsonProperty("merlot:mailAddress")
    @NotNull
    private StringTypeValue mailAddress;

    @JsonProperty("merlot:termsAndConditions")
    @NotNull
    private TermsAndConditions termsAndConditions;

}
