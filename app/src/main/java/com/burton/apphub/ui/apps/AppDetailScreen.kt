package com.burton.apphub.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.apphub.domain.InstallJob
import com.burton.apphub.ui.components.AppIcon
import com.burton.apphub.ui.theme.BurtonCharcoal
import com.burton.apphub.ui.theme.BurtonDanger
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.theme.BurtonSand
import com.burton.apphub.ui.theme.BurtonVoid

@Composable
fun AppDetailScreen(
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val app = ui.app
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(app?.name ?: "App", style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
        }
        if (app == null) {
            Text("This app is no longer in your repositories.", color = BurtonMute, modifier = Modifier.padding(16.dp))
            return
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 72.dp, corner = 16.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
                    Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
                    val versionLine = buildString {
                        app.suggested?.let { append(it.versionName) }
                        if (app.isInstalled) {
                            if (isNotEmpty()) append(" · ")
                            append("installed ${app.installedVersionName ?: app.installedVersionCode}")
                        }
                    }
                    if (versionLine.isNotBlank()) {
                        Text(versionLine, style = MaterialTheme.typography.bodyMedium, color = BurtonSand)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!ui.canInstall) {
                Text(
                    "Allow installing unknown apps so Burton App Hub can install APKs from your repositories.",
                    color = BurtonMute,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                TextButtonish("Allow installs", onClick = viewModel::requestInstallPermission)
                Spacer(Modifier.height(12.dp))
            }
            val job = ui.job
            when (job?.stage) {
                InstallJob.Stage.DOWNLOADING -> {
                    Text("Downloading ${job.versionName}", color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { job.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = BurtonSand,
                        trackColor = BurtonMute.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                InstallJob.Stage.INSTALLING -> {
                    Text("Waiting for the system install prompt.", color = BurtonMute)
                    Spacer(Modifier.height(16.dp))
                }
                InstallJob.Stage.FAILED -> {
                    Text(job.error ?: "Install failed", color = BurtonIvory)
                    Spacer(Modifier.height(16.dp))
                }
                else -> Unit
            }
            PrimaryButton(
                label = when {
                    app.hasUpdate -> "Update to ${app.suggested?.versionName}"
                    app.isInstalled -> "Open"
                    else -> "Install ${app.suggested?.versionName ?: ""}".trim()
                },
                onClick = {
                    when {
                        app.hasUpdate -> viewModel.install()
                        app.isInstalled -> viewModel.open()
                        else -> viewModel.install()
                    }
                },
                enabled = job?.stage != InstallJob.Stage.DOWNLOADING && job?.stage != InstallJob.Stage.INSTALLING,
            )
            if (app.isInstalled) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::uninstall,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BurtonDanger),
                ) {
                    Text("Uninstall")
                }
            }
            Spacer(Modifier.height(20.dp))
            MetaLine("Repository", app.repoName)
            MetaLine("License", app.license)
            if (app.summary.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("ABOUT", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
                Spacer(Modifier.height(6.dp))
                Text(app.summary, style = MaterialTheme.typography.bodyLarge, color = BurtonIvory)
            }
            if (app.description.isNotBlank() && app.description != app.summary) {
                Spacer(Modifier.height(10.dp))
                Text(app.description, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
            }
            if (app.versions.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("VERSIONS", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
                Spacer(Modifier.height(8.dp))
                app.versions.forEach { version ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(version.versionName, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
                            Text(
                                "${formatSize(version.size)} · ${version.versionCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BurtonMute,
                            )
                        }
                        TextButtonish("Install", onClick = { viewModel.install(version) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Composable
private fun TextButtonish(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(label, color = BurtonSand)
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BurtonMute, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = BurtonIvory, modifier = Modifier.weight(0.6f))
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "APK"
    val kb = bytes / 1024.0
    return if (kb < 1024) "${kb.toInt()} KB" else String.format("%.1f MB", kb / 1024.0)
}
