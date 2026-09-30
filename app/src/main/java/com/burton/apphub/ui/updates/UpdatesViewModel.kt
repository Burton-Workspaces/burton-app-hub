package com.burton.apphub.ui.updates

import androidx.lifecycle.ViewModel
import com.burton.apphub.data.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val store: StoreRepository,
) : ViewModel() {
    val state = store.state

    fun refresh() = store.refresh(notifyUpdates = false)

    fun updateAll() {
        store.state.value.updates.forEach { store.install(it.packageName) }
    }
}
