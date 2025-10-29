package com.example.modicanalyzer.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore Helper - Simple implementation for user data & image entries
 * 
 * Database: (default) - FREE Spark Plan compatible
 * 
 * Structure:
 * /users/{userId}
 *   - name, email, createdAt, profileImage, role
 *   /data_entries/{entryId}
 *     - imageUrl, caption, metadata, createdAt
 */
@Singleton
class FirestoreHelper @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "FirestoreHelper"
        private const val USERS_COLLECTION = "users"
        private const val DATA_ENTRIES_COLLECTION = "data_entries"
    }
    
    // ============================================
    // USER PROFILE OPERATIONS
    // ============================================
    
    /**
     * Create or update user profile in Firestore
     * 
     * @param userId Firebase Auth UID
     * @param name User's full name
     * @param email User's email
     * @param role Optional role (e.g., "student", "admin", "patient", "doctor")
     * @param profileImageUrl Optional profile image URL from Firebase Storage
     */
    suspend fun createOrUpdateUserProfile(
        userId: String,
        name: String,
        email: String,
        role: String? = null,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return try {
            val userProfile = buildMap {
                put("name", name)
                put("email", email)
                if (role != null) put("role", role)
                if (profileImageUrl != null) put("profileImage", profileImageUrl)
                put("updatedAt", FieldValue.serverTimestamp())
                
                // Only set createdAt on first creation (won't overwrite if exists)
                put("createdAt", FieldValue.serverTimestamp())
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userProfile, SetOptions.merge()) // merge = update existing fields
                .await()
            
            Log.d(TAG, "✅ User profile created/updated: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating/updating user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile from Firestore
     * 
     * @param userId Firebase Auth UID
     * @return User data as Map, or null if not found
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                Log.d(TAG, "✅ User profile retrieved: $userId")
                Result.success(document.data)
            } else {
                Log.w(TAG, "⚠️ User profile not found: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update user's profile image URL
     * 
     * @param userId Firebase Auth UID
     * @param imageUrl URL from Firebase Storage
     */
    suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "profileImage" to imageUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Profile image updated: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating profile image", e)
            Result.failure(e)
        }
    }
    
    // ============================================
    // DATA ENTRIES (Images/Scans/Uploads)
    // ============================================
    
    /**
     * Add a new data entry (image upload with metadata)
     * 
     * @param userId Firebase Auth UID
     * @param imageUrl URL from Firebase Storage
     * @param caption Short description
     * @param metadata Additional data (resolution, type, analysis results, etc.)
     * @return Entry ID if successful
     */
    suspend fun addDataEntry(
        userId: String,
        imageUrl: String,
        caption: String?,
        metadata: Map<String, Any>? = null
    ): Result<String> {
        return try {
            val entry = buildMap {
                put("imageUrl", imageUrl)
                if (caption != null) put("caption", caption)
                if (metadata != null) put("metadata", metadata)
                put("createdAt", FieldValue.serverTimestamp())
            }
            
            val documentRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DATA_ENTRIES_COLLECTION)
                .add(entry) // auto-generate ID
                .await()
            
            Log.d(TAG, "✅ Data entry added: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding data entry", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all data entries for a user
     * 
     * @param userId Firebase Auth UID
     * @return List of entries (newest first)
     */
    suspend fun getUserDataEntries(userId: String): Result<List<DataEntry>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DATA_ENTRIES_COLLECTION)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val entries = snapshot.documents.mapNotNull { doc ->
                try {
                    DataEntry(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        caption = doc.getString("caption"),
                        metadata = doc.get("metadata") as? Map<String, Any>,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Skipping invalid entry: ${doc.id}")
                    null
                }
            }
            
            Log.d(TAG, "✅ Retrieved ${entries.size} data entries for user: $userId")
            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting data entries", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific data entry
     * 
     * @param userId Firebase Auth UID
     * @param entryId Entry document ID
     */
    suspend fun getDataEntry(userId: String, entryId: String): Result<DataEntry?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DATA_ENTRIES_COLLECTION)
                .document(entryId)
                .get()
                .await()
            
            if (document.exists()) {
                val entry = DataEntry(
                    id = document.id,
                    imageUrl = document.getString("imageUrl") ?: "",
                    caption = document.getString("caption"),
                    metadata = document.get("metadata") as? Map<String, Any>,
                    createdAt = document.getTimestamp("createdAt")?.toDate()?.time
                )
                Result.success(entry)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting data entry", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update a data entry
     * 
     * @param userId Firebase Auth UID
     * @param entryId Entry document ID
     * @param updates Fields to update
     */
    suspend fun updateDataEntry(
        userId: String,
        entryId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            val updateData = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DATA_ENTRIES_COLLECTION)
                .document(entryId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "✅ Data entry updated: $entryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating data entry", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a data entry
     * 
     * @param userId Firebase Auth UID
     * @param entryId Entry document ID
     */
    suspend fun deleteDataEntry(userId: String, entryId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DATA_ENTRIES_COLLECTION)
                .document(entryId)
                .delete()
                .await()
            
            Log.d(TAG, "✅ Data entry deleted: $entryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting data entry", e)
            Result.failure(e)
        }
    }
}

/**
 * Data class representing a user's data entry
 */
data class DataEntry(
    val id: String,
    val imageUrl: String,
    val caption: String? = null,
    val metadata: Map<String, Any>? = null,
    val createdAt: Long? = null
)
