package com.cashpal.app.services

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.tasks.await

class FirebaseStorageService {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    // Profile Image Upload
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val imageRef = storageRef.child("profile_images/$userId.jpg")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // QR Code Image Upload
    suspend fun uploadQRCode(userId: String, qrCodeUri: Uri): Result<String> {
        return try {
            val qrRef = storageRef.child("qr_codes/$userId/${System.currentTimeMillis()}.jpg")
            val uploadTask = qrRef.putFile(qrCodeUri).await()
            val downloadUrl = qrRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Receipt Image Upload
    suspend fun uploadReceiptImage(userId: String, transactionId: String, receiptUri: Uri): Result<String> {
        return try {
            val receiptRef = storageRef.child("receipts/$userId/$transactionId.jpg")
            val uploadTask = receiptRef.putFile(receiptUri).await()
            val downloadUrl = receiptRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Document Upload (for verification, etc.)
    suspend fun uploadDocument(userId: String, documentType: String, documentUri: Uri): Result<String> {
        return try {
            val docRef = storageRef.child("documents/$userId/$documentType/${System.currentTimeMillis()}.pdf")
            val uploadTask = docRef.putFile(documentUri).await()
            val downloadUrl = docRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete file
    suspend fun deleteFile(filePath: String): Result<Unit> {
        return try {
            val fileRef = storageRef.child(filePath)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get download URL
    suspend fun getDownloadUrl(filePath: String): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get file metadata
    suspend fun getFileMetadata(filePath: String): Result<com.google.firebase.storage.StorageMetadata> {
        return try {
            val fileRef = storageRef.child(filePath)
            val metadata = fileRef.metadata.await()
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
