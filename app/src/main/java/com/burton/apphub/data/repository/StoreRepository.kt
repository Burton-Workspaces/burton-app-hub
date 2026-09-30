package com.burton.apphub.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.burton.apphub.MainActivity
import com.burton.apphub.R
import com.burton.apphub.data.index.FdroidIndexClient
import com.burton.apphub.data.install.ApkInstaller
import com.burton.apphub.data.install.InstalledApps
import com.burton.apphub.domain.ApkVersion
import com.burton.apphub.domain.CatalogApp
import com.burton.apphub.domain.InstallJob
import com.burton.apphub.domain.Repo
import com.burton.apphub.domain.StoreSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: LocalPrefs,
    private val indexClient: FdroidIndexClient,
    private val installedApps: InstalledApps,
    private val installer: ApkInstaller,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshLock = Mutex()
    private val _state = MutableStateFlow(StoreSnapshot())
    val state: StateFlow<StoreSnapshot> = _state.asStateFlow()

    init {
        scope.launch {
            prefs.repos.collect { repos ->
                _state.update { it.copy(repos = repos) }
            }
        }
        scope.launch {
            prefs.autoUpdate.collect { enabled ->
                _state.update { it.copy(autoUpdate = enabled) }
            }
        }
        scope.launch {
            prefs.lastRefreshed.collect { stamp ->
                _state.update { it.copy(lastRefreshed = stamp) }
            }
        }
        scope.launch { refresh(notifyUpdates = false) }
    }

    fun refresh(notifyUpdates: Boolean = false) {
        scope.launch { refreshNow(notifyUpdates) }
    }

    suspend fun refreshNow(notifyUpdates: Boolean): Result<Unit> = refreshLock.withLock {
        _state.update { it.copy(refreshing = true, error = null) }
        val repos = prefs.repos()
        val installed = installedApps.all()
        val errors = mutableListOf<String>()
        val catalog = mutableListOf<CatalogApp>()
        repos.filter { it.enabled }.forEach { repo ->
            try {
                val parsed = withContext(Dispatchers.IO) { indexClient.fetch(repo) }
                if (parsed.repoName.isNotBlank() && parsed.repoName != repo.name) {
                    prefs.saveRepos(
                        prefs.repos().map {
                            if (it.id == repo.id) it.copy(name = parsed.repoName, description = parsed.description.ifBlank { it.description }) else it
                        },
                    )
                }
                parsed.apps.forEach { app ->
                    val local = installed[app.packageName]
                    catalog += app.copy(
                        installedVersionCode = local?.versionCode,
                        installedVersionName = local?.versionName,
                    )
                }
            } catch (error: Exception) {
                errors += "${repo.name}: ${error.message ?: "failed"}"
            }
        }
        val merged = mergeApps(catalog)
        val stamp = System.currentTimeMillis()
        prefs.setLastRefreshed(stamp)
        _state.update {
            it.copy(
                apps = merged,
                refreshing = false,
                error = errors.takeIf { list -> list.isNotEmpty() }?.joinToString("\n"),
                lastRefreshed = stamp,
            )
        }
        if (notifyUpdates) notifyIfUpdates(merged.count { it.hasUpdate })
        if (errors.isNotEmpty() && merged.isEmpty()) Result.failure(IllegalStateException(errors.joinToString("\n")))
        else Result.success(Unit)
    }

    suspend fun addRepo(address: String, fingerprint: String): Result<Repo> {
        val trimmed = address.trim().trimEnd('/')
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Enter a repository URL"))
        val candidates = linkedSetOf(trimmed)
        if (!trimmed.endsWith("/fdroid/repo")) {
            candidates += "$trimmed/fdroid/repo"
        }
        var lastError: Exception? = null
        for (candidate in candidates) {
            val probe = Repo(
                id = UUID.randomUUID().toString(),
                name = candidate.substringAfterLast('/').ifBlank { candidate },
                address = candidate,
                fingerprint = fingerprint.trim().replace(" ", "").uppercase(),
            )
            try {
                val parsed = withContext(Dispatchers.IO) { indexClient.fetch(probe) }
                val repo = probe.copy(
                    name = parsed.repoName.ifBlank { probe.name },
                    address = parsed.repoAddress.ifBlank { candidate },
                    description = parsed.description,
                )
                prefs.addRepo(repo)
                refresh(notifyUpdates = false)
                return Result.success(repo)
            } catch (error: Exception) {
                lastError = error
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Could not add repository"))
    }

    fun removeRepo(id: String) {
        scope.launch {
            prefs.removeRepo(id)
            refresh(notifyUpdates = false)
        }
    }

    fun setRepoEnabled(id: String, enabled: Boolean) {
        scope.launch {
            prefs.setRepoEnabled(id, enabled)
            refresh(notifyUpdates = false)
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        scope.launch { prefs.setAutoUpdate(enabled) }
    }

    fun install(packageName: String) {
        val app = _state.value.apps.firstOrNull { it.packageName == packageName } ?: return
        val version = app.suggested ?: return
        install(app, version)
    }

    fun install(app: CatalogApp, version: ApkVersion) {
        if (!installer.canInstall()) {
            context.startActivity(installer.unknownSourcesIntent())
            return
        }
        scope.launch {
            try {
                _state.update {
                    it.copy(
                        jobs = it.jobs + (app.packageName to InstallJob(
                            packageName = app.packageName,
                            versionName = version.versionName,
                            stage = InstallJob.Stage.DOWNLOADING,
                            totalBytes = version.size,
                        )),
                    )
                }
                val apk = withContext(Dispatchers.IO) {
                    installer.download(app.packageName, version) { job ->
                        _state.update { snap -> snap.copy(jobs = snap.jobs + (app.packageName to job)) }
                    }
                }
                _state.update {
                    it.copy(
                        jobs = it.jobs + (app.packageName to InstallJob(
                            packageName = app.packageName,
                            versionName = version.versionName,
                            bytesRead = version.size,
                            totalBytes = version.size,
                            stage = InstallJob.Stage.INSTALLING,
                        )),
                    )
                }
                withContext(Dispatchers.Main) { installer.install(app.packageName, apk) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        jobs = it.jobs + (app.packageName to InstallJob(
                            packageName = app.packageName,
                            versionName = version.versionName,
                            stage = InstallJob.Stage.FAILED,
                            error = error.message ?: "Install failed",
                        )),
                    )
                }
            }
        }
    }

    fun onInstallFinished(packageName: String, error: String?) {
        _state.update { snap ->
            val job = snap.jobs[packageName]
            snap.copy(
                jobs = if (job == null) snap.jobs else snap.jobs + (packageName to job.copy(
                    stage = if (error == null) InstallJob.Stage.DONE else InstallJob.Stage.FAILED,
                    error = error,
                )),
            )
        }
        if (error == null) refresh(notifyUpdates = false)
    }

    fun open(packageName: String) {
        val intent = installedApps.launchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun uninstall(packageName: String) {
        context.startActivity(installer.uninstallIntent(packageName))
    }

    fun canInstall(): Boolean = installer.canInstall()

    fun requestInstallPermission() {
        context.startActivity(installer.unknownSourcesIntent())
    }

    private fun mergeApps(apps: List<CatalogApp>): List<CatalogApp> {
        return apps.groupBy { it.packageName }.values.map { group ->
            val primary = group.maxBy { it.suggested?.versionCode ?: 0L }
            primary.copy(
                versions = group.flatMap { it.versions }.distinctBy { it.versionCode }.sortedByDescending { it.versionCode },
                iconUrls = group.flatMap { it.iconUrls }.distinct(),
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun notifyIfUpdates(count: Int) {
        if (count <= 0 || !_state.value.autoUpdate) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_UPDATES, "Updates", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, "updates"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_hub)
            .setContentTitle("Burton App Hub")
            .setContentText(if (count == 1) "1 update available" else "$count updates available")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_UPDATES, notification)
    }

    companion object {
        private const val CHANNEL_UPDATES = "updates"
        private const val NOTIFICATION_UPDATES = 41
    }
}
