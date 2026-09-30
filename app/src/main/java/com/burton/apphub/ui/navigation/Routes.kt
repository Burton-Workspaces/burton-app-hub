package com.burton.apphub.ui.navigation

object Routes {
    const val APPS = "apps"
    const val UPDATES = "updates"
    const val SETTINGS = "settings"
    const val APP = "app/{packageName}"

    fun app(packageName: String) = "app/${android.net.Uri.encode(packageName)}"
}
