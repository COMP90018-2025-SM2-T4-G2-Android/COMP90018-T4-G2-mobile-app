package com.cashpal.app.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class CashPalRepository(
    private val authService: com.cashpal.app.services.FirebaseAuthService,
    private val firestoreService: com.cashpal.app.services.FirestoreService,
    private val storageService: com.cashpal.app.services.FirebaseStorageService
) {
    
    // Authentication
    fun getCurrentUser(): FirebaseUser? = authService.getCurrentUser()
    fun isUserSignedIn(): Boolean = authService.isUserSignedIn()
    
    suspend fun signUpWithEmail(email: String, password: String, displayName: String) = flow {
        try {
            val authResult = authService.signUpWithEmail(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                // Create user profile in Firestore
                val userProfile = com.cashpal.app.models.User(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = displayName,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                val result = firestoreService.createUser(userProfile)
                if (result.isSuccess) {
                    emit(Result.success(user))
                } else {
                    emit(Result.failure(result.exceptionOrNull() ?: Exception("Failed to create user profile")))
                }
            } else {
                emit(Result.failure(Exception("Failed to create user")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String) = flow {
        try {
            val authResult = authService.signInWithEmail(email, password).await()
            emit(Result.success(authResult.user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun signOut() {
        authService.signOut()
    }
    
    // User Profile Management
    suspend fun getUserProfile(userId: String) = flow {
        try {
            val result = firestoreService.getUser(userId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) = flow {
        try {
            val result = firestoreService.updateUser(userId, updates)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun uploadProfileImage(userId: String, imageUri: Uri) = flow {
        try {
            val result = storageService.uploadProfileImage(userId, imageUri)
            if (result.isSuccess) {
                // Update user profile with new image URL
                val updateResult = firestoreService.updateUser(userId, mapOf<String, Any>(
                    "avatarUrl" to (result.getOrNull() ?: ""),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
                emit(updateResult)
            } else {
                emit(Result.failure(result.exceptionOrNull() ?: Exception("Failed to upload image")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    // Transaction Management
    suspend fun createTransaction(transaction: com.cashpal.app.models.Transaction) = flow {
        try {
            val result = firestoreService.createTransaction(transaction)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserTransactions(userId: String, limit: Int = 50) = flow {
        try {
            val result = firestoreService.getUserTransactions(userId, limit)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateTransactionStatus(transactionId: String, status: com.cashpal.app.models.TransactionStatus) = flow {
        try {
            val result = firestoreService.updateTransactionStatus(transactionId, status)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    // Contact Management
    suspend fun createContact(contact: com.cashpal.app.models.Contact) = flow {
        try {
            val result = firestoreService.createContact(contact)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserContacts(userId: String) = flow {
        try {
            val result = firestoreService.getUserContacts(userId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updateContact(contactId: String, updates: Map<String, Any>) = flow {
        try {
            val result = firestoreService.updateContact(contactId, updates)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    // Payment Request Management
    suspend fun createPaymentRequest(paymentRequest: com.cashpal.app.models.PaymentRequest) = flow {
        try {
            val result = firestoreService.createPaymentRequest(paymentRequest)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserPaymentRequests(userId: String) = flow {
        try {
            val result = firestoreService.getUserPaymentRequests(userId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun updatePaymentRequestStatus(requestId: String, status: com.cashpal.app.models.PaymentRequestStatus) = flow {
        try {
            val result = firestoreService.updatePaymentRequestStatus(requestId, status)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    // Real-time Listeners
    fun listenToUserTransactions(userId: String, callback: (List<com.cashpal.app.models.Transaction>) -> Unit): ListenerRegistration {
        return firestoreService.listenToUserTransactions(userId, callback)
    }
    
    fun listenToUserBalance(userId: String, callback: (Double) -> Unit): ListenerRegistration {
        return firestoreService.listenToUserBalance(userId, callback)
    }
    
    // Storage Operations
    suspend fun uploadReceipt(userId: String, transactionId: String, receiptUri: Uri) = flow {
        try {
            val result = storageService.uploadReceiptImage(userId, transactionId, receiptUri)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun uploadQRCode(userId: String, qrCodeUri: Uri) = flow {
        try {
            val result = storageService.uploadQRCode(userId, qrCodeUri)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

