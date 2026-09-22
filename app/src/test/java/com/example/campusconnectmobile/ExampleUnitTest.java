package com.example.campusconnectmobile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void passwordHashVerifiesOnlyTheOriginalPassword() {
        String hash = PasswordHasher.hash("correct horse battery staple");

        assertTrue(PasswordHasher.verify("correct horse battery staple", hash));
        assertFalse(PasswordHasher.verify("incorrect password", hash));
        assertFalse(PasswordHasher.verify("correct horse battery staple", "not-a-password-hash"));
    }
}