package kr.ac.yumin.playlist.security;

import java.security.*;
import java.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 600000;

    private PasswordHasher() {}

    public static String hash(char[] password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2_sha256$"
                + ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(char[] password, String encoded) {
        if (encoded == null) return false;
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256")) return false;
            int count = Integer.parseInt(parts[1]);
            if (count < 100000 || count > 2000000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]),
                    expected = Base64.getDecoder().decode(parts[3]);
            return salt.length == 16
                    && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(password, salt, count));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int count) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, count, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 암호화 기능을 사용할 수 없습니다.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
