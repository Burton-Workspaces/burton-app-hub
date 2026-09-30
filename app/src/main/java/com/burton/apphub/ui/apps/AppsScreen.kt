package com.burton.apphub.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.apphub.ui.components.AppRow
import com.burton.apphub.ui.components.AppsSkeleton
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonLine
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.theme.BurtonSand

@Composable
fun AppsScreen(
    onOpenApp: (String) -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snapshot = ui.snapshot
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Apps",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh, enabled = !snapshot.refreshing) {
                if (snapshot.refreshing && snapshot.apps.isNotEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BurtonSand,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = "Refresh", tint = BurtonIvory)
                }
            }
        }
        Text(
            text = when {
                snapshot.refreshing && snapshot.apps.isEmpty() -> "Loading repositories"
                snapshot.apps.isEmpty() -> "No apps in your repositories yet"
                ui.query.isNotBlank() -> "${ui.visibleApps.size} of ${snapshot.apps.size} apps"
                else -> "${snapshot.apps.size} apps from ${snapshot.enabledRepos.size} ${if (snapshot.enabledRepos.size == 1) "repo" else "repos"}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (ui.query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BurtonIvory,
                unfocusedTextColor = BurtonIvory,
                focusedBorderColor = BurtonSand,
                unfocusedBorderColor = BurtonLine,
                cursorColor = BurtonIvory,
                focusedPlaceholderColor = BurtonMute,
                unfocusedPlaceholderColor = BurtonMute,
                focusedLeadingIconColor = BurtonSand,
                unfocusedLeadingIconColor = BurtonMute,
                focusedTrailingIconColor = BurtonIvory,
                unfocusedTrailingIconColor = BurtonMute,
            ),
        )
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.refreshing && snapshot.apps.isEmpty() -> AppsSkeleton()
            snapshot.apps.isEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        snapshot.error ?: "Add an F-Droid repository in Settings.",
                        color = BurtonIvory,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = viewModel::refresh) {
                        Text("Try again", color = BurtonSand)
                    }
                }
            }
            ui.visibleApps.isEmpty() -> Text("No matches.", color = BurtonMute)
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.visibleApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        job = snapshot.jobs[app.packageName],
                        onClick = { onOpenApp(app.packageName) },
                    )
                }
            }
        }
    }
}
