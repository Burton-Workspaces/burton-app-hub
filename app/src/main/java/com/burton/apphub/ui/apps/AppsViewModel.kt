package com.burton.apphub.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.apphub.data.repository.StoreRepository
import com.burton.apphub.domain.CatalogApp
import com.burton.apphub.domain.StoreSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppsUi(
    val snapshot: StoreSnapshot = StoreSnapshot(),
    val query: String = "",
) {
    val visibleApps: List<CatalogApp>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return snapshot.apps
            return snapshot.apps.filter { app ->
                app.name.contains(needle, ignoreCase = true) ||
                    app.summary.contains(needle, ignoreCase = true) ||
                    app.packageName.contains(needle, ignoreCase = true)
            }
        }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val store: StoreRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val ui: StateFlow<AppsUi> = combine(store.state, query) { snapshot, q ->
        AppsUi(snapshot, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUi())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clear() {
        query.value = ""
    }

    fun refresh() = store.refresh(notifyUpdates = false)
}
