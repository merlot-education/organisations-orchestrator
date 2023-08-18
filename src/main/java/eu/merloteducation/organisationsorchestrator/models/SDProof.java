package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SDProof {
    private String created;
    private String jws;
    private String proofPurpose;
    private String type;
    private String verificationMethod;
}
