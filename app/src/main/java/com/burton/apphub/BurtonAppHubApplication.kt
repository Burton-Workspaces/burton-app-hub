package com.burton.apphub

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.burton.apphub.data.icon.AppIconFetcher
import com.burton.apphub.data.repository.CatalogRefreshWorker
import com.burton.apphub.data.repository.LocalPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class BurtonAppHubApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefs: LocalPrefs
    @Inject lateinit var http: OkHttpClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(http)
            .components { add(AppIconFetcher.Factory(this@BurtonAppHubApplication, http)) }
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
