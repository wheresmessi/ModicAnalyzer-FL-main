package com.example.modicanalyzer.fl

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.util.concurrent.Executors

/**
 * Federated Learning Manager
 * Orchestrates the federated learning process including local training,
 * weight upload, aggregation, and model updates
 */
class FederatedLearningManager(
    private val context: Context,
    private val serverUrl: String = "https://modic-fl-server.onrender.com" // Your live Render server
) {
    private val flClient = FederatedLearningClient(context, serverUrl)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private const val TAG = "FLManager"
    }

    interface FederatedLearningCallback {
        fun onTrainingStarted()
        fun onWeightsExtracted(layerCount: Int)
        fun onLocalTrainingCompleted()
        fun onWeightsUploaded(response: String)
        fun onAggregationTriggered(response: String)
        fun onModelUpdated(layerCount: Int)
        fun onError(error: String)
        fun onStatusUpdate(message: String)
    }

    /**
     * Start a full federated learning cycle
     */
    fun startFederatedLearningCycle(
        interpreter: Interpreter,
        callback: FederatedLearningCallback
    ) {
        scope.launch {
            try {
                callback.onTrainingStarted()
                
                // Step 1: Extract current model weights
                callback.onStatusUpdate("Extracting model weights...")
                val currentWeights = flClient.extractModelWeights(interpreter)
                callback.onWeightsExtracted(currentWeights.size)
                
                // Step 2: Simulate local training
                callback.onStatusUpdate("Performing local training...")
                val trainedWeights = flClient.simulateLocalTraining(currentWeights, epochs = 5)
                callback.onLocalTrainingCompleted()
                
                // Step 3: Upload trained weights
                callback.onStatusUpdate("Uploading weights to server...")
                val uploadResult = flClient.uploadWeights(trainedWeights)
                uploadResult.fold(
                    onSuccess = { response ->
                        callback.onWeightsUploaded(response)
                        
                        // Step 4: Trigger aggregation (optional - could be done periodically)
                        callback.onStatusUpdate("Triggering aggregation...")
                        triggerAggregationAndUpdate(interpreter, callback)
                    },
                    onFailure = { error ->
                        callback.onError("Upload failed: ${error.message}")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in federated learning cycle", e)
                callback.onError("Federated learning failed: ${e.message}")
            }
        }
    }

    /**
     * Trigger aggregation and download updated model
     */
    fun triggerAggregationAndUpdate(
        interpreter: Interpreter,
        callback: FederatedLearningCallback
    ) {
        scope.launch {
            try {
                // Trigger aggregation
                val aggregationResult = flClient.triggerAggregation()
                aggregationResult.fold(
                    onSuccess = { response ->
                        callback.onAggregationTriggered(response)
                        
                        // Download updated weights
                        callback.onStatusUpdate("Downloading updated model...")
                        downloadAndApplyUpdatedModel(interpreter, callback)
                    },
                    onFailure = { error ->
                        callback.onError("Aggregation failed: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering aggregation", e)
                callback.onError("Aggregation error: ${e.message}")
            }
        }
    }

    /**
     * Download and apply updated model weights
     */
    private suspend fun downloadAndApplyUpdatedModel(
        interpreter: Interpreter,
        callback: FederatedLearningCallback
    ) {
        try {
            val downloadResult = flClient.downloadLatestWeights()
            downloadResult.fold(
                onSuccess = { weights ->
                    // In a real implementation, you would apply these weights to the interpreter
                    // This requires TensorFlow Lite Model Personalization or similar APIs
                    callback.onModelUpdated(weights.size)
                    callback.onStatusUpdate("Model updated successfully!")
                },
                onFailure = { error ->
                    if (error.message?.contains("No aggregated weights") == true) {
                        callback.onStatusUpdate("No aggregated model available yet")
                    } else {
                        callback.onError("Download failed: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading updated model", e)
            callback.onError("Model update failed: ${e.message}")
        }
    }

    /**
     * Check server status
     */
    fun checkServerStatus(callback: (String) -> Unit) {
        scope.launch {
            val result = flClient.getServerStatus()
            result.fold(
                onSuccess = { status -> callback("✅ Server online: $status") },
                onFailure = { error -> callback("❌ Server offline: ${error.message}") }
            )
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }

    /**
     * Schedule periodic federated learning
     */
    fun schedulePeriodicTraining(
        interpreter: Interpreter,
        callback: FederatedLearningCallback,
        intervalMinutes: Long = 60
    ) {
        scope.launch {
            while (isActive) {
                try {
                    startFederatedLearningCycle(interpreter, callback)
                    delay(intervalMinutes * 60 * 1000) // Convert to milliseconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic training", e)
                    delay(10 * 60 * 1000) // Wait 10 minutes before retry
                }
            }
        }
    }
}