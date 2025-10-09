package com.example.modicanalyzer.fl

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Federated Learning Client for Android
 * Handles weight serialization, upload, download, and model updates
 */
class FederatedLearningClient(
    private val context: Context,
    private val serverUrl: String = "https://modic-fl-server.onrender.com", // Your live Render server
    private val clientId: String = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "FLClient"
        private const val WEIGHTS_FILE = "model_weights.npz"
    }

    /**
     * Extract weights from TensorFlow Lite model
     * Note: This is a simplified version. In practice, you'd need to use
     * TensorFlow Lite's Model Personalization API or extract weights differently
     */
    suspend fun extractModelWeights(interpreter: org.tensorflow.lite.Interpreter): Map<String, FloatArray> = withContext(Dispatchers.IO) {
        val weights = mutableMapOf<String, FloatArray>()
        
        try {
            // This is a placeholder - actual implementation depends on your model structure
            // You would typically use TensorFlow Lite's Model Personalization API
            // or access the model's internal tensors
            
            // For now, we'll create dummy weights based on typical CNN structure
            weights["conv2d_kernel"] = FloatArray(3 * 3 * 3 * 32) { kotlin.random.Random.nextFloat() * 0.1f }
            weights["conv2d_bias"] = FloatArray(32) { kotlin.random.Random.nextFloat() * 0.1f }
            weights["dense_kernel"] = FloatArray(1568 * 128) { kotlin.random.Random.nextFloat() * 0.1f }
            weights["dense_bias"] = FloatArray(128) { kotlin.random.Random.nextFloat() * 0.1f }
            weights["output_kernel"] = FloatArray(128 * 10) { kotlin.random.Random.nextFloat() * 0.1f }
            weights["output_bias"] = FloatArray(10) { kotlin.random.Random.nextFloat() * 0.1f }
            
            Log.d(TAG, "Extracted ${weights.size} weight layers")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting weights", e)
        }
        
        weights
    }

    /**
     * Serialize weights to NPZ format (simplified version)
     * NPZ is essentially a ZIP file containing numpy arrays
     */
    private fun serializeWeightsToNPZ(weights: Map<String, FloatArray>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val zipOutputStream = ZipOutputStream(byteArrayOutputStream)

        try {
            for ((name, array) in weights) {
                // Create entry for this array
                val entry = ZipEntry("$name.npy")
                zipOutputStream.putNextEntry(entry)

                // Write NPY header (simplified)
                val header = createNPYHeader(array.size)
                zipOutputStream.write(header)

                // Write array data as little-endian floats
                val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                for (value in array) {
                    buffer.putFloat(value)
                }
                zipOutputStream.write(buffer.array())
                zipOutputStream.closeEntry()
            }
        } finally {
            zipOutputStream.close()
        }

        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Create a simplified NPY header for float32 arrays
     */
    private fun createNPYHeader(arraySize: Int): ByteArray {
        val headerStr = "{'descr': '<f4', 'fortran_order': False, 'shape': ($arraySize,), }"
        val headerBytes = headerStr.toByteArray()
        
        // NPY magic number and version
        val magic = byteArrayOf(0x93.toByte(), 'N'.code.toByte(), 'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte())
        val version = byteArrayOf(1, 0)
        
        // Header length (2 bytes, little-endian)
        val headerLen = headerBytes.size + 1 // +1 for newline
        val headerLenBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(headerLen.toShort()).array()
        
        // Combine all parts
        val result = ByteArrayOutputStream()
        result.write(magic)
        result.write(version)
        result.write(headerLenBytes)
        result.write(headerBytes)
        result.write('\n'.code)
        
        return result.toByteArray()
    }

    /**
     * Upload model weights to the federated learning server
     */
    suspend fun uploadWeights(weights: Map<String, FloatArray>): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Serializing weights to NPZ format...")
            val npzData = serializeWeightsToNPZ(weights)
            
            Log.d(TAG, "Creating upload request...")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("client_id", clientId)
                .addFormDataPart(
                    "file", 
                    "weights.npz",
                    npzData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/upload_weights")
                .post(requestBody)
                .build()

            Log.d(TAG, "Uploading weights to server...")
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Upload successful: $responseBody")
                Result.success(responseBody)
            } else {
                val error = "Upload failed: ${response.code} ${response.message}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading weights", e)
            Result.failure(e)
        }
    }

    /**
     * Download latest aggregated weights from server
     */
    suspend fun downloadLatestWeights(): Result<Map<String, FloatArray>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading latest weights...")
            val request = Request.Builder()
                .url("$serverUrl/latest_weights")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d(TAG, "Downloaded ${responseBody.size} bytes")
                    val weights = deserializeNPZWeights(responseBody)
                    Log.d(TAG, "Deserialized ${weights.size} weight layers")
                    Result.success(weights)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else if (response.code == 404) {
                Log.w(TAG, "No aggregated weights available yet")
                Result.failure(Exception("No aggregated weights available"))
            } else {
                val error = "Download failed: ${response.code} ${response.message}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading weights", e)
            Result.failure(e)
        }
    }

    /**
     * Deserialize NPZ weights (simplified implementation)
     */
    private fun deserializeNPZWeights(npzData: ByteArray): Map<String, FloatArray> {
        val weights = mutableMapOf<String, FloatArray>()
        
        try {
            val zipInputStream = ZipInputStream(ByteArrayInputStream(npzData))
            var entry: ZipEntry?
            
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                val name = entry!!.name.removeSuffix(".npy")
                
                // Read NPY data (skip header for simplicity)
                val data = zipInputStream.readBytes()
                
                // Skip NPY header (find data start - this is simplified)
                var dataStart = 0
                for (i in 8 until data.size - 3) {
                    if (data[i] == '\n'.code.toByte()) {
                        dataStart = i + 1
                        break
                    }
                }
                
                // Convert bytes to float array
                val floatData = mutableListOf<Float>()
                val buffer = ByteBuffer.wrap(data, dataStart, data.size - dataStart).order(ByteOrder.LITTLE_ENDIAN)
                
                while (buffer.remaining() >= 4) {
                    floatData.add(buffer.float)
                }
                
                weights[name] = floatData.toFloatArray()
                zipInputStream.closeEntry()
            }
            zipInputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing NPZ weights", e)
        }
        
        return weights
    }

    /**
     * Trigger aggregation on the server
     */
    suspend fun triggerAggregation(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Triggering aggregation...")
            val request = Request.Builder()
                .url("$serverUrl/aggregate")
                .post("".toRequestBody())
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Aggregation triggered: $responseBody")
                Result.success(responseBody)
            } else {
                val error = "Aggregation failed: ${response.code} ${response.message}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering aggregation", e)
            Result.failure(e)
        }
    }

    /**
     * Get server status
     */
    suspend fun getServerStatus(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/status")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Result.success(responseBody)
            } else {
                val error = "Status check failed: ${response.code} ${response.message}"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Simulate local training (placeholder)
     * In a real implementation, you would retrain the model with local data
     */
    fun simulateLocalTraining(weights: Map<String, FloatArray>, epochs: Int = 5): Map<String, FloatArray> {
        Log.d(TAG, "Simulating $epochs epochs of local training...")
        
        val trainedWeights = mutableMapOf<String, FloatArray>()
        val learningRate = 0.01f
        
        for ((name, weight) in weights) {
            // Simulate gradient updates
            val updatedWeight = weight.map { w ->
                w + learningRate * (kotlin.random.Random.nextFloat() - 0.5f) * 0.02f
            }.toFloatArray()
            
            trainedWeights[name] = updatedWeight
        }
        
        Log.d(TAG, "Local training completed")
        return trainedWeights
    }
}