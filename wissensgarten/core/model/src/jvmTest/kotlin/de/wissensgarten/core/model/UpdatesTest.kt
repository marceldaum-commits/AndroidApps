package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class UpdatesTest {
    private fun rel(tag: String, apk: String? = "Wissensgarten.apk", draft: Boolean = false) =
        """{"tag_name":"$tag","body":"Neu: $tag","draft":$draft,"prerelease":false,"assets":[${
            apk?.let { """{"name":"$it","browser_download_url":"https://github.com/x/$it","size":123,"extra":1}""" } ?: ""
        }],"html_url":"x"}"""

    @Test fun picksHighestWissensgartenVersion() {
        val json = "[" + listOf(
            rel("wissensgarten-v0.8.2"), rel("wissensgarten-v0.8.10"), rel("mathe-v9.9.9"),
            rel("wissensgarten-v0.9.0", draft = true), rel("wissensgarten-v0.8.11", apk = null),
        ).joinToString(",") + "]"
        val r = Updates.latest(json)!!
        assertEquals("0.8.10", r.version)
        assertEquals("https://github.com/x/Wissensgarten.apk", r.apkUrl)
        assertEquals("Neu: wissensgarten-v0.8.10", r.notes)
    }

    @Test fun comparesNumerically() {
        assertTrue(Updates.isNewer("0.8.10", "0.8.2"))
        assertTrue(Updates.isNewer("0.9", "0.8.9"))
        assertFalse(Updates.isNewer("0.8.2", "0.8.2"))
        assertFalse(Updates.isNewer("0.8.1", "0.8.2"))
    }

    @Test fun emptyListMeansNoUpdate() = assertNull(Updates.latest("[]"))
}

class UpdatesRealResponseTest {
    /** Echte Antwort der GitHub-API (gespeichert), damit Änderungen am Format auffallen. */
    @Test fun parsesRealGithubResponse() {
        val json = javaClass.getResource("/github_releases.json")!!.readText()
        val r = Updates.latest(json)!!
        assertEquals("0.8.2", r.version)
        assertTrue(r.apkUrl.endsWith("Wissensgarten-0.8.2.apk"))
        assertTrue(r.sizeBytes > 1_000_000)
    }
}
