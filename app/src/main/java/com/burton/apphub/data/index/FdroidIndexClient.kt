package com.burton.apphub.data.index

import com.burton.apphub.domain.Repo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidIndexClient @Inject constructor(
    private val http: OkHttpClient,
) {
    fun fetch(repo: Repo): ParsedIndex {
        val address = FdroidIndexParser.normalizeAddress(repo.address)
        val errors = mutableListOf<String>()
        for (path in INDEX_PATHS) {
            try {
                val body = download("$address$path")
                val json = if (path.endsWith(".jar")) extractIndexJson(body) else body.decodeToString()
                return FdroidIndexParser.parse(json, address, repo.id)
            } catch (error: Exception) {
                errors += "${path.trimStart('/')}: ${error.message}"
            }
        }
        throw IllegalStateException("Could not load F-Droid index from $address. ${errors.joinToString("; ")}")
    }

    private fun download(url: String): ByteArray {
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            return response.body?.bytes() ?: throw IllegalStateException("Empty body")
        }
    }

    private fun extractIndexJson(jar: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(jar)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                if (name == "index-v1.json" || name == "index-v2.json" || name == "index.json") {
                    return zip.readBytes().decodeToString()
                }
                entry = zip.nextEntry
            }
        }
        throw IllegalStateException("No index JSON inside jar")
    }

    companion object {
        private val INDEX_PATHS = listOf("/index-v1.json", "/index-v2.json", "/index-v1.jar")
    }
}
