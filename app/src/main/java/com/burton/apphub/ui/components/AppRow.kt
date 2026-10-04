package com.burton.apphub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burton.apphub.domain.CatalogApp
import com.burton.apphub.domain.InstallJob
import com.burton.apphub.ui.theme.BurtonCharcoal
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.theme.BurtonSand

@Composable
fun AppRow(
    app: CatalogApp,
    job: InstallJob?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(app = app, size = 48.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.name,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = subtitle(app, job)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (job != null && job.stage == InstallJob.Stage.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { job.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = BurtonSand,
                    trackColor = BurtonMute.copy(alpha = 0.2f),
                )
            }
        }
        val badge = when {
            job?.stage == InstallJob.Stage.DOWNLOADING -> "Downloading"
            job?.stage == InstallJob.Stage.INSTALLING -> "Installing"
            job?.stage == InstallJob.Stage.FAILED -> "Failed"
            app.hasUpdate -> "Update"
            app.isInstalled -> "Installed"
            else -> app.suggested?.versionName.orEmpty()
        }
        if (badge.isNotBlank()) {
            Text(badge, style = MaterialTheme.typography.labelLarge, color = BurtonSand)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = BurtonMute)
    }
}

private fun subtitle(app: CatalogApp, job: InstallJob?): String {
    if (job?.error != null) return job.error
    return app.installedVersionName ?: app.installedVersionCode?.toString().orEmpty()
}
