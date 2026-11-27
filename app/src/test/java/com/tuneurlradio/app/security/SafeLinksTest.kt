package com.tuneurlradio.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLinksTest {

    @Test fun httpsUrlIsKept() {
        assertEquals("https://www.ksal.com/page?a=1", SafeLinks.webUrl("https://www.ksal.com/page?a=1"))
    }

    @Test fun httpIsUpgradedToHttps() {
        assertEquals("https://example.com/x", SafeLinks.webUrl("http://example.com/x"))
        assertEquals("https://example.com/x", SafeLinks.webUrl("  HTTP://example.com/x "))
    }

    @Test fun otherSchemesAreRejected() {
        listOf(
            "javascript:alert(1)",
            "file:///data/data/com.tuneurlradio.app/databases/tuneurlradio.db",
            "intent://scan/#Intent;scheme=zxing;end",
            "content://com.example/secret",
            "market://details?id=x",
            "tel:123",
            "example.com",
            "",
            null
        ).forEach { assertNull(it, SafeLinks.webUrl(it)) }
    }

    @Test fun urlWithUserInfoOrNoHostIsRejected() {
        assertNull(SafeLinks.webUrl("https://bank.example@evil.example/"))
        assertNull(SafeLinks.webUrl("https:///path-only"))
        assertNull(SafeLinks.webUrl("https://exa mple.com/"))
    }

    @Test fun webViewAllowsOnlyHttpsAsIs() {
        assertTrue(SafeLinks.isAllowedInWebView("https://example.com/"))
        assertFalse(SafeLinks.isAllowedInWebView("http://example.com/"))
        assertFalse(SafeLinks.isAllowedInWebView("intent://x#Intent;end"))
        assertFalse(SafeLinks.isAllowedInWebView(null))
    }

    @Test fun phoneNumbersWithSeparatorsAreKept() {
        assertEquals("+1 (785) 823-1111", SafeLinks.phoneNumber("+1 (785) 823-1111"))
        assertEquals("785.823.1111", SafeLinks.phoneNumber(" 785.823.1111 "))
        assertEquals("911", SafeLinks.phoneNumber("911"))
    }

    @Test fun phoneNumbersWithOtherContentAreRejected() {
        listOf(
            "123;ussd*#06#",
            "*#06#",
            "12",
            "785-823-1111?body=hello",
            "https://example.com",
            "",
            null
        ).forEach { assertNull(it, SafeLinks.phoneNumber(it)) }
    }
}
