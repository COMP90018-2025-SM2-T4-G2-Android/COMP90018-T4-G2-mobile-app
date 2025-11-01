package com.cashpal.app.ai

import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.ChatMessage
import com.cashpal.app.models.Transaction
import com.cashpal.app.models.User
import com.cashpal.app.services.HuggingFaceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIAssistantManager {
    
    private var conversationHistory = mutableListOf<ChatMessage>()
    private var userData: User? = null
    private var recentTransactions: List<Transaction> = emptyList()
    
    suspend fun initializeUserContext(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val repository = ServiceLocator.getRepository()
                
                // Load user profile
                try {
                    repository.getUserProfile(userId).collect { result ->
                        result.fold(
                            onSuccess = { user -> userData = user },
                            onFailure = { error ->
                                android.util.Log.e("AIAssistantManager", "Failed to load user profile", error)
                            }
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AIAssistantManager", "Exception loading user profile", e)
                }
                
                // Load recent transactions
                try {
                    repository.getUserTransactions(userId, limit = 10).collect { result ->
                        result.fold(
                            onSuccess = { transactions -> recentTransactions = transactions },
                            onFailure = { error ->
                                android.util.Log.e("AIAssistantManager", "Failed to load transactions", error)
                            }
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AIAssistantManager", "Exception loading transactions", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("AIAssistantManager", "Failed to load user context", e)
            }
        }
    }
    
    suspend fun sendMessage(userMessage: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Add user message to history
                val userMsg = ChatMessage(userMessage, isUser = true)
                conversationHistory.add(userMsg)
                
                // Create context-aware prompt
                val prompt = buildPrompt(userMessage)
                
                // Call HuggingFace API
                val response = HuggingFaceService.api.chat(
                    com.cashpal.app.services.HuggingFaceRequest(inputs = prompt)
                )
                
                // Extract response text
                val aiResponse = response.firstOrNull()?.generatedText ?: "Sorry, I couldn't generate a response."
                
                // Add AI response to history
                val aiMsg = ChatMessage(aiResponse, isUser = false)
                conversationHistory.add(aiMsg)
                
                Result.success(aiResponse)
            } catch (e: Exception) {
                android.util.Log.e("AIAssistantManager", "AI request failed", e)
                Result.failure(e)
            }
        }
    }
    
    private fun buildPrompt(userMessage: String): String {
        val context = buildContext()
        return """
            You are CashPal AI Assistant, a helpful financial assistant for the CashPal mobile payment app.
            
            $context
            
            User: $userMessage
            
            CashPal AI:"""
    }
    
    private fun buildContext(): String {
        val userInfo = userData?.let {
            """
            User Profile:
            - Name: ${it.displayName}
            - Email: ${it.email}
            - Balance: ${String.format("%.2f", it.balance)} ${it.currency}
            - Currency: ${it.currency}
            """
        } ?: ""
        
        val transactionInfo = if (recentTransactions.isNotEmpty()) {
            val summary = recentTransactions.take(5).joinToString("\n") { t ->
                "- ${t.description}: ${String.format("%.2f", t.amount)} ${t.currency} (${t.category.name})"
            }
            """
            Recent Transactions:
            $summary
            """
        } else {
            "No recent transactions found."
        }
        
        return """
            Your capabilities:
            1. Help users understand their transactions and spending patterns
            2. Guide users through app features (sending money, scanning QR codes, etc.)
            3. Provide contextual advice about unusual spending or budget insights
            
            $userInfo
            
            $transactionInfo
            
            Keep responses concise, friendly, and helpful. Focus on CashPal app features and user's financial data.
        """
    }
    
    fun getConversationHistory(): List<ChatMessage> = conversationHistory.toList()
    
    fun clearHistory() {
        conversationHistory.clear()
    }
}

