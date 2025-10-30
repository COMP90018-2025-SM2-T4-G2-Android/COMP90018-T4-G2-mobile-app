package com.cashpal.app.di

import android.content.Context
import com.cashpal.app.repository.CashPalRepository
import com.cashpal.app.repository.SecurityRepository
import com.cashpal.app.services.FirebaseAuthService
import com.cashpal.app.services.FirebaseStorageService
import com.cashpal.app.services.FirestoreService

object ServiceLocator {
    private lateinit var authService: FirebaseAuthService
    private lateinit var firestoreService: FirestoreService
    private lateinit var storageService: FirebaseStorageService

    private var _repository: CashPalRepository? = null
    private var _securityRepository: SecurityRepository? = null
    
    fun initialize(context: Context) {
        authService = FirebaseAuthService()
        firestoreService = FirestoreService()
        storageService = FirebaseStorageService()
        
        _repository = CashPalRepository(
            authService = authService,
            firestoreService = firestoreService,
            storageService = storageService
        )

        _securityRepository = SecurityRepository(
            authService = authService,
            firestoreService = firestoreService
        )
    }
    
    fun getRepository(): CashPalRepository {
        return _repository ?: throw IllegalStateException("ServiceLocator not initialized")
    }

    fun getSecurityRepository(): SecurityRepository {
        return _securityRepository ?: throw IllegalStateException("ServiceLocator not initialized")
    }
}
