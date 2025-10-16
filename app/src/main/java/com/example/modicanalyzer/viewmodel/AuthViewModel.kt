package com.example.modicanalyzer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.modicanalyzer.data.model.AuthState
import com.example.modicanalyzer.data.repository.AuthRepository
import com.example.modicanalyzer.util.NetworkConnectivityObserver
import com.example.modicanalyzer.util.ValidationUtil
import com.example.modicanalyzer.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Authentication operations.
 * 
 * Manages:
 * - User signup (online/offline) with validation
 * - User login (online/offline) with validation
 * - Authentication state
 * - Network connectivity status
 * - Automatic sync trigger on network availability
 * - Input validation and error handling
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkObserver: NetworkConnectivityObserver,
    private val workManager: WorkManager
) : ViewModel() {
    
    /**
     * Current authentication state.
     * UI observes this to react to auth changes.
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Network connectivity status.
     * true = online, false = offline
     */
    val isOnline: StateFlow<Boolean> = networkObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkObserver.isCurrentlyConnected()
        )
    
    init {
        // Monitor network changes and trigger sync when coming online
        observeNetworkChanges()
        
        // Check if user is already logged in
        checkCurrentUser()
    }
    
    /**
     * Sign up a new user with validation.
     * 
     * @param email User's email
     * @param password User's password
     * @param confirmPassword Password confirmation
     * @param displayName User's display name (optional)
     */
    fun signUp(
        email: String, 
        password: String, 
        confirmPassword: String,
        displayName: String? = null
    ) {
        viewModelScope.launch {
            // Validate all input fields
            val validationResults = ValidationUtil.validateSignupForm(
                email = email.trim(),
                password = password,
                confirmPassword = confirmPassword,
                displayName = displayName?.trim()
            )
            
            // Check if any validation failed
            val firstError = validationResults.values.firstOrNull { !it.isValid }
            if (firstError != null) {
                _authState.value = AuthState.Error(firstError.errorMessage ?: "Invalid input")
                return@launch
            }
            
            // All validations passed, proceed with signup
            authRepository.signUp(
                email = email.trim(),
                password = password,
                displayName = displayName?.trim(),
                isOnline = isOnline.value
            ).collect { state ->
                _authState.value = state
                
                // If signup successful and online, trigger immediate sync
                if (state is AuthState.Success && isOnline.value) {
                    triggerSync(state.userId)
                }
            }
        }
    }
    
    /**
     * Log in an existing user with validation.
     * 
     * @param email User's email
     * @param password User's password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            // Validate input fields
            val validationResults = ValidationUtil.validateLoginForm(
                email = email.trim(),
                password = password
            )
            
            // Check if any validation failed
            val firstError = validationResults.values.firstOrNull { !it.isValid }
            if (firstError != null) {
                _authState.value = AuthState.Error(firstError.errorMessage ?: "Invalid input")
                return@launch
            }
            
            // Validation passed, proceed with login
            authRepository.login(
                email = email.trim(),
                password = password,
                isOnline = isOnline.value
            ).collect { state ->
                _authState.value = state
                
                // If login successful and online, trigger sync
                if (state is AuthState.Success && isOnline.value) {
                    triggerSync(state.userId)
                }
            }
        }
    }
    
    /**
     * Sign out the current user.
     */
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        
        // Cancel any ongoing sync work
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    /**
     * Check if a user is currently authenticated.
     */
    private fun checkCurrentUser() {
        val currentUser = authRepository.getCurrentFirebaseUser()
        if (currentUser != null) {
            _authState.value = AuthState.Success(
                userId = currentUser.uid,
                email = currentUser.email ?: "",
                isFirebaseAuth = true
            )
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Observe network connectivity changes.
     * Trigger sync when device comes online.
     */
    private fun observeNetworkChanges() {
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    // Device came online, trigger sync if user is authenticated
                    val currentState = _authState.value
                    if (currentState is AuthState.Success) {
                        triggerSync(currentState.userId)
                    }
                }
            }
        }
    }
    
    /**
     * Trigger background sync via WorkManager.
     * 
     * @param userId User's ID to sync
     */
    private fun triggerSync(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    SyncWorker.KEY_USER_ID to userId,
                    SyncWorker.KEY_SYNC_TYPE to SyncWorker.SYNC_TYPE_FULL
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Reset auth state to idle.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
