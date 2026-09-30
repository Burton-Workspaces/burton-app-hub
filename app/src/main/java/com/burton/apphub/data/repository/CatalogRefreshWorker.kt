package com.burton.apphub.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CatalogRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val store: StoreRepository,
    private val prefs: LocalPrefs,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val notify = prefs.autoUpdateEnabled()
        return store.refreshNow(notifyUpdates = notify).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val UNIQUE = "catalog-refresh"

        fun schedule(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork(UNIQUE)
                return
            }
            val request = PeriodicWorkRequestBuilder<CatalogRefreshWorker>(6, TimeUnit.HOURS)
                .build()
            manager.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
