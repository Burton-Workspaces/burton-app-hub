package com.burton.apphub.ui.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.apphub.data.repository.StoreRepository
import com.burton.apphub.domain.ApkVersion
import com.burton.apphub.domain.CatalogApp
import com.burton.apphub.domain.InstallJob
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppDetailUi(
    val app: CatalogApp? = null,
    val job: InstallJob? = null,
    val canInstall: Boolean = true,
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val store: StoreRepository,
) : ViewModel() {
    private val packageName: String = android.net.Uri.decode(checkNotNull(savedStateHandle["packageName"]))

    val ui: StateFlow<AppDetailUi> = store.state.map { snap ->
        AppDetailUi(
            app = snap.apps.firstOrNull { it.packageName == packageName },
            job = snap.jobs[packageName],
            canInstall = store.canInstall(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDetailUi())

    fun install() = store.install(packageName)

    fun install(version: ApkVersion) {
        val app = ui.value.app ?: return
        store.install(app, version)
    }

    fun open() = store.open(packageName)

    fun uninstall() = store.uninstall(packageName)

    fun requestInstallPermission() = store.requestInstallPermission()
}
