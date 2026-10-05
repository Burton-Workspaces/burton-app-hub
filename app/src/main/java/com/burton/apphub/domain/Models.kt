package com.burton.apphub.domain

data class Repo(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String = "",
    val enabled: Boolean = true,
    val description: String = "",
)

data class ApkVersion(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val minSdk: Int,
    val added: Long = 0,
)

data class CatalogApp(
    val packageName: String,
    val name: String,
    val summary: String,
    val description: String,
    val iconUrls: List<String>,
    val license: String,
    val categories: List<String>,
    val repoId: String,
    val repoName: String,
    val versions: List<ApkVersion>,
    val installedVersionCode: Long? = null,
    val installedVersionName: String? = null,
) {
    val suggested: ApkVersion?
        get() = versions.maxByOrNull { it.versionCode }

    val iconUrl: String?
        get() = iconUrls.firstOrNull()

    val isInstalled: Boolean
        get() = installedVersionCode != null

    val hasUpdate: Boolean
        get() {
            val installed = installedVersionCode ?: return false
            val latest = suggested?.versionCode ?: return false
            return latest > installed
        }

    val statusLabel: String
        get() = when {
            hasUpdate -> "Update"
            isInstalled -> "Installed"
            else -> suggested?.versionName ?: ""
        }
}

data class InstallJob(
    val packageName: String,
    val versionName: String,
    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
    val stage: Stage = Stage.DOWNLOADING,
    val error: String? = null,
) {
    enum class Stage { DOWNLOADING, INSTALLING, DONE, FAILED }

    val progress: Float
        get() = if (totalBytes <= 0L) 0f else (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class StoreSnapshot(
    val repos: List<Repo> = emptyList(),
    val apps: List<CatalogApp> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
    val lastRefreshed: Long? = null,
    val autoUpdate: Boolean = false,
    val jobs: Map<String, InstallJob> = emptyMap(),
) {
    val updates: List<CatalogApp>
        get() = apps.filter { it.hasUpdate }.sortedBy { it.name.lowercase() }

    val enabledRepos: List<Repo>
        get() = repos.filter { it.enabled }
}

object DefaultRepos {
    const val BURTON_WORKSPACES_ID = "burton-workspaces"
    const val BURTON_WORKSPACES_ADDRESS =
        "https://burton-workspaces.github.io/burton-app-dist/fdroid/repo"
    private const val LEGACY_BURTON_WORKSPACES_ADDRESS =
        "https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo"

    val burtonWorkspaces = Repo(
        id = BURTON_WORKSPACES_ID,
        name = "Burton Workspaces",
        address = BURTON_WORKSPACES_ADDRESS,
        fingerprint = "D517D045B3E2FB297C0EC0BBA17AFF03488A4BB4EF431331A3A1C3FB46A5EFB6",
        enabled = true,
        description = "Apps published by Burton Workspaces.",
    )

    val seed = listOf(burtonWorkspaces)

    fun migrateAddress(address: String): String =
        address.replace(LEGACY_BURTON_WORKSPACES_ADDRESS, BURTON_WORKSPACES_ADDRESS, ignoreCase = true)
}
