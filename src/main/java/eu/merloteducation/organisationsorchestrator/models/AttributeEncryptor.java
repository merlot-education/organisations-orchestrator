/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
