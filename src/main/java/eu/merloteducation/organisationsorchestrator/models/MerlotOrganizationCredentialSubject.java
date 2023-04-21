package eu.merloteducation.organisationsorchestrator.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MerlotOrganizationCredentialSubject {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("merlot:registrationNumber")
    private StringTypeValue registrationNumber;

    @JsonProperty("merlot:legalName")
    private StringTypeValue legalName;

    @JsonProperty("merlot:merlotID")
    private StringTypeValue merlotId;

    @JsonProperty("merlot:orgaName")
    private StringTypeValue orgaName;

    @JsonProperty("merlot:legalAddress")
    private VCard legalAddress;


}
