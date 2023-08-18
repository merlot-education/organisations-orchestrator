package eu.merloteducation.organisationsorchestrator.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicKey {
    private String kty;
    private String e;
    private String alg;
    private String n;
}
