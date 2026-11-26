package com.tuneurlradio.app.security

import android.content.Intent
import android.net.Uri

/**
 * Checks for links and phone numbers that come from the match server
 * (an engagement's `info` field) before the app opens them.
 *
 * The server is trusted, but its data still passes through broadcasts, a
 * database and the network, so the app only opens what it expects:
 * - web pages and images: https only (http is upgraded to https, because
 *   cleartext traffic is blocked for every host but the two legacy APIs);
 * - phone numbers: digits with the usual separators, nothing else.
 */
object SafeLinks {

    private val phoneAllowed = Regex("^\\+?[0-9 ()./-]{3,32}$")

    /** An https URL with a host, or null. */
    fun webUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val candidate = if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.substring("http://".length)
        } else {
            trimmed
        }
        // java.net.URI (not android.net.Uri) so this is testable on the JVM and
        // strict: it rejects spaces and other characters a browser would guess at.
        val uri = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null) return null // no "https://bank.com@evil.example"
        return candidate
    }

    /** True when [url] may be shown inside the app's WebView. */
    fun isAllowedInWebView(url: String?): Boolean = webUrl(url) == url && url != null

    /** A phone number with only digits, a leading +, spaces and ()./- , or null. */
    fun phoneNumber(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (!phoneAllowed.matches(trimmed)) return null
        if (trimmed.count { it.isDigit() } < 3) return null
        return trimmed
    }

    /** ACTION_VIEW for a checked web URL, or null. */
    fun viewIntent(raw: String?): Intent? =
        webUrl(raw)?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }

    /** ACTION_DIAL for a checked number, or null. Never ACTION_CALL. */
    fun dialIntent(raw: String?): Intent? =
        phoneNumber(raw)?.let { Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", it, null)) }

    /** Opens the SMS app for a checked number, or null. */
    fun smsIntent(raw: String?): Intent? =
        phoneNumber(raw)?.let { Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", it, null)) }
}
