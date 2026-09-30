package com.burton.apphub.data.index

import com.burton.apphub.domain.ApkVersion
import com.burton.apphub.domain.CatalogApp
import org.json.JSONArray
import org.json.JSONObject

data class ParsedIndex(
    val repoName: String,
    val repoAddress: String,
    val description: String,
    val timestamp: Long,
    val apps: List<CatalogApp>,
)

object FdroidIndexParser {
    fun parse(body: String, fallbackAddress: String, repoId: String): ParsedIndex {
        val root = JSONObject(body)
        return when {
            root.has("packages") && root.optJSONObject("packages")?.keys()?.hasNext() == true &&
                root.optJSONArray("apps") != null -> parseV1(root, fallbackAddress, repoId)
            root.has("packages") && root.optJSONObject("repo") != null -> {
                if (root.has("apps")) parseV1(root, fallbackAddress, repoId)
                else parseV2(root, fallbackAddress, repoId)
            }
            else -> parseV1(root, fallbackAddress, repoId)
        }
    }

    fun parseV1(root: JSONObject, fallbackAddress: String, repoId: String): ParsedIndex {
        val repo = root.optJSONObject("repo") ?: JSONObject()
        val address = normalizeAddress(repo.optString("address").ifBlank { fallbackAddress })
        val repoName = repo.optString("name").ifBlank { address }
        val description = repo.optString("description")
        val timestamp = repo.optLong("timestamp")
        val packages = root.optJSONObject("packages") ?: JSONObject()
        val appsJson = root.optJSONArray("apps") ?: JSONArray()
        val apps = buildList {
            for (i in 0 until appsJson.length()) {
                val app = appsJson.optJSONObject(i) ?: continue
                val packageName = app.optString("packageName")
                if (packageName.isBlank()) continue
                val versions = parseV1Packages(packages.optJSONArray(packageName), address)
                add(
                    CatalogApp(
                        packageName = packageName,
                        name = app.optString("name").ifBlank { packageName },
                        summary = app.optString("summary"),
                        description = stripHtml(app.optString("description")),
                        iconUrl = iconUrl(address, packageName, app.optString("icon").ifBlank { null }),
                        license = app.optString("license").ifBlank { "Unknown" },
                        categories = stringList(app.optJSONArray("categories")),
                        repoId = repoId,
                        repoName = repoName,
                        versions = versions,
                    ),
                )
            }
        }
        return ParsedIndex(repoName, address, description, timestamp, apps.sortedBy { it.name.lowercase() })
    }

    fun parseV2(root: JSONObject, fallbackAddress: String, repoId: String): ParsedIndex {
        val repo = root.optJSONObject("repo") ?: JSONObject()
        val address = normalizeAddress(repo.optString("address").ifBlank { fallbackAddress })
        val repoName = localized(repo.optJSONObject("name")).ifBlank { address }
        val description = localized(repo.optJSONObject("description"))
        val timestamp = repo.optLong("timestamp")
        val packages = root.optJSONObject("packages") ?: JSONObject()
        val apps = buildList {
            packages.keys().forEach { packageName ->
                val entry = packages.optJSONObject(packageName) ?: return@forEach
                val metadata = entry.optJSONObject("metadata") ?: JSONObject()
                val versionsObj = entry.optJSONObject("versions") ?: JSONObject()
                val versions = parseV2Versions(versionsObj, address)
                val iconName = metadata.optJSONObject("icon")?.let { localizedName(it) }
                add(
                    CatalogApp(
                        packageName = packageName,
                        name = localized(metadata.optJSONObject("name")).ifBlank { packageName },
                        summary = localized(metadata.optJSONObject("summary")),
                        description = stripHtml(localized(metadata.optJSONObject("description"))),
                        iconUrl = iconUrl(address, packageName, iconName),
                        license = metadata.optString("license").ifBlank { "Unknown" },
                        categories = stringList(metadata.optJSONArray("categories")),
                        repoId = repoId,
                        repoName = repoName,
                        versions = versions,
                    ),
                )
            }
        }
        return ParsedIndex(repoName, address, description, timestamp, apps.sortedBy { it.name.lowercase() })
    }

    private fun parseV1Packages(array: JSONArray?, address: String): List<ApkVersion> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val apk = array.optJSONObject(i) ?: continue
                val apkName = apk.optString("apkName")
                if (apkName.isBlank()) continue
                add(
                    ApkVersion(
                        versionName = apk.optString("versionName"),
                        versionCode = apk.optLong("versionCode"),
                        apkName = apkName,
                        url = apkUrl(address, apkName),
                        sha256 = apk.optString("hash"),
                        size = apk.optLong("size"),
                        minSdk = apk.optInt("minSdkVersion"),
                        added = apk.optLong("added"),
                    ),
                )
            }
        }.sortedByDescending { it.versionCode }
    }

    private fun parseV2Versions(versions: JSONObject, address: String): List<ApkVersion> {
        return buildList {
            versions.keys().forEach { key ->
                val version = versions.optJSONObject(key) ?: return@forEach
                val file = version.optJSONObject("file") ?: JSONObject()
                val manifest = version.optJSONObject("manifest") ?: JSONObject()
                val usesSdk = manifest.optJSONObject("usesSdk") ?: JSONObject()
                val name = file.optString("name")
                if (name.isBlank()) return@forEach
                add(
                    ApkVersion(
                        versionName = manifest.optString("versionName"),
                        versionCode = manifest.optLong("versionCode"),
                        apkName = name.trimStart('/'),
                        url = apkUrl(address, name),
                        sha256 = file.optString("sha256").ifBlank { key },
                        size = file.optLong("size"),
                        minSdk = usesSdk.optInt("minSdkVersion"),
                        added = version.optLong("added"),
                    ),
                )
            }
        }.sortedByDescending { it.versionCode }
    }

    fun iconUrl(address: String, packageName: String, icon: String?): String {
        val base = normalizeAddress(address)
        val named = icon?.trim()?.trimStart('/')?.takeIf { it.isNotBlank() && !it.equals("icon.png", ignoreCase = true) }
        return if (named != null) {
            if (named.startsWith("http")) named
            else if (named.contains('/')) "$base/$named"
            else "$base/icons/$named"
        } else {
            "$base/icons-640/$packageName.png"
        }
    }

    fun apkUrl(address: String, apkName: String): String {
        val base = normalizeAddress(address)
        val name = apkName.trimStart('/')
        return if (name.startsWith("http")) name else "$base/$name"
    }

    fun normalizeAddress(address: String): String = address.trim().trimEnd('/')

    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        return html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)<p[^>]*>"), "")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun localized(obj: JSONObject?): String {
        if (obj == null) return ""
        listOf("en-US", "en").forEach { key ->
            val value = obj.optString(key)
            if (value.isNotBlank()) return value
        }
        val keys = obj.keys()
        if (keys.hasNext()) return obj.optString(keys.next())
        return ""
    }

    private fun localizedName(icon: JSONObject): String? {
        val localized = icon.optJSONObject("en-US") ?: icon.optJSONObject("en")
        if (localized != null) return localized.optString("name").ifBlank { null }
        val keys = icon.keys()
        while (keys.hasNext()) {
            val child = icon.optJSONObject(keys.next()) ?: continue
            val name = child.optString("name")
            if (name.isNotBlank()) return name
        }
        return null
    }

    private fun stringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }
}
