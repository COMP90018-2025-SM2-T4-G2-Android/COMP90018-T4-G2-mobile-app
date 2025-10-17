package com.cashpal.app.services

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest

class FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun isUserSignedIn(): Boolean = auth.currentUser != null
    
    // Email/Password Authentication
    fun signUpWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }
    
    fun signInWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }
    
    // Google Sign In
    fun signInWithGoogle(idToken: String): Task<AuthResult> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential)
    }
    
    // Phone Authentication
    fun signInWithPhone(credential: PhoneAuthCredential): Task<AuthResult> {
        return auth.signInWithCredential(credential)
    }
    
    // Profile Management
    fun updateUserProfile(displayName: String?, photoUri: String?): Task<Void> {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .apply {
                displayName?.let { setDisplayName(it) }
                photoUri?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }
            .build()
        
        return auth.currentUser?.updateProfile(profileUpdates) ?: 
            throw IllegalStateException("No user signed in")
    }
    
    fun updatePassword(newPassword: String): Task<Void> {
        return auth.currentUser?.updatePassword(newPassword) ?:
            throw IllegalStateException("No user signed in")
    }
    
    fun sendPasswordResetEmail(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun deleteUser(): Task<Void> {
        return auth.currentUser?.delete() ?:
            throw IllegalStateException("No user signed in")
    }
}
