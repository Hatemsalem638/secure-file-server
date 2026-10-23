package com.mycompany.javafxapplication1;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticated file encryption using AES-256-GCM.
 *
 * <p>The AES key is derived from the user's passphrase with PBKDF2-HMAC-SHA256
 * and a random per-file salt, and every encryption uses a fresh random IV.
 * GCM authenticates the ciphertext, so a wrong passphrase or any tampering
 * with the file is detected on decryption instead of producing garbage.</p>
 *
 * <p>Encrypted file layout:
 * {@code MAGIC(4) | VERSION(1) | SALT(16) | IV(12) | CIPHERTEXT + GCM TAG(16)}</p>
 */
public final class CryptoService {

    private static final byte[] MAGIC = {'S', 'F', 'S', 'E'};
    private static final byte VERSION = 1;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int HEADER_BYTES = MAGIC.length + 1 + SALT_BYTES + IV_BYTES;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoService() {
    }

    /** Thrown when decryption fails because the passphrase is wrong or the data was modified. */
    public static class DecryptionException extends Exception {
        public DecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static byte[] encrypt(byte[] plaintext, char[] passphrase) throws GeneralSecurityException {
        requirePassphrase(passphrase);
        byte[] salt = randomBytes(SALT_BYTES);
        byte[] iv = randomBytes(IV_BYTES);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(header(salt, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        return ByteBuffer.allocate(HEADER_BYTES + ciphertext.length)
                .put(header(salt, iv))
                .put(ciphertext)
                .array();
    }

    public static byte[] decrypt(byte[] encrypted, char[] passphrase) throws DecryptionException {
        requirePassphrase(passphrase);
        if (!isEncrypted(encrypted)) {
            throw new DecryptionException("This file was not encrypted by Secure File Server.", null);
        }
        byte[] salt = Arrays.copyOfRange(encrypted, MAGIC.length + 1, MAGIC.length + 1 + SALT_BYTES);
        byte[] iv = Arrays.copyOfRange(encrypted, MAGIC.length + 1 + SALT_BYTES, HEADER_BYTES);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(header(salt, iv));
            return cipher.doFinal(encrypted, HEADER_BYTES, encrypted.length - HEADER_BYTES);
        } catch (AEADBadTagException e) {
            throw new DecryptionException("Wrong passphrase, or the file has been modified.", e);
        } catch (GeneralSecurityException e) {
            throw new DecryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /** True if the bytes start with this application's encrypted-file header. */
    public static boolean isEncrypted(byte[] data) {
        if (data == null || data.length < HEADER_BYTES + TAG_BITS / 8) {
            return false;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) {
                return false;
            }
        }
        return data[MAGIC.length] == VERSION;
    }

    private static SecretKeySpec deriveKey(char[] passphrase, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] header(byte[] salt, byte[] iv) {
        return ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).put(VERSION).put(salt).put(iv).array();
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    private static void requirePassphrase(char[] passphrase) {
        if (passphrase == null || passphrase.length == 0) {
            throw new IllegalArgumentException("A passphrase is required.");
        }
    }
}
