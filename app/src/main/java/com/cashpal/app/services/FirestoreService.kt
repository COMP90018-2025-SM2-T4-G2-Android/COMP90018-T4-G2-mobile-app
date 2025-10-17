package com.cashpal.app.services

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    
    // User Operations
    suspend fun createUser(user: com.cashpal.app.models.User): Result<String> {
        return try {
            val docRef = db.collection("users").document(user.id)
            docRef.set(user).await()
            Result.success(user.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<com.cashpal.app.models.User?> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(com.cashpal.app.models.User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Transaction Operations
    suspend fun createTransaction(transaction: com.cashpal.app.models.Transaction): Result<String> {
        return try {
            val docRef = db.collection("transactions").document()
            val transactionWithId = transaction.copy(id = docRef.id)
            docRef.set(transactionWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserTransactions(userId: String, limit: Int = 50): Result<List<com.cashpal.app.models.Transaction>> {
        return try {
            val query = db.collection("transactions")
                .where(
                    Filter.or(
                        Filter.equalTo("fromUserId", userId),
                        Filter.equalTo("toUserId", userId)
                    )
                )
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            val snapshot = query.get().await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.cashpal.app.models.Transaction::class.java)
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTransactionStatus(transactionId: String, status: com.cashpal.app.models.TransactionStatus): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "completedAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("transactions").document(transactionId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Contact Operations
    suspend fun createContact(contact: com.cashpal.app.models.Contact): Result<String> {
        return try {
            val docRef = db.collection("contacts").document()
            val contactWithId = contact.copy(id = docRef.id)
            docRef.set(contactWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserContacts(userId: String): Result<List<com.cashpal.app.models.Contact>> {
        return try {
            val query = db.collection("contacts")
                .whereEqualTo("userId", userId)
                .orderBy("isFrequent", Query.Direction.DESCENDING)
                .orderBy("lastTransactionDate", Query.Direction.DESCENDING)
            
            val snapshot = query.get().await()
            val contacts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.cashpal.app.models.Contact::class.java)
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateContact(contactId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("contacts").document(contactId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Payment Request Operations
    suspend fun createPaymentRequest(paymentRequest: com.cashpal.app.models.PaymentRequest): Result<String> {
        return try {
            val docRef = db.collection("paymentRequests").document()
            val requestWithId = paymentRequest.copy(id = docRef.id)
            docRef.set(requestWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserPaymentRequests(userId: String): Result<List<com.cashpal.app.models.PaymentRequest>> {
        return try {
            val query = db.collection("paymentRequests")
                .where(
                    Filter.or(
                        Filter.equalTo("fromUserId", userId),
                        Filter.equalTo("toUserId", userId)
                    )
                )
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            val snapshot = query.get().await()
            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.cashpal.app.models.PaymentRequest::class.java)
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePaymentRequestStatus(requestId: String, status: com.cashpal.app.models.PaymentRequestStatus): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "respondedAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("paymentRequests").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time listeners
    fun listenToUserTransactions(userId: String, callback: (List<com.cashpal.app.models.Transaction>) -> Unit): ListenerRegistration {
        return db.collection("transactions")
            .where(
                Filter.or(
                    Filter.equalTo("fromUserId", userId),
                    Filter.equalTo("toUserId", userId)
                )
            )
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.cashpal.app.models.Transaction::class.java)
                } ?: emptyList()
                callback(transactions)
            }
    }
    
    fun listenToUserBalance(userId: String, callback: (Double) -> Unit): ListenerRegistration {
        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(0.0)
                    return@addSnapshotListener
                }
                
                val balance = snapshot?.getDouble("balance") ?: 0.0
                callback(balance)
            }
    }
    
    // Payment Request Operations
    suspend fun getPaymentRequest(requestId: String): Result<com.cashpal.app.models.PaymentRequest?> {
        return try {
            val document = db.collection("paymentRequests").document(requestId).get().await()
            if (document.exists()) {
                val paymentRequest = document.toObject(com.cashpal.app.models.PaymentRequest::class.java)
                Result.success(paymentRequest)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Atomic Transaction Processing with Balance Updates
    suspend fun processTransactionWithBalanceUpdate(
        transaction: com.cashpal.app.models.Transaction,
        fromUserBalance: Double,
        toUserBalance: Double
    ): Result<String> {
        return try {
            val batch = db.batch()
            
            // Create transaction document
            val transactionRef = db.collection("transactions").document()
            val transactionWithId = transaction.copy(id = transactionRef.id)
            batch.set(transactionRef, transactionWithId)
            
            // Update sender balance
            val fromUserRef = db.collection("users").document(transaction.fromUserId)
            batch.update(fromUserRef, mapOf(
                "balance" to fromUserBalance,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ))
            
            // Update receiver balance
            val toUserRef = db.collection("users").document(transaction.toUserId)
            batch.update(toUserRef, mapOf(
                "balance" to toUserBalance,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ))
            
            // Commit the batch
            batch.commit().await()
            
            Result.success(transactionWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
