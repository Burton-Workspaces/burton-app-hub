package com.burton.apphub.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burton.apphub.domain.DefaultRepos
import com.burton.apphub.domain.Repo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hubStore: DataStore<Preferences> by preferencesDataStore("burton_app_hub")

@Singleton
class LocalPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.hubStore

    val repos: Flow<List<Repo>> = store.data.map { prefs ->
        val raw = prefs[REPOS]
        if (raw == null) DefaultRepos.seed else decodeRepos(raw)
    }

    val autoUpdate: Flow<Boolean> = store.data.map { it[AUTO_UPDATE] ?: false }

    val lastRefreshed: Flow<Long?> = store.data.map { it[LAST_REFRESHED] }

    suspend fun repos(): List<Repo> = repos.first()

    suspend fun autoUpdateEnabled(): Boolean = autoUpdate.first()

    suspend fun setAutoUpdate(enabled: Boolean) {
        store.edit { it[AUTO_UPDATE] = enabled }
    }

    suspend fun setLastRefreshed(epochMillis: Long) {
        store.edit { it[LAST_REFRESHED] = epochMillis }
    }

    suspend fun saveRepos(list: List<Repo>) {
        store.edit { it[REPOS] = encodeRepos(list) }
    }

    suspend fun addRepo(repo: Repo) {
        val current = repos()
        if (current.any { it.address.equals(repo.address, ignoreCase = true) || it.id == repo.id }) {
            throw IllegalStateException("That repository is already added")
        }
        saveRepos(current + repo)
    }

    suspend fun removeRepo(id: String) {
        saveRepos(repos().filterNot { it.id == id })
    }

    suspend fun setRepoEnabled(id: String, enabled: Boolean) {
        saveRepos(repos().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    companion object {
        private val REPOS = stringPreferencesKey("repos")
        private val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        private val LAST_REFRESHED = longPreferencesKey("last_refreshed")

        fun encodeRepos(repos: List<Repo>): String {
            val array = JSONArray()
            repos.forEach { repo ->
                array.put(
                    JSONObject()
                        .put("id", repo.id)
                        .put("name", repo.name)
                        .put("address", repo.address)
                        .put("fingerprint", repo.fingerprint)
                        .put("enabled", repo.enabled)
                        .put("description", repo.description),
                )
            }
            return array.toString()
        }

        fun decodeRepos(raw: String): List<Repo> {
            if (raw.isBlank()) return emptyList()
            val array = JSONArray(raw)
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val address = obj.optString("address")
                    if (address.isBlank()) continue
                    add(
                        Repo(
                            id = obj.optString("id").ifBlank { address },
                            name = obj.optString("name").ifBlank { address },
                            address = address,
                            fingerprint = obj.optString("fingerprint"),
                            enabled = obj.optBoolean("enabled", true),
                            description = obj.optString("description"),
                        ),
                    )
                }
            }
        }
    }
}
