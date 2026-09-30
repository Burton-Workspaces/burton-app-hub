package com.burton.apphub.data.install

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledPackage(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
)

@Singleton
class InstalledApps @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun all(): Map<String, InstalledPackage> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        return packages.mapNotNull { info ->
            val pkg = info.toInstalled() ?: return@mapNotNull null
            pkg.packageName to pkg
        }.toMap()
    }

    fun get(packageName: String): InstalledPackage? {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            info.toInstalled()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launchIntent(packageName: String) = context.packageManager.getLaunchIntentForPackage(packageName)

    private fun PackageInfo.toInstalled(): InstalledPackage? {
        val pkg = packageName ?: return null
        val code = if (Build.VERSION.SDK_INT >= 28) longVersionCode else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
        return InstalledPackage(pkg, versionName, code)
    }
}
