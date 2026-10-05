package com.burton.apphub.data.repository

import com.burton.apphub.domain.Repo
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPrefsCodecTest {
    @Test
    fun roundTripRepos() {
        val repos = listOf(
            Repo(
                id = "one",
                name = "Burton Workspaces",
                address = "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo",
                fingerprint = "D517D045",
                enabled = true,
                description = "Apps",
            ),
            Repo(
                id = "two",
                name = "Extra",
                address = "https://example.invalid/fdroid/repo",
                enabled = false,
            ),
        )
        val encoded = LocalPrefs.encodeRepos(repos)
        val decoded = LocalPrefs.decodeRepos(encoded)
        assertEquals(repos, decoded)
    }

    @Test
    fun emptyIsEmpty() {
        assertEquals(emptyList<Repo>(), LocalPrefs.decodeRepos(""))
    }

    @Test
    fun decodeMigratesLegacyCatalogAddress() {
        val encoded = LocalPrefs.encodeRepos(
            listOf(
                Repo(
                    id = "burton-workspaces",
                    name = "Burton Workspaces",
                    address = "https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo",
                    fingerprint = "D517D045",
                    enabled = true,
                    description = "Apps",
                ),
            ),
        )
        val decoded = LocalPrefs.decodeRepos(encoded)
        assertEquals(
            "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo",
            decoded.single().address,
        )
    }
}
