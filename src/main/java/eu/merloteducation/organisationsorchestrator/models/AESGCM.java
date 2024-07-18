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

import org.bouncycastle.crypto.CryptoException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Implement AES-GCM encryption.
 *
 * Based on: <a href="https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9">this</a>
 * and <a href="https://gist.github.com/peacefixation/fd47d516aabbc3362561938c85546a17">this</a>
 *
 */
public class AESGCM {

    public static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AUTH_TAG_LENGTH = 128;

    private AESGCM() {
    }

    public static String encrypt(String plainText, SecretKey secretKey) throws CryptoException {
        // create a new IV for this encryption
        byte[] iv = new byte[12];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        // create a cipher instance
        final Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, secretKey, iv);

        // convert the plainText to bytes
        byte[] plainTextBytes;
        plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);

        // encrypt the plainText
        byte[] cipherText;
        try {
            cipherText = cipher.doFinal(plainTextBytes);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new CryptoException("Could not encrypt plainText", ex);
        }

        // store the length of the IV, the IV and the cipherText together
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        // encode as Base64 to return a string representation
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }


    public static String decrypt(String cipherMessage, SecretKey secretKey) throws CryptoException {
        // decode from Base64 for the byte representation
        byte[] decoded = Base64.getDecoder().decode(cipherMessage);

        // check the length of the IV
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        int ivLength = byteBuffer.getInt();
        if(ivLength < 12 || ivLength >= 16) {
            throw new CryptoException("invalid IV length", null);
        }

        // read the IV
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);

        // read the cipherText
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // create a cipher instance
        final Cipher cipher = createCipher(Cipher.DECRYPT_MODE, secretKey, iv);

        // decrypt the cipherText
        byte[] plainTextBytes;
        try {
            plainTextBytes = cipher.doFinal(cipherText);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new CryptoException("Could not decrypt cipherText", ex);
        }

        // get the string representation
        return new String(plainTextBytes, StandardCharsets.UTF_8);
    }

    private static Cipher createCipher(int cipherMode, SecretKey secretKey, byte[] iv) throws CryptoException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new CryptoException("Could not create cipher instance", ex);
        }

        // initialise the cipher
        try {
            cipher.init(cipherMode, secretKey, new GCMParameterSpec(AUTH_TAG_LENGTH, iv));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new CryptoException("Could not initialise cipher", ex);
        }

        return cipher;
    }

}