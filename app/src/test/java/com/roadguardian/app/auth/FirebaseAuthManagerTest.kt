package com.roadguardian.app.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthManagerTest {

    @Test
    fun authManager_initializesSafelyWhenAuthUnavailable() {
        val manager = FirebaseAuthManager(firebaseAuth = null)

        assertNull(manager.currentUserId)
        assertFalse(manager.isAuthenticated)
        assertFalse(manager.isAnonymous)
        assertEquals(AuthState.Initial, manager.authState.value)

        var errorCalled = false
        manager.ensureAnonymousAuth(
            onSuccess = { },
            onError = { errorCalled = true }
        )

        assertTrue(errorCalled)
        assertTrue(manager.authState.value is AuthState.Error)
    }

    @Test
    fun authManager_awaitAuthenticatedReturnsFalseWhenAuthUnavailable() = runBlocking {
        val manager = FirebaseAuthManager(firebaseAuth = null)
        val ready = manager.awaitAuthenticated(timeoutMs = 100L)
        assertFalse(ready)
    }

    @Test
    fun authState_sealedHierarchyHandlesAllStates() {
        val initial: AuthState = AuthState.Initial
        val authenticating: AuthState = AuthState.Authenticating
        val authenticated: AuthState = AuthState.Authenticated("user-123", isAnonymous = true)
        val error: AuthState = AuthState.Error("Network failure", errorCode = "ERROR_NETWORK")

        assertTrue(initial is AuthState.Initial)
        assertTrue(authenticating is AuthState.Authenticating)
        assertTrue(authenticated is AuthState.Authenticated)
        assertEquals("user-123", (authenticated as AuthState.Authenticated).userId)
        assertTrue(authenticated.isAnonymous)
        assertTrue(error is AuthState.Error)
        assertEquals("Network failure", (error as AuthState.Error).message)
        assertEquals("ERROR_NETWORK", error.errorCode)
    }
}
