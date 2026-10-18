package com.example.campusconnectmobile;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForgotPasswordTokenTest {

    @Test
    public void extractToken_returnsBareCodeTrimmed() {
        assertEquals("abc123", ForgotPasswordActivity.extractToken("  abc123 \n"));
    }

    @Test
    public void extractToken_readsTokenFromPastedLink() {
        assertEquals("abc123",
                ForgotPasswordActivity.extractToken("https://x.test/reset-password.html?token=abc123&foo=1"));
        assertEquals("abc123", ForgotPasswordActivity.extractToken("reset-password.html?token=abc123"));
    }

    @Test
    public void extractToken_handlesNullAndEmpty() {
        assertEquals("", ForgotPasswordActivity.extractToken(null));
        assertEquals("", ForgotPasswordActivity.extractToken(" "));
    }
}
