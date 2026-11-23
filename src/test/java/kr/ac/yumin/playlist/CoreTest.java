package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.security.PasswordHasher;
import kr.ac.yumin.playlist.service.*;

import org.junit.jupiter.api.Test;

class CoreTest {
    @Test
    void passwordRoundTripAndSalt() {
        String a = PasswordHasher.hash("테스트 암호!".toCharArray()),
                b = PasswordHasher.hash("테스트 암호!".toCharArray());
        assertNotEquals(a, b);
        assertTrue(a.length() <= 255);
        assertTrue(PasswordHasher.verify("테스트 암호!".toCharArray(), a));
        assertFalse(PasswordHasher.verify("틀린 암호".toCharArray(), a));
    }

    @Test
    void demoAndMalformedHashesCannotAuthenticate() {
        for (String hash :
                new String[] {
                    "DEMO_HASH_NOT_FOR_AUTH_01",
                    "pbkdf2_sha256$-1$a$b",
                    "pbkdf2_sha256$99999999$a$b",
                    "garbage",
                    ""
                }) assertFalse(PasswordHasher.verify("anything".toCharArray(), hash));
    }

    @Test
    void oracleCharLimitsUseUnicodeCharacters() {
        assertEquals("가".repeat(30), AuthService.text("가".repeat(30), "아이디", 30, true));
        assertEquals("😀".repeat(30), AuthService.text("😀".repeat(30), "아이디", 30, true));
        assertThrows(AppException.class, () -> AuthService.text("가".repeat(31), "아이디", 30, true));
        assertThrows(AppException.class, () -> AuthService.text("   ", "제목", 150, true));
    }
}
