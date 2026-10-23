package com.mycompany.javafxapplication1;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CryptoServiceTest {

    private static final char[] PASS = "correct horse battery".toCharArray();

    @Test
    void roundTripRestoresOriginalBytes() throws Exception {
        byte[] plain = "Quarterly report — confidential".getBytes(StandardCharsets.UTF_8);
        byte[] enc = CryptoService.encrypt(plain, PASS);
        assertArrayEquals(plain, CryptoService.decrypt(enc, PASS));
    }

    @Test
    void ciphertextDoesNotContainPlaintext() throws Exception {
        byte[] plain = "secret-marker-123".getBytes(StandardCharsets.UTF_8);
        String enc = new String(CryptoService.encrypt(plain, PASS), StandardCharsets.ISO_8859_1);
        assertFalse(enc.contains("secret-marker-123"));
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() throws Exception {
        byte[] plain = "repeat".getBytes(StandardCharsets.UTF_8);
        assertFalse(java.util.Arrays.equals(CryptoService.encrypt(plain, PASS), CryptoService.encrypt(plain, PASS)),
                "a fresh salt and IV must be used for every encryption");
    }

    @Test
    void wrongPassphraseIsRejected() throws Exception {
        byte[] enc = CryptoService.encrypt("data".getBytes(StandardCharsets.UTF_8), PASS);
        assertThrows(CryptoService.DecryptionException.class,
                () -> CryptoService.decrypt(enc, "wrong".toCharArray()));
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        byte[] enc = CryptoService.encrypt("data that matters".getBytes(StandardCharsets.UTF_8), PASS);
        enc[enc.length - 5] ^= 0x01;
        assertThrows(CryptoService.DecryptionException.class, () -> CryptoService.decrypt(enc, PASS));
    }

    @Test
    void emptyContentRoundTrips() throws Exception {
        assertArrayEquals(new byte[0], CryptoService.decrypt(CryptoService.encrypt(new byte[0], PASS), PASS));
    }

    @Test
    void nonEncryptedInputIsRejected() {
        assertThrows(CryptoService.DecryptionException.class,
                () -> CryptoService.decrypt("plain text file".getBytes(StandardCharsets.UTF_8), PASS));
    }

    @Test
    void emptyPassphraseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CryptoService.encrypt(new byte[]{1}, new char[0]));
    }
}
