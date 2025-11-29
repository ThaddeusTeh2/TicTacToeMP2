package com.dx.tictactoemp.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.dx.tictactoemp.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class FirebaseAuthService(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun currentUserFlow(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let {
                User(
                    id = it.uid,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )
            }
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): Result<User> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("YOUR_WEB_CLIENT_ID") // TODO: Replace with actual Web Client ID
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()

                    val firebaseUser = authResult.user
                        ?: return Result.failure(Exception("Sign-in failed: No user"))

                    val user = User(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis()
                    )

                    upsertUser(user)
                    Result.success(user)
                } catch (e: GoogleIdTokenParsingException) {
                    Result.failure(e)
                }
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id = firebaseUser.uid,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            createdAt = System.currentTimeMillis()
        )
    }

    private suspend fun upsertUser(user: User) {
        try {
            val userDoc = firestore.collection("users").document(user.id)
            val userData = hashMapOf(
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            userDoc.set(userData).await()
        } catch (e: Exception) {
            // Log but don't fail sign-in if user doc creation fails
            e.printStackTrace()
        }
    }
}

