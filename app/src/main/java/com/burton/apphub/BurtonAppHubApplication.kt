package com.burton.apphub

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.burton.apphub.data.repository.CatalogRefreshWorker
import com.burton.apphub.data.repository.LocalPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BurtonAppHubApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefs: LocalPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            CatalogRefreshWorker.schedule(this@BurtonAppHubApplication, prefs.autoUpdateEnabled())
            prefs.autoUpdate.distinctUntilChanged().drop(1).collect { enabled ->
                CatalogRefreshWorker.schedule(this@BurtonAppHubApplication, enabled)
            }
        }
    }
}
