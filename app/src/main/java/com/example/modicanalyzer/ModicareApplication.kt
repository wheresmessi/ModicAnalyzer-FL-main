package com.example.modicanalyzer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main Application class for the Modicare Offline-First app.
 * 
 * Responsibilities:
 * - Initialize Hilt dependency injection
 * - Configure WorkManager for background sync
 * - Set up Firebase
 * - Initialize any global app configurations
 * 
 * The @HiltAndroidApp annotation triggers Hilt's code generation and creates
 * an application-level dependency container.
 * 
 * Features:
 * - Singleton dependency injection
 * - WorkManager integration with Hilt
 * - Custom WorkerFactory for dependency injection in Workers
 */
@HiltAndroidApp
class ModicareApplication : Application(), Configuration.Provider {
    
    /**
     * Hilt-provided WorkerFactory for creating Workers with dependency injection.
     * This allows SyncWorker to receive injected dependencies.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize application-level components here if needed
        // Firebase is auto-initialized via google-services.json
        
        android.util.Log.d("ModicareApp", "Application initialized with Hilt and WorkManager")
    }
    
    /**
     * Provide WorkManager configuration with custom WorkerFactory.
     * This is required for Hilt to inject dependencies into Workers.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
