package com.burton.apphub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.apphub.data.repository.StoreRepository
import com.burton.apphub.domain.StoreSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddRepoUi(
    val address: String = "",
    val fingerprint: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: StoreRepository,
) : ViewModel() {
    val state: StateFlow<StoreSnapshot> = store.state

    private val _add = MutableStateFlow(AddRepoUi())
    val add: StateFlow<AddRepoUi> = _add.asStateFlow()

    fun onAddress(value: String) {
        _add.value = _add.value.copy(address = value, error = null)
    }

    fun onFingerprint(value: String) {
        _add.value = _add.value.copy(fingerprint = value, error = null)
    }

    fun resetAdd() {
        _add.value = AddRepoUi()
    }

    fun setAutoUpdate(enabled: Boolean) = store.setAutoUpdate(enabled)

    fun setRepoEnabled(id: String, enabled: Boolean) = store.setRepoEnabled(id, enabled)

    fun removeRepo(id: String) = store.removeRepo(id)

    fun addRepo(onAdded: () -> Unit) {
        val form = _add.value
        viewModelScope.launch {
            _add.value = form.copy(busy = true, error = null)
            val result = store.addRepo(form.address, form.fingerprint)
            result.fold(
                onSuccess = {
                    _add.value = AddRepoUi()
                    onAdded()
                },
                onFailure = { error ->
                    _add.value = form.copy(busy = false, error = error.message ?: "Could not add repository")
                },
            )
        }
    }
}
