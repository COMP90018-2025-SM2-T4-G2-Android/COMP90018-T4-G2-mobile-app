package com.cashpal.app.auth

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

object Reauth {
    private const val TAG = "Reauth"

    /** Side-effect-free password verification. Does NOT change session or navigate. */
    fun reauthenticate(email: String, password: String): Flow<Result<Unit>> = flow {
        runCatching {
            val user = FirebaseAuth.getInstance().currentUser
                ?: error("Not signed in")
            val cred = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(cred).await()
        }.onSuccess {
            emit(Result.success(Unit))
        }.onFailure { e ->
            Log.e(TAG, "reauthenticate failed", e)
            emit(Result.failure(e))
        }
    }
}
