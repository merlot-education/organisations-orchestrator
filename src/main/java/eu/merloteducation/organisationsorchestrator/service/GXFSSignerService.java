package eu.merloteducation.organisationsorchestrator.service;

import com.danubetech.keyformats.crypto.PrivateKeySigner;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PrivateKeySigner;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords;
import info.weboftrust.ldsignatures.signer.JsonWebSignature2020LdSigner;
import info.weboftrust.ldsignatures.signer.LdSigner;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Service
public class GXFSSignerService {


    /**
     * Given a verifiable presentation, sign and return it.
     *
     * @param verifiablePresentationJson presentation to sign
     * @return signed presentation
     * @throws Exception signature/check exception
     */
    public String signVerifiablePresentation(String verifiablePresentationJson) throws Exception {
        Security.addProvider(new BouncyCastleProvider());


        VerifiablePresentation vp = VerifiablePresentation.fromJson(verifiablePresentationJson);
        VerifiableCredential vc = vp.getVerifiableCredential();

        System.out.println("Signing VC");
        LdProof vc_proof = sign(vc);
        check(vc, vc_proof);
        System.out.println("Signed");

        vc.setJsonObjectKeyValue("proof", vc.getLdProof().getJsonObject());
        vp.setJsonObjectKeyValue("verifiableCredential", vc.getJsonObject());

        System.out.println("Signing VC");
        LdProof vp_proof = sign(vp);
        check(vp, vp_proof);
        System.out.println("Signed");

        vp.setJsonObjectKeyValue("proof", vp.getLdProof().getJsonObject());

        return vp.toString();
    }

    private static LdProof sign(JsonLDObject credential) throws IOException, GeneralSecurityException, JsonLDException {
        InputStream privateKeyStream = GXFSSignerService.class.getClassLoader().getResourceAsStream("prk.ss.pem");

        PEMParser pemParser = new PEMParser(new InputStreamReader(privateKeyStream));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());

        PrivateKey prk = converter.getPrivateKey(privateKeyInfo);

        KeyPair kp = new KeyPair(null, prk);
        PrivateKeySigner<?> privateKeySigner = new RSA_PS256_PrivateKeySigner(kp);

        LdSigner signer = new JsonWebSignature2020LdSigner(privateKeySigner);

        signer.setCreated(new Date());
        signer.setProofPurpose(LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD);
        signer.setVerificationMethod(URI.create("did:web:compliance.lab.gaia-x.eu"));

        LdProof ldProof = signer.sign(credential);

        return ldProof;
    }

    /**
     * Given a credential and proof, check if the signature is valid.
     *
     * @param credential credential to check
     * @param proof      proof
     * @throws IOException              IOException
     * @throws GeneralSecurityException GeneralSecurityException
     * @throws JsonLDException          JsonLDException
     */
    public static void check(JsonLDObject credential, LdProof proof) throws IOException, GeneralSecurityException, JsonLDException {
        //---extract Expiration Date--- https://stackoverflow.com/a/11621488
        InputStream publicKeyStream = GXFSSignerService.class.getClassLoader().getResourceAsStream("cert.ss.pem");
        String certString = new String(publicKeyStream.readAllBytes(), StandardCharsets.UTF_8);
        ByteArrayInputStream certStream = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

        for (X509Certificate cert : certs) {

            PublicKey puk = cert.getPublicKey();
            PublicKeyVerifier<?> pkVerifier = new RSA_PS256_PublicKeyVerifier((RSAPublicKey) puk);

            LdVerifier verifier = new JsonWebSignature2020LdVerifier(pkVerifier);

            System.out.println(verifier.verify(credential, proof));
        }
    }

}
