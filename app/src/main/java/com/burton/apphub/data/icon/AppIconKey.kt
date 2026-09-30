package com.burton.apphub.data.icon

data class AppIconKey(
    val packageName: String,
    val iconUrls: List<String>,
    val apkUrl: String?,
    val installed: Boolean,
)
