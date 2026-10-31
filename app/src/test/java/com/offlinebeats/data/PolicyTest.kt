package com.offlinebeats.data

import org.junit.Assert.*
import org.junit.Test

class PolicyTest {
    @Test fun directAudioAllowed() {
        assertEquals(LinkKind.DIRECT_AUDIO, LinkRules.classify("https://example.org/music/a.mp3?x=1"))
        assertEquals(LinkKind.DIRECT_AUDIO, LinkRules.classify("https://example.org/a.flac"))
    }
    @Test fun videoSitesNotRipped() {
        assertEquals(LinkKind.YOUTUBE, LinkRules.classify("https://music.youtube.com/watch?v=123"))
        assertEquals(LinkKind.YOUTUBE, LinkRules.classify("https://youtu.be/123"))
        assertEquals(LinkKind.WEB_PAGE, LinkRules.classify("https://open.spotify.com/track/123"))
    }
    @Test fun rejectInsecureAndUnclearLinks() {
        assertEquals(LinkKind.INVALID, LinkRules.classify("http://example.org/a.mp3"))
        assertEquals(LinkKind.INVALID, LinkRules.classify("file:///some.mp3"))
        assertEquals(LinkKind.WEB_PAGE, LinkRules.classify("https://example.org/audio"))
    }
    @Test fun permittedCommonsLicenses() {
        assertTrue(LicenseRules.allowed("CC BY 4.0",
            "https://creativecommons.org/licenses/by/4.0/", "true"))
        assertTrue(LicenseRules.allowed("CC BY-SA 3.0",
            "https://creativecommons.org/licenses/by-sa/3.0/", "true"))
        assertTrue(LicenseRules.allowed("CC0",
            "https://creativecommons.org/publicdomain/zero/1.0/", "false"))
        assertTrue(LicenseRules.allowed("Public domain", "", "false"))
    }
    @Test fun rejectRestrictedAndUnknownLicenses() {
        assertFalse(LicenseRules.allowed("CC BY-NC 4.0",
            "https://creativecommons.org/licenses/by-nc/4.0/", "true"))
        assertFalse(LicenseRules.allowed("Unknown", "", "false"))
        assertFalse(LicenseRules.allowed("Public domain", "", "true"))
        assertFalse(LicenseRules.allowed("CC BY", "https://evil.example/licenses/by/4.0", "false"))
    }
    @Test fun rejectUrlsWithCredentialsOrNonHttpSchemes() {
        assertEquals(LinkKind.INVALID, LinkRules.classify("https://user:pass@example.org/a.mp3"))
        assertEquals(LinkKind.INVALID, LinkRules.classify("ftp://example.org/a.mp3"))
        assertEquals(LinkKind.YOUTUBE, LinkRules.classify("https://www.youtube.com/watch?v=abc"))
    }
}
