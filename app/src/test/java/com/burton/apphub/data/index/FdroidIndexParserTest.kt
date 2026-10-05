package com.burton.apphub.data.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FdroidIndexParserTest {
    @Test
    fun parseV1BurtonWorkspaces() {
        val parsed = FdroidIndexParser.parse(INDEX_V1, "https://example.invalid/fdroid/repo", "burton")
        assertEquals("Burton Workspaces", parsed.repoName)
        assertEquals("https://burton-workspaces.github.io/burton-app-dist/fdroid/repo", parsed.repoAddress)
        assertEquals(1, parsed.apps.size)
        val app = parsed.apps.single()
        assertEquals("com.burton.sonos", app.packageName)
        assertEquals("Burton Sonos", app.name)
        assertEquals("Unknown", app.license)
        assertEquals(2, app.versions.size)
        assertEquals("1.4.1", app.suggested?.versionName)
        assertEquals(1_004_001L, app.suggested?.versionCode)
        assertEquals(
            "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo/burton-sonos-1.4.1.apk",
            app.suggested?.url,
        )
        assertEquals(
            "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo/icons-640/com.burton.sonos.png",
            app.iconUrl,
        )
        assertTrue(
            app.iconUrls.contains(
                "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo/icons/com.burton.sonos.png",
            ),
        )
    }

    @Test
    fun iconUrlsPreferNamedFileAcrossDensities() {
        val urls = FdroidIndexParser.iconUrls(
            "https://host/fdroid/repo/",
            "com.example.app",
            "com.example.app.4.png",
        )
        assertEquals(
            "https://host/fdroid/repo/icons-640/com.example.app.4.png",
            urls.first(),
        )
        assertTrue(urls.contains("https://host/fdroid/repo/icons-480/com.example.app.4.png"))
        assertTrue(urls.contains("https://host/fdroid/repo/icons-640/com.example.app.png"))
    }

    @Test
    fun parseV2LocalizedName() {
        val parsed = FdroidIndexParser.parse(INDEX_V2, "https://example.invalid/fdroid/repo", "burton")
        assertEquals("Burton Workspaces", parsed.repoName)
        val app = parsed.apps.single()
        assertEquals("Burton Sonos", app.name)
        assertEquals("1.4.1", app.suggested?.versionName)
        assertTrue(app.suggested!!.url.endsWith("/burton-sonos-1.4.1.apk"))
    }

    @Test
    fun stripHtmlAndNormalize() {
        assertEquals("Hello world", FdroidIndexParser.stripHtml("<p>Hello<br/>world</p>").replace('\n', ' ').replace(Regex(" +"), " "))
        assertEquals(
            "https://host/fdroid/repo",
            FdroidIndexParser.normalizeAddress("https://host/fdroid/repo/"),
        )
        assertEquals(
            "https://host/fdroid/repo/app.apk",
            FdroidIndexParser.apkUrl("https://host/fdroid/repo/", "/app.apk"),
        )
    }

    companion object {
        private val INDEX_V1 = """
            {
              "repo": {
                "timestamp": 1790781809000,
                "version": 20002,
                "name": "Burton Workspaces",
                "icon": "icon.png",
                "address": "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo",
                "description": "Apps."
              },
              "apps": [
                {
                  "categories": ["fdroid"],
                  "license": "Unknown",
                  "name": "Burton Sonos",
                  "packageName": "com.burton.sonos"
                }
              ],
              "packages": {
                "com.burton.sonos": [
                  {
                    "apkName": "burton-sonos-1.4.1.apk",
                    "hash": "abc",
                    "size": 1889664,
                    "versionCode": 1004001,
                    "versionName": "1.4.1",
                    "minSdkVersion": 26
                  },
                  {
                    "apkName": "burton-sonos-1.4.0.apk",
                    "hash": "def",
                    "size": 1889664,
                    "versionCode": 1004000,
                    "versionName": "1.4.0",
                    "minSdkVersion": 26
                  }
                ]
              }
            }
        """.trimIndent()

        private val INDEX_V2 = """
            {
              "repo": {
                "name": { "en-US": "Burton Workspaces" },
                "description": { "en-US": "Apps." },
                "address": "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo",
                "timestamp": 1790781809000
              },
              "packages": {
                "com.burton.sonos": {
                  "metadata": {
                    "name": { "en-US": "Burton Sonos" },
                    "categories": ["fdroid"]
                  },
                  "versions": {
                    "aaa": {
                      "file": { "name": "/burton-sonos-1.4.1.apk", "sha256": "aaa", "size": 1889664 },
                      "manifest": {
                        "versionName": "1.4.1",
                        "versionCode": 1004001,
                        "usesSdk": { "minSdkVersion": 26 }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
