package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    private fun getClientId(context: Context): String? {
        return try {
            val clientId = context.getString(R.string.default_web_client_id)
            if (clientId.isNotBlank()) clientId else null
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id string resource not found", e)
            null
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun openAccountSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to launch settings", e)
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAccountSelected: (email: String, displayName: String, photoUrl: String, idToken: String) -> Unit,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit = {},
        onNoCredentials: () -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            Log.e(TAG, "Activity not found in context")
            onAuthError("Unable to locate Activity for sign-in")
            return
        }

        val clientId = getClientId(context)
        if (clientId == null) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        // Credential Manager Option Exclusivity: exactly one credential option per request
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdCred.id
                    val displayName = googleIdCred.displayName ?: email.substringBefore("@")
                    val photoUrl = googleIdCred.profilePictureUri?.toString() ?: ""
                    val idToken = googleIdCred.idToken

                    onAccountSelected(email, displayName, photoUrl, idToken)

                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    Log.d(TAG, "Google Sign-In success in Firebase: ${Firebase.auth.currentUser?.email}")
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential format received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google accounts found on device/emulator: ${e.message}", e)
                onNoCredentials()
            } catch (e: Exception) {
                if (e.message?.contains("no credential", ignoreCase = true) == true) {
                    Log.w(TAG, "No credentials available on device/emulator: ${e.message}", e)
                    onNoCredentials()
                } else {
                    Log.e(TAG, "Google Sign-In failed", e)
                    onAuthError(e.localizedMessage ?: "Sign-in failed")
                }
            }
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAccountSelected: (email: String, displayName: String, photoUrl: String, idToken: String) -> Unit,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null) {
            val email = firebaseUser.email ?: "user@google.com"
            val name = firebaseUser.displayName ?: email.substringBefore("@")
            val photo = firebaseUser.photoUrl?.toString() ?: ""
            onAccountSelected(email, name, photo, "")
            onAuthSuccess()
            return
        }

        val clientId = getClientId(context)
        if (clientId == null) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdCred.id
                    val displayName = googleIdCred.displayName ?: email.substringBefore("@")
                    val photoUrl = googleIdCred.profilePictureUri?.toString() ?: ""
                    val idToken = googleIdCred.idToken

                    onAccountSelected(email, displayName, photoUrl, idToken)

                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        Firebase.auth.signInWithCredential(authCredential).await()
                    } catch (_: Exception) {}
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit
    ) {
        try {
            Firebase.auth.signOut()
        } catch (_: Exception) {}
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }
}
