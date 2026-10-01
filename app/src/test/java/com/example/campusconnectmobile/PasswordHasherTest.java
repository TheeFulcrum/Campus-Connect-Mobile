package com.example.campusconnectmobile;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void testHashGeneratesValidFormat() {
        String password = "CampusPassword123!";
        String hashedPassword = PasswordHasher.hash(password);

        assertNotNull(hashedPassword);
        assertTrue("Hash should start with pbkdf2-sha256", hashedPassword.startsWith("pbkdf2-sha256$"));
    }

    @Test
    public void testVerifyCorrectPasswordReturnsTrue() {
        String password = "CampusPassword123!";
        String hashedPassword = PasswordHasher.hash(password);

        assertTrue("Verification should succeed for correct password",
                PasswordHasher.verify(password, hashedPassword));
    }

    @Test
    public void testVerifyIncorrectPasswordReturnsFalse() {
        String password = "CampusPassword123!";
        String wrongPassword = "WrongPassword456!";
        String hashedPassword = PasswordHasher.hash(password);

        assertFalse("Verification should fail for incorrect password",
                PasswordHasher.verify(wrongPassword, hashedPassword));
    }

    @Test
    public void testVerifyHandlesMalformedHashGracefully() {
        assertFalse("Null stored hash should return false", PasswordHasher.verify("password", null));
        assertFalse("Null password should return false", PasswordHasher.verify(null, "somehash"));
        assertFalse("Invalid format hash should return false", PasswordHasher.verify("password", "invalid_hash_string"));
        assertFalse("Truncated hash should return false", PasswordHasher.verify("password", "pbkdf2-sha256$100$salt"));
    }

    @Test
    public void testLegacyPlaintextMatching() {
        assertTrue(PasswordHasher.matchesLegacyPlaintext("plainTextPass123", "plainTextPass123"));
        assertFalse(PasswordHasher.matchesLegacyPlaintext("plainTextPass123", "differentPass"));
        assertFalse(PasswordHasher.matchesLegacyPlaintext(null, "plainTextPass123"));
    }
}
