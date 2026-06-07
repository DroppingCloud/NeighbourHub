package com.community.platform.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class EncryptionUtils {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PREFIX = "ENC:";
    private static final byte[] IV = new byte[16];

    public static String encrypt(String plainText, String secret) {
        if (plainText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(secret), new IvParameterSpec(IV));
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public static String decrypt(String cipherText, String secret) {
        if (cipherText == null) {
            return null;
        }
        if (!isEncrypted(cipherText)) {
            return cipherText;
        }
        try {
            String base64 = cipherText.substring(PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(base64);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(secret), new IvParameterSpec(IV));
            byte[] decryptedBytes = cipher.doFinal(decoded);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static SecretKeySpec getSecretKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            byte[] aesKey = new byte[16];
            System.arraycopy(keyBytes, 0, aesKey, 0, aesKey.length);
            return new SecretKeySpec(aesKey, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
}
