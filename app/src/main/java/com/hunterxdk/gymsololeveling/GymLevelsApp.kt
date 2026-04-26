package com.hunterxdk.gymsololeveling

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hunterxdk.gymsololeveling.core.worker.StreakCheckWorker
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymLevelsApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var exerciseRepository: ExerciseRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        StreakCheckWorker.schedule(this)
        val handler = CoroutineExceptionHandler { _, e ->
            Log.e("GymLevelsApp", "Exercise seeding failed", e)
        }
        MainScope().launch(handler) {
            try {
                exerciseRepository.seedIfNeeded()
                Log.d("GymLevelsApp", "Exercise seeding complete")
            } catch (e: Exception) {
                Log.e("GymLevelsApp", "Exercise seeding failed: ${e.message}", e)
            }
        }
    }
}