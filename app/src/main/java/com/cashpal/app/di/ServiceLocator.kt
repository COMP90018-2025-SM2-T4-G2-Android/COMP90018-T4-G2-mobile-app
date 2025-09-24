package com.cashpal.app.di

import android.content.Context
import com.cashpal.app.repository.CashPalRepository
import com.cashpal.app.services.FirebaseAuthService
import com.cashpal.app.services.FirebaseStorageService
import com.cashpal.app.services.FirestoreService

object ServiceLocator {
    private var _repository: CashPalRepository? = null
    
    fun initialize(context: Context) {
        val authService = FirebaseAuthService()
        val firestoreService = FirestoreService()
        val storageService = FirebaseStorageService()
        
        _repository = CashPalRepository(
            authService = authService,
            firestoreService = firestoreService,
            storageService = storageService
        )
    }
    
    fun getRepository(): CashPalRepository {
        return _repository ?: throw IllegalStateException("ServiceLocator not initialized")
    }
}
