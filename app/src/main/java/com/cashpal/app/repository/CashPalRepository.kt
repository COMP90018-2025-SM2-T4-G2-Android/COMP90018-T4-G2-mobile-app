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
                // Create user profile in Firestore with default values
                val userProfile = com.cashpal.app.models.User(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = displayName,
                    phoneNumber = user.phoneNumber,
                    avatarUrl = user.photoUrl?.toString(),
                    balance = 0.0, // Start with 0 balance
                    currency = "AUD", // Default currency
                    isVerified = user.isEmailVerified,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                val result = firestoreService.createUser(userProfile)
                if (result.isSuccess) {
                    // Initialize empty collections for new user
                    initializeNewUserCollections(user.uid)
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
    
    suspend fun resetPassword(email: String) = flow {
        try {
            authService.sendPasswordResetEmail(email).await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun signInWithGoogle(activity: android.app.Activity, onIntentReady: (android.content.Intent) -> Unit) {
        try {
            // Get Google Sign-In client with proper configuration
            val webClientId = activity.getString(com.cashpal.app.R.string.default_web_client_id)
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()
            
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
            
            // Sign out any existing user first to ensure clean state
            googleSignInClient.signOut().addOnCompleteListener {
                // Launch Google Sign-In intent
                val signInIntent = googleSignInClient.signInIntent
                onIntentReady(signInIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("CashPalRepository", "Google Sign-In setup failed", e)
        }
    }
    
    suspend fun handleGoogleSignInResult(data: android.content.Intent) = flow {
        try {
            android.util.Log.d("CashPalRepository", "Handling Google Sign-In result")
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            
            android.util.Log.d("CashPalRepository", "Google account retrieved: ${account?.email}")
            val idToken = account?.idToken
            if (idToken != null) {
                android.util.Log.d("CashPalRepository", "ID token received, authenticating with Firebase")
                val authResult = authService.signInWithGoogle(idToken).await()
                val user = authResult.user
                
                if (user != null) {
                    android.util.Log.d("CashPalRepository", "Firebase user created: ${user.uid}")
                    val userModel = com.cashpal.app.models.User(
                        id = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "",
                        phoneNumber = user.phoneNumber,
                        avatarUrl = user.photoUrl?.toString(),
                        balance = 0.0,
                        currency = "AUD",
                        isVerified = user.isEmailVerified,
                        createdAt = com.google.firebase.Timestamp.now(),
                        updatedAt = com.google.firebase.Timestamp.now()
                    )
                    
                    // Try to create user in Firestore, but don't fail if it already exists
                    try {
                        android.util.Log.d("CashPalRepository", "Creating user in Firestore")
                        firestoreService.createUser(userModel)
                        android.util.Log.d("CashPalRepository", "User created successfully in Firestore")
                        // Initialize empty collections for new user
                        initializeNewUserCollections(user.uid)
                    } catch (e: Exception) {
                        android.util.Log.w("CashPalRepository", "User might already exist in Firestore", e)
                        // User might already exist, continue anyway
                    }
                    
                    emit(Result.success(userModel))
                } else {
                    android.util.Log.e("CashPalRepository", "Failed to get user from Firebase auth result")
                    emit(Result.failure(Exception("Failed to get user from Google Sign-In")))
                }
            } else {
                android.util.Log.e("CashPalRepository", "Failed to get ID token from Google account")
                emit(Result.failure(Exception("Failed to get ID token from Google Sign-In")))
            }
        } catch (e: Exception) {
            android.util.Log.e("CashPalRepository", "Google Sign-In result handling failed", e)
            emit(Result.failure(e))
        }
    }
    
    // User Profile Management
    suspend fun createUser(user: com.cashpal.app.models.User) = flow {
        try {
            val result = firestoreService.createUser(user)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
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
            // If transaction is completed and involves users (not system-to-system), update balances
            if (transaction.status == com.cashpal.app.models.TransactionStatus.COMPLETED && 
                transaction.fromUserId != "system" && transaction.toUserId != "system") {
                // Use processTransactionWithBalanceUpdate for user-to-user transactions
                val fromUserResult = firestoreService.getUser(transaction.fromUserId)
                val toUserResult = firestoreService.getUser(transaction.toUserId)
                
                if (fromUserResult.isSuccess && toUserResult.isSuccess) {
                    val fromUser = fromUserResult.getOrNull()
                    val toUser = toUserResult.getOrNull()
                    
                    if (fromUser != null && toUser != null) {
                        val result = firestoreService.processTransactionWithBalanceUpdate(
                            transaction = transaction,
                            fromUserBalance = fromUser.balance - transaction.amount,
                            toUserBalance = toUser.balance + transaction.amount
                        )
                        emit(result)
                        return@flow
                    }
                }
            }
            
            // For system transactions or if balance update fails, just create the transaction
            val result = firestoreService.createTransaction(transaction)
            if (result.isSuccess && transaction.status == com.cashpal.app.models.TransactionStatus.COMPLETED) {
                // Update balance for system transactions (e.g., welcome bonus)
                if (transaction.fromUserId == "system" && transaction.toUserId != "system") {
                    val toUserResult = firestoreService.getUser(transaction.toUserId)
                    toUserResult.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                val newBalance = user.balance + transaction.amount
                                firestoreService.updateUser(transaction.toUserId, mapOf(
                                    "balance" to newBalance,
                                    "updatedAt" to com.google.firebase.Timestamp.now()
                                ))
                            }
                        },
                        onFailure = { }
                    )
                }
            }
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
    
    // Transaction Processing with Balance Updates
    suspend fun processTransaction(
        fromUserId: String,
        toUserId: String,
        amount: Double,
        description: String,
        category: com.cashpal.app.models.TransactionCategory = com.cashpal.app.models.TransactionCategory.OTHER
    ) = flow {
        try {
            // Get both users' current balances
            val fromUserResult = firestoreService.getUser(fromUserId)
            val toUserResult = firestoreService.getUser(toUserId)
            
            if (fromUserResult.isFailure || toUserResult.isFailure) {
                emit(Result.failure(Exception("Failed to get user information")))
                return@flow
            }
            
            val fromUser = fromUserResult.getOrNull()
            val toUser = toUserResult.getOrNull()
            
            if (fromUser == null || toUser == null) {
                emit(Result.failure(Exception("User not found")))
                return@flow
            }
            
            // Check if sender has sufficient balance
            if (fromUser.balance < amount) {
                emit(Result.failure(Exception("Insufficient balance")))
                return@flow
            }
            
            // Create transaction
            val transaction = com.cashpal.app.models.Transaction(
                fromUserId = fromUserId,
                toUserId = toUserId,
                amount = amount,
                currency = fromUser.currency,
                description = description,
                category = category,
                status = com.cashpal.app.models.TransactionStatus.COMPLETED,
                type = com.cashpal.app.models.TransactionType.TRANSFER,
                createdAt = com.google.firebase.Timestamp.now(),
                completedAt = com.google.firebase.Timestamp.now()
            )
            
            // Process transaction with atomic balance updates
            val result = firestoreService.processTransactionWithBalanceUpdate(
                transaction = transaction,
                fromUserBalance = fromUser.balance - amount,
                toUserBalance = toUser.balance + amount
            )
            
            emit(result)
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun acceptPaymentRequest(requestId: String) = flow {
        try {
            // Get payment request
            val requestResult = firestoreService.getPaymentRequest(requestId)
            if (requestResult.isFailure) {
                emit(Result.failure(Exception("Payment request not found")))
                return@flow
            }
            
            val paymentRequest = requestResult.getOrNull()
            if (paymentRequest == null || paymentRequest.status != com.cashpal.app.models.PaymentRequestStatus.PENDING) {
                emit(Result.failure(Exception("Invalid payment request")))
                return@flow
            }
            
            // Process the transaction
            processTransaction(
                fromUserId = paymentRequest.fromUserId,
                toUserId = paymentRequest.toUserId,
                amount = paymentRequest.amount,
                description = paymentRequest.description
            ).collect { transactionResult ->
                if (transactionResult.isSuccess) {
                    // Update payment request status
                    updatePaymentRequestStatus(requestId, com.cashpal.app.models.PaymentRequestStatus.ACCEPTED).collect { updateResult ->
                        emit(updateResult)
                    }
                } else {
                    emit(transactionResult)
                }
            }
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Initialize empty collections for new users
     * This ensures new users have proper database structure
     */
    private suspend fun initializeNewUserCollections(userId: String) {
        try {
            android.util.Log.d("CashPalRepository", "Initializing collections for new user: $userId")
            
            // Create a welcome transaction to give users a starting balance
            // This is optional - you can remove this if you want users to start with truly empty accounts
            val welcomeTransaction = com.cashpal.app.models.Transaction(
                id = "",
                fromUserId = "system", // System-generated transaction
                toUserId = userId,
                amount = 100.0, // Welcome bonus of $100 AUD
                currency = "AUD",
                description = "Welcome to CashPal! Here's your starting balance.",
                category = com.cashpal.app.models.TransactionCategory.OTHER,
                status = com.cashpal.app.models.TransactionStatus.COMPLETED,
                type = com.cashpal.app.models.TransactionType.RECEIPT,
                createdAt = com.google.firebase.Timestamp.now(),
                completedAt = com.google.firebase.Timestamp.now()
            )
            
            // Add the welcome transaction (this will also update the user's balance)
            createTransaction(welcomeTransaction).collect { result ->
                result.fold(
                    onSuccess = {
                        android.util.Log.d("CashPalRepository", "Welcome transaction created for new user")
                    },
                    onFailure = { error ->
                        android.util.Log.w("CashPalRepository", "Failed to create welcome transaction", error)
                        // Don't fail the entire signup process if welcome transaction fails
                    }
                )
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CashPalRepository", "Failed to initialize collections for new user", e)
            // Don't fail the entire signup process if initialization fails
        }
    }
}

