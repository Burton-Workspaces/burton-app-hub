package com.burton.apphub.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.apphub.BuildConfig
import com.burton.apphub.domain.Repo
import com.burton.apphub.ui.components.FullScreenModal
import com.burton.apphub.ui.theme.BurtonCharcoal
import com.burton.apphub.ui.theme.BurtonDanger
import com.burton.apphub.ui.theme.BurtonElevated
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonLine
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.theme.BurtonSand

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Text(
            "Repositories, updates, and about this app",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("About", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
                        Spacer(Modifier.height(4.dp))
                        Text("Burton App Hub", style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
                    }
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BurtonMute,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto update", style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
                        Text(
                            "Check repositories in the background and notify when updates are available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BurtonMute,
                        )
                    }
                    Switch(
                        checked = snapshot.autoUpdate,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33) {
                                notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setAutoUpdate(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BurtonIvory,
                            checkedTrackColor = BurtonSand,
                            uncheckedThumbColor = BurtonMute,
                            uncheckedTrackColor = BurtonElevated,
                        ),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "REPOSITORIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = BurtonSand,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add repository", tint = BurtonIvory)
                    }
                }
            }
            items(snapshot.repos, key = { it.id }) { repo ->
                RepoCard(
                    repo = repo,
                    onEnabled = { viewModel.setRepoEnabled(repo.id, it) },
                    onRemove = { viewModel.removeRepo(repo.id) },
                )
            }
        }
    }
    if (adding) {
        AddRepoModal(
            viewModel = viewModel,
            onDismiss = {
                viewModel.resetAdd()
                adding = false
            },
        )
    }
}

@Composable
private fun RepoCard(
    repo: Repo,
    onEnabled: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
                Text(
                    repo.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = repo.enabled,
                onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BurtonIvory,
                    checkedTrackColor = BurtonSand,
                    uncheckedThumbColor = BurtonMute,
                    uncheckedTrackColor = BurtonElevated,
                ),
            )
        }
        if (repo.fingerprint.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Fingerprint ${formatFingerprint(repo.fingerprint)}",
                style = MaterialTheme.typography.labelSmall,
                color = BurtonMute,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = "Remove repository", tint = BurtonDanger)
            }
        }
    }
}

@Composable
private fun AddRepoModal(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val form by viewModel.add.collectAsStateWithLifecycle()
    FullScreenModal(
        onDismiss = onDismiss,
        title = "Add repository",
        actionLabel = if (form.busy) "Adding…" else "Add",
        actionEnabled = form.address.isNotBlank() && !form.busy,
        onAction = { viewModel.addRepo(onAdded = onDismiss) },
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Paste an F-Droid repository URL. Burton App Hub will fetch the index and list its apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        RepoField(
            value = form.address,
            onValueChange = viewModel::onAddress,
            label = "Repository URL",
            placeholder = "https://example.com/fdroid/repo",
        )
        Spacer(Modifier.height(12.dp))
        RepoField(
            value = form.fingerprint,
            onValueChange = viewModel::onFingerprint,
            label = "Fingerprint (optional)",
            placeholder = "64-character SHA-256",
        )
        if (form.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(form.error ?: "", color = BurtonIvory, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RepoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BurtonIvory,
            unfocusedTextColor = BurtonIvory,
            focusedBorderColor = BurtonSand,
            unfocusedBorderColor = BurtonLine,
            cursorColor = BurtonIvory,
            focusedLabelColor = BurtonSand,
            unfocusedLabelColor = BurtonMute,
            focusedPlaceholderColor = BurtonMute,
            unfocusedPlaceholderColor = BurtonMute,
        ),
    )
}

private fun formatFingerprint(value: String): String {
    val hex = value.replace(" ", "").uppercase()
    return hex.chunked(2).joinToString(" ")
}
