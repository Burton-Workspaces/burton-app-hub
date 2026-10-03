package com.burton.apphub.ui.updates

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.apphub.ui.components.AppRow
import com.burton.apphub.ui.components.AppsSkeleton
import com.burton.apphub.ui.components.EmptyStatePanel
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.theme.BurtonSand
import com.burton.apphub.ui.theme.BurtonVoid

@Composable
fun UpdatesScreen(
    onOpenApp: (String) -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val updates = snapshot.updates
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Updates",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh, enabled = !snapshot.refreshing) {
                if (snapshot.refreshing) {
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
        when {
            snapshot.refreshing && snapshot.apps.isEmpty() -> {
                Text(
                    "Checking repositories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                )
                Spacer(Modifier.height(16.dp))
                AppsSkeleton(count = 3)
            }
            updates.isEmpty() -> {
                Spacer(Modifier.height(16.dp))
                EmptyStatePanel(title = "Everything is up to date")
            }
            else -> {
                Text(
                    text = if (updates.size == 1) "1 update available" else "${updates.size} updates available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::updateAll,
                    colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Update all")
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(updates, key = { it.packageName }) { app ->
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
}
