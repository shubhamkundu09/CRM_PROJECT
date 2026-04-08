package com.crm.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {

    private static final String SECRET = "A1b2C3d4E5f6G7h8"; // 16 bytes for AES-128
    private static final String ALGORITHM = "AES";

    private static SecretKeySpec getKey() {
        return new SecretKeySpec(SECRET.getBytes(), ALGORITHM);
    }

    public static String encrypt(Long id) {
        if (id == null) return null;
        return encrypt(String.valueOf(id));
    }

    public static String encrypt(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public static Long decryptToLong(String encryptedData) {
        String decrypted = decrypt(encryptedData);
        if (decrypted == null) return null;
        try {
            return Long.parseLong(decrypted);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid encrypted ID format", e);
        }
    }

    public static String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decoded = Base64.getUrlDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}