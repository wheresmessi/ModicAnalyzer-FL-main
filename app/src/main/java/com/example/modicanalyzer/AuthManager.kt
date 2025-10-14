package com.example.modicanalyzer

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * AuthManager handles authentication state for the app. It uses FirebaseAuth when available
 * and falls back to a local SharedPreferences-based session for demo/offline usage.
 */
class AuthManager(private val context: Context) {
    companion object {
        private const val PREF_NAME = "modic_auth"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // FirebaseAuth instance (lazy). If Firebase is not configured or google-services.json is missing,
    // calls to FirebaseAuth will safely fail and fallback will be used.
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Perform a demo/local login (keeps existing behavior) — used as a fallback or for the demo user.
     */
    fun localLogin(email: String, name: String, role: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }

    /**
     * Logout from both Firebase (if present) and local session storage.
     */
    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // ignore
        }
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    /**
     * Returns true if either Firebase has a current user or local session marks logged-in.
     */
    fun isLoggedIn(): Boolean {
        val firebaseUser: FirebaseUser? = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
        return firebaseUser != null || prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(): String? = try { firebaseAuth?.currentUser?.email } catch (e: Exception) { null }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    /**
     * Sign in with Firebase using email and password. On success the caller should call
     * saveUserProfileIfNeeded to persist display name / role locally. Returns the FirebaseUser
     * on success via the provided callbacks.
     */
    fun signInWithFirebase(email: String, password: String, onSuccess: (FirebaseUser) -> Unit, onFailure: (Exception) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onFailure(IllegalStateException("FirebaseAuth not available"))
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) } ?: onFailure(IllegalStateException("No user returned"))
            }
            .addOnFailureListener { ex -> onFailure(ex) }
    }

    fun createUserWithFirebase(email: String, password: String, onSuccess: (FirebaseUser) -> Unit, onFailure: (Exception) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onFailure(IllegalStateException("FirebaseAuth not available"))
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) } ?: onFailure(IllegalStateException("No user returned"))
            }
            .addOnFailureListener { ex -> onFailure(ex) }
    }

    /**
     * Persist basic profile information locally so UI can show name/role. This does not replace
     * a proper backend user profile; it's a small convenience for the demo app.
     */
    fun saveUserProfileIfNeeded(email: String, name: String, role: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }

    fun getDemoUserInfo(): UserInfo = UserInfo(
        email = "demo@modicanalyzer.com",
        name = "Demo User",
        role = "Patient"
    )

    data class UserInfo(
        val email: String,
        val name: String,
        val role: String
    )
}