package com.roadguardian.app.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

sealed interface AuthState {
    data object Initial : AuthState
    data object Authenticating : AuthState
    data class Authenticated(val userId: String, val isAnonymous: Boolean) : AuthState
    data class Error(val message: String, val errorCode: String? = null) : AuthState
}

class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) {
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authMutex = Mutex()
    private var inFlightAuth: CompletableDeferred<Boolean>? = null

    val currentUserId: String?
        get() = firebaseAuth?.currentUser?.uid

    val isAuthenticated: Boolean
        get() = firebaseAuth?.currentUser != null

    val isAnonymous: Boolean
        get() = firebaseAuth?.currentUser?.isAnonymous == true

    init {
        if (firebaseAuth != null) {
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    userId = user.uid,
                    isAnonymous = user.isAnonymous
                )
            }
            firebaseAuth.addAuthStateListener { auth ->
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    _authState.value = AuthState.Authenticated(
                        userId = currentUser.uid,
                        isAnonymous = currentUser.isAnonymous
                    )
                } else {
                    _authState.value = AuthState.Initial
                }
            }
        }
    }

    /**
     * Diagnostic logging helper adhering strictly to privacy and diagnostic requirements.
     * Logs only boolean states and safe error details, never UIDs, tokens, or credentials.
     */
    private fun logAuthSuccess() {
        val user = firebaseAuth?.currentUser
        val authenticated = user != null
        val anonymous = user?.isAnonymous ?: false
        val uidPresent = !user?.uid.isNullOrBlank()

        Log.i(TAG, "Anonymous authentication SUCCESS")
        Log.i(TAG, "authenticated = $authenticated")
        Log.i(TAG, "anonymous = $anonymous")
        Log.i(TAG, "uidPresent = $uidPresent")
    }

    private fun logAuthFailure(exception: Exception) {
        Log.e(TAG, "Anonymous authentication FAILED")
        Log.e(TAG, "exception class: ${exception.javaClass.name}")
        Log.e(TAG, "exception message: ${exception.message}")
        if (exception is FirebaseAuthException) {
            Log.e(TAG, "Firebase error code: ${exception.errorCode}")
        }
    }

    /**
     * Ensures an anonymous user session is active. If a user is already signed in,
     * it immediately reports success. Otherwise, it executes signInAnonymously().
     */
    fun ensureAnonymousAuth(
        onSuccess: (userId: String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            val error = IllegalStateException("FirebaseAuth instance is unavailable")
            logAuthFailure(error)
            _authState.value = AuthState.Error(error.message ?: "Auth unavailable")
            onError(error)
            return
        }

        val existingUser = auth.currentUser
        if (existingUser != null) {
            logAuthSuccess()
            _authState.value = AuthState.Authenticated(
                userId = existingUser.uid,
                isAnonymous = existingUser.isAnonymous
            )
            onSuccess(existingUser.uid)
            return
        }

        Log.i(TAG, "Anonymous authentication started")
        _authState.value = AuthState.Authenticating

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    logAuthSuccess()
                    _authState.value = AuthState.Authenticated(
                        userId = user.uid,
                        isAnonymous = user.isAnonymous
                    )
                    onSuccess(user.uid)
                } else {
                    val ex = IllegalStateException("Sign in succeeded but user is null")
                    logAuthFailure(ex)
                    _authState.value = AuthState.Error(ex.message ?: "User is null")
                    onError(ex)
                }
            }
            .addOnFailureListener { exception ->
                val errorCode = (exception as? FirebaseAuthException)?.errorCode
                logAuthFailure(exception)
                _authState.value = AuthState.Error(
                    message = exception.message ?: "Authentication failed",
                    errorCode = errorCode
                )
                onError(exception)
            }
    }

    /**
     * Suspending function for asynchronous background operations (e.g. Firestore writes).
     * Returns true if authenticated, false otherwise. Does not block the caller thread.
     */
    suspend fun awaitAuthenticated(timeoutMs: Long = 10000L): Boolean {
        val auth = firebaseAuth ?: return false
        if (auth.currentUser != null) {
            return true
        }

        val deferred = authMutex.withLock {
            if (auth.currentUser != null) return true
            inFlightAuth?.let { return it.await() }

            val newDeferred = CompletableDeferred<Boolean>()
            inFlightAuth = newDeferred
            newDeferred
        }

        ensureAnonymousAuth(
            onSuccess = {
                deferred.complete(true)
            },
            onError = {
                deferred.complete(false)
            }
        )

        val success = withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: false

        authMutex.withLock {
            if (inFlightAuth === deferred) {
                inFlightAuth = null
            }
        }

        return success && auth.currentUser != null
    }
}
