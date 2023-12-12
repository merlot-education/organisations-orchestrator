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
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Service
public class GXFSSignerService {

    private final Logger logger = LoggerFactory.getLogger(GXFSSignerService.class);
    private final PrivateKey prk;
    private final List<X509Certificate> certs;

    public GXFSSignerService(@Value("${gxfscatalog.cert-path}") String certPath,
                             @Value("${gxfscatalog.private-key-path}") String privateKeyPath) throws IOException, CertificateException {
        try (InputStream privateKeyStream = new FileInputStream(privateKeyPath)) {
            PEMParser pemParser = new PEMParser(new InputStreamReader(privateKeyStream));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());

            prk = converter.getPrivateKey(privateKeyInfo);
        }

        //---extract Expiration Date--- https://stackoverflow.com/a/11621488
        try (InputStream publicKeyStream = new FileInputStream(certPath)) {
            String certString = new String(publicKeyStream.readAllBytes(), StandardCharsets.UTF_8);
            ByteArrayInputStream certStream = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);
        }
    }

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

        logger.debug("Signing VC");
        LdProof vcProof = sign(vc);
        check(vc, vcProof);
        logger.debug("Signed");

        vc.setJsonObjectKeyValue("proof", vc.getLdProof().getJsonObject());
        vp.setJsonObjectKeyValue("verifiableCredential", vc.getJsonObject());

        logger.debug("Signing VP");
        LdProof vpProof = sign(vp);
        check(vp, vpProof);
        logger.debug("Signed");

        vp.setJsonObjectKeyValue("proof", vp.getLdProof().getJsonObject());

        return vp.toString();
    }

    private LdProof sign(JsonLDObject credential) throws IOException, GeneralSecurityException, JsonLDException {
        KeyPair kp = new KeyPair(null, prk);
        PrivateKeySigner<?> privateKeySigner = new RSA_PS256_PrivateKeySigner(kp);

        JsonWebSignature2020LdSigner signer = new JsonWebSignature2020LdSigner(privateKeySigner);

        signer.setCreated(new Date());
        signer.setProofPurpose(LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD);
        signer.setVerificationMethod(URI.create("did:web:merlot-education.eu"));

        return signer.sign(credential);
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
    private void check(JsonLDObject credential, LdProof proof) throws IOException, GeneralSecurityException, JsonLDException {
        for (X509Certificate cert : certs) {
            PublicKey puk = cert.getPublicKey();
            PublicKeyVerifier<?> pkVerifier = new RSA_PS256_PublicKeyVerifier((RSAPublicKey) puk);
            JsonWebSignature2020LdVerifier verifier = new JsonWebSignature2020LdVerifier(pkVerifier);
            verifier.verify(credential, proof);
        }
    }
}
