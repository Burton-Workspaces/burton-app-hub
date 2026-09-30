package com.burton.apphub.data.icon

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class AppIconFetcher(
    private val key: AppIconKey,
    private val context: Context,
    private val http: OkHttpClient,
    private val probe: OkHttpClient,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        lockFor(key.packageName).withLock {
            installedIcon() ?: cachedPng() ?: repoIcon() ?: apkIcon()
        }
    }

    private fun installedIcon(): FetchResult? {
        if (!key.installed) return null
        return try {
            val drawable = context.packageManager.getApplicationIcon(key.packageName)
            DrawableResult(drawable, false, DataSource.MEMORY)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun cachedPng(): FetchResult? {
        val file = cacheFile()
        if (!file.exists() || file.length() < 32L) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    private fun repoIcon(): FetchResult? {
        key.iconUrls.forEach { url ->
            val bytes = downloadImage(url) ?: return@forEach
            writeCache(bytes)
            return cachedPng()
        }
        return null
    }

    private fun apkIcon(): FetchResult? {
        val apkUrl = key.apkUrl ?: return null
        val apk = File(context.cacheDir, "icons/${key.packageName}.apk")
        apk.parentFile?.mkdirs()
        return try {
            downloadTo(apkUrl, apk)
            val bitmap = iconFromApk(apk) ?: return null
            cacheFile().outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            cachedPng()
        } catch (_: Exception) {
            null
        } finally {
            apk.delete()
        }
    }

    private fun iconFromApk(apk: File): Bitmap? {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, 0)
        } ?: return null
        val appInfo = info.applicationInfo ?: return null
        appInfo.sourceDir = apk.absolutePath
        appInfo.publicSourceDir = apk.absolutePath
        val drawable = appInfo.loadIcon(pm) ?: return null
        return drawable.toBitmap(width = ICON_SIZE, height = ICON_SIZE)
    }

    private fun downloadImage(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).get().build()
            probe.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val type = response.header("content-type").orEmpty().lowercase()
                if (type.contains("html") || type.contains("text/")) return null
                val bytes = response.body?.bytes() ?: return null
                if (!looksLikeImage(bytes)) return null
                bytes
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadTo(url: String, dest: File) {
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            dest.outputStream().use { out ->
                response.body?.byteStream()?.copyTo(out)
            }
        }
    }

    private fun writeCache(bytes: ByteArray) {
        val file = cacheFile()
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun cacheFile(): File {
        val dir = File(context.cacheDir, "icons").apply { mkdirs() }
        return File(dir, "${key.packageName}.png")
    }

    class Factory(
        private val context: Context,
        private val http: OkHttpClient,
    ) : Fetcher.Factory<AppIconKey> {
        private val probe: OkHttpClient = http.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        override fun create(data: AppIconKey, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, context, http, probe)
        }
    }

    companion object {
        private const val ICON_SIZE = 192
        private val locks = mutableMapOf<String, Mutex>()

        private fun lockFor(packageName: String): Mutex =
            synchronized(locks) { locks.getOrPut(packageName) { Mutex() } }

        private fun looksLikeImage(bytes: ByteArray): Boolean {
            if (bytes.size < 12) return false
            if (bytes[0] == '<'.code.toByte()) return false
            val png = bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte()
            val jpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            val riff = bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "RIFF"
            val webp = riff && bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII) == "WEBP"
            return png || jpeg || webp
        }
    }
}
