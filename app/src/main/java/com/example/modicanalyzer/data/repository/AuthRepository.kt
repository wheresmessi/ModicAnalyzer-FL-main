package com.example.modicanalyzer.data.repository

import com.example.modicanalyzer.data.local.dao.UserDao
import com.example.modicanalyzer.data.local.entity.UserEntity
import com.example.modicanalyzer.data.model.AuthState
import com.example.modicanalyzer.data.model.SyncStatus
import com.example.modicanalyzer.data.remote.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Authentication operations.
 * 
 * Implements offline-first authentication:
 * 1. Offline Mode: Store credentials locally in Room with synced=false
 * 2. Online Mode: Authenticate with Firebase and sync local user
 * 3. Hybrid Mode: Support both offline and online users simultaneously
 * 
 * Features:
 * - Offline signup/login using local SQLite
 * - Firebase authentication when online
 * - Automatic sync of offline users when network becomes available
 * - Password hashing for security
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseAuth: FirebaseAuth
) {
    
    /**
     * Sign up a new user.
     * 
     * Offline Flow:
     * 1. Create user in local Room database with PENDING sync status
     * 2. Hash password for local authentication
     * 3. Generate temporary UUID as userId
     * 
     * Online Flow:
     * 1. Create user in Firebase Auth
     * 2. Store user in Room with Firebase UID
     * 3. Sync profile to Firestore
     * 4. Mark as SYNCED
     * 
     * @param email User's email
     * @param password User's password
     * @param displayName User's display name (optional)
     * @param isOnline Whether device has network connectivity
     * @return Flow emitting AuthState changes
     */
    fun signUp(
        email: String,
        password: String,
        displayName: String?,
        isOnline: Boolean
    ): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        
        try {
            // Check if email already exists locally
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                emit(AuthState.Error("User with this email already exists"))
                return@flow
            }
            
            if (isOnline) {
                // Online signup: Use Firebase Auth
                try {
                    val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                    val firebaseUser = authResult.user
                    
                    if (firebaseUser != null) {
                        // Update display name in Firebase
                        if (displayName != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                            firebaseUser.updateProfile(profileUpdates).await()
                        }
                        
                        // Create user entity for local database
                        val userEntity = UserEntity(
                            userId = firebaseUser.uid,
                            email = email,
                            passwordHash = hashPassword(password),
                            displayName = displayName,
                            isFirebaseAuth = true,
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        
                        // Save to local database
                        userDao.insertUser(userEntity)
                        
                        // Sync profile to Firestore
                        firestoreDataSource.syncUserProfile(
                            userId = firebaseUser.uid,
                            email = email,
                            displayName = displayName
                        )
                        
                        emit(AuthState.Success(
                            userId = firebaseUser.uid,
                            email = email,
                            isFirebaseAuth = true
                        ))
                    } else {
                        emit(AuthState.Error("Failed to create user"))
                    }
                } catch (e: Exception) {
                    // If Firebase auth fails, fall back to offline mode
                    emit(AuthState.Error("Online signup failed: ${e.message}. Try offline mode."))
                }
            } else {
                // Offline signup: Store in local database
                val userId = UUID.randomUUID().toString()
                val userEntity = UserEntity(
                    userId = userId,
                    email = email,
                    passwordHash = hashPassword(password),
                    displayName = displayName,
                    isFirebaseAuth = false,
                    syncStatus = SyncStatus.PENDING
                )
                
                userDao.insertUser(userEntity)
                
                emit(AuthState.Success(
                    userId = userId,
                    email = email,
                    isFirebaseAuth = false
                ))
            }
        } catch (e: Exception) {
            emit(AuthState.Error("Signup failed: ${e.message}"))
        }
    }
    
    /**
     * Log in an existing user.
     * 
     * Flow:
     * 1. Check if user exists in local database
     * 2. If online, try Firebase authentication
     * 3. If offline or Firebase fails, use local authentication
     * 
     * @param email User's email
     * @param password User's password
     * @param isOnline Whether device has network connectivity
     * @return Flow emitting AuthState changes
     */
    fun login(
        email: String,
        password: String,
        isOnline: Boolean
    ): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        
        try {
            val localUser = userDao.getUserByEmail(email)
            
            if (isOnline) {
                // Try Firebase authentication
                try {
                    val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                    val firebaseUser = authResult.user
                    
                    if (firebaseUser != null) {
                        // Update or create local user
                        val userEntity = localUser?.copy(
                            userId = firebaseUser.uid,
                            isFirebaseAuth = true,
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = System.currentTimeMillis()
                        ) ?: UserEntity(
                            userId = firebaseUser.uid,
                            email = email,
                            passwordHash = hashPassword(password),
                            displayName = firebaseUser.displayName,
                            isFirebaseAuth = true,
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        
                        userDao.insertUser(userEntity)
                        
                        emit(AuthState.Success(
                            userId = firebaseUser.uid,
                            email = email,
                            isFirebaseAuth = true
                        ))
                    } else {
                        emit(AuthState.Error("Login failed"))
                    }
                } catch (e: Exception) {
                    // Firebase login failed, try offline login
                    if (localUser != null && verifyPassword(password, localUser.passwordHash)) {
                        emit(AuthState.Success(
                            userId = localUser.userId,
                            email = localUser.email,
                            isFirebaseAuth = false
                        ))
                    } else {
                        emit(AuthState.Error("Login failed: ${e.message}"))
                    }
                }
            } else {
                // Offline login: Check local database
                if (localUser != null && verifyPassword(password, localUser.passwordHash)) {
                    emit(AuthState.Success(
                        userId = localUser.userId,
                        email = localUser.email,
                        isFirebaseAuth = localUser.isFirebaseAuth
                    ))
                } else {
                    emit(AuthState.Error("Invalid credentials or user not found"))
                }
            }
        } catch (e: Exception) {
            emit(AuthState.Error("Login failed: ${e.message}"))
        }
    }
    
    /**
     * Sync offline users to Firebase when network becomes available.
     * Called by SyncWorker.
     * 
     * @return List of user IDs that were successfully synced
     */
    suspend fun syncOfflineUsers(): List<String> {
        val unsyncedUsers = userDao.getUnsyncedUsers()
        val syncedUserIds = mutableListOf<String>()
        
        unsyncedUsers.forEach { user ->
            try {
                // Update sync status to SYNCING
                userDao.updateSyncStatus(user.userId, SyncStatus.SYNCING, null)
                
                // This user was created offline, we can't migrate them to Firebase Auth
                // without their password. Instead, just mark them as synced locally.
                // In a production app, you'd need to handle this during their next login.
                
                // Sync profile to Firestore
                val result = firestoreDataSource.syncUserProfile(
                    userId = user.userId,
                    email = user.email,
                    displayName = user.displayName
                )
                
                if (result.isSuccess) {
                    userDao.updateSyncStatus(
                        user.userId,
                        SyncStatus.SYNCED,
                        System.currentTimeMillis()
                    )
                    syncedUserIds.add(user.userId)
                } else {
                    userDao.updateSyncStatus(user.userId, SyncStatus.FAILED, null)
                }
            } catch (e: Exception) {
                userDao.updateSyncStatus(user.userId, SyncStatus.FAILED, null)
            }
        }
        
        return syncedUserIds
    }
    
    /**
     * Get current authenticated user from Firebase.
     */
    fun getCurrentFirebaseUser() = firebaseAuth.currentUser
    
    /**
     * Sign out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
    
    /**
     * Get user by ID from local database.
     */
    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }
    
    /**
     * Observe user changes.
     */
    fun observeUser(userId: String): Flow<UserEntity?> {
        return userDao.observeUserById(userId)
    }
    
    /**
     * Hash password using SHA-256 for local storage.
     * Note: In production, use bcrypt or argon2 instead.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify password against stored hash.
     */
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
