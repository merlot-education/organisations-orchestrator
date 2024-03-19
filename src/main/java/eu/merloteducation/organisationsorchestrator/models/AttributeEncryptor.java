package eu.merloteducation.organisationsorchestrator.models;

import jakarta.persistence.AttributeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private final Key key;
    private final Cipher cipherEncrypt;
    private final Cipher cipherDecrypt;

    public AttributeEncryptor(@Value("${db.encryption.key}") String encryptionKey,
                              @Value("${db.encryption.transformation:AES}") String transformation) // TODO check options
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        key = new SecretKeySpec(encryptionKey.getBytes(), transformation);
        cipherEncrypt = Cipher.getInstance(transformation);
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, key);
        cipherDecrypt = Cipher.getInstance(transformation);
        cipherDecrypt.init(Cipher.DECRYPT_MODE, key);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return Base64.getEncoder().encodeToString(cipherEncrypt.doFinal(attribute.getBytes()));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return new String(cipherDecrypt.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e);
        }
    }
}
