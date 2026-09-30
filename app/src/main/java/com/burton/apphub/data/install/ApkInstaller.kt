package com.burton.apphub.data.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.burton.apphub.domain.ApkVersion
import com.burton.apphub.domain.InstallJob
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
) {
    fun canInstall(): Boolean =
        if (Build.VERSION.SDK_INT >= 26) context.packageManager.canRequestPackageInstalls() else true

    fun unknownSourcesIntent(): Intent {
        val uri = Uri.parse("package:${context.packageName}")
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun download(
        packageName: String,
        version: ApkVersion,
        onProgress: (InstallJob) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val dest = File(dir, "${packageName}-${version.versionCode}.apk")
        val request = Request.Builder().url(version.url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed (HTTP ${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("Empty APK body")
            val total = if (version.size > 0) version.size else body.contentLength()
            var read = 0L
            dest.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        out.write(buffer, 0, n)
                        read += n
                        onProgress(
                            InstallJob(
                                packageName = packageName,
                                versionName = version.versionName,
                                bytesRead = read,
                                totalBytes = total.coerceAtLeast(0),
                                stage = InstallJob.Stage.DOWNLOADING,
                            ),
                        )
                    }
                }
            }
        }
        if (version.sha256.isNotBlank()) {
            val actual = sha256(dest)
            if (!actual.equals(version.sha256, ignoreCase = true)) {
                dest.delete()
                throw IllegalStateException("APK checksum mismatch")
            }
        }
        return dest
    }

    fun install(packageName: String, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= 34) {
            params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("package", 0, apk.length()).use { out ->
                apk.inputStream().use { input -> input.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                putExtra(EXTRA_PACKAGE, packageName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val EXTRA_PACKAGE = "packageName"
    }
}
