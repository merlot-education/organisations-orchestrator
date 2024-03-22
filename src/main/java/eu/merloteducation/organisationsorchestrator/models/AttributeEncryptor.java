package eu.merloteducation.organisationsorchestrator.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bouncycastle.crypto.CryptoException;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private final SecretKey key;

    public AttributeEncryptor(@Value("${db.encryption.key}") String encryptionKey) {
        key = new SecretKeySpec(encryptionKey.getBytes(), AESGCM.ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if(attribute == null) {
            return null;
        }

        try {
            return AESGCM.encrypt(attribute, key);
        } catch(CryptoException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if(dbData == null) {
            return null;
        }

        try {
            return AESGCM.decrypt(dbData, key);
        } catch(CryptoException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
