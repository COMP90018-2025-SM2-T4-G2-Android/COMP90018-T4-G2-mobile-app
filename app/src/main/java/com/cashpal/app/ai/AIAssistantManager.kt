package com.cashpal.app.ai

import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.ChatMessage
import com.cashpal.app.models.Transaction
import com.cashpal.app.models.User
import com.cashpal.app.services.GeminiService
import com.cashpal.app.services.GeminiRequest
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
                
                // Build conversation history for Gemini API
                val geminiHistory = conversationHistory.map { msg ->
                    GeminiRequest.Content(
                        parts = listOf(GeminiRequest.Content.Part(msg.content)),
                        role = if (msg.isUser) "user" else "model"
                    )
                }
                
                // Build context-aware system prompt
                val systemPrompt = buildContext()
                val systemMessage = """
                    You are CashPal AI Assistant, a helpful financial assistant for the CashPal mobile payment app.
                    
                    $systemPrompt
                    
                    Please respond to the user's messages in a friendly, helpful, and concise manner.
                """.trimIndent()
                
                // Build full conversation with system context
                val fullConversation = mutableListOf<GeminiRequest.Content>().apply {
                    // Add system context as first message
                    add(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Content.Part(systemMessage)),
                            role = "user"
                        )
                    )
                    // Add conversation history (limit to last 10 messages to avoid token limits)
                    addAll(geminiHistory.takeLast(10))
                }
                
                // Call Gemini API
                val request = GeminiRequest(
                    contents = fullConversation,
                    generationConfig = GeminiRequest.GenerationConfig(
                        temperature = 0.7,
                        maxOutputTokens = 1024
                    )
                )
                
                val response = GeminiService.generateContent(request)
                
                // Handle response
                val aiResponse = if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    responseBody.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: responseBody.error?.message
                        ?: "Sorry, I couldn't generate a response."
                } else {
                    // Extract detailed error message from response body
                    val errorDetails = try {
                        response.errorBody()?.string() ?: response.message()
                    } catch (e: Exception) {
                        response.message()
                    }
                    
                    val apiError = response.body()?.error?.message
                    val errorMsg = apiError ?: when (response.code()) {
                        400 -> "Invalid request. Please check your prompt."
                        401 -> "Authentication failed. Please check your GEMINI_API_KEY in local.properties."
                        403 -> "Permission denied. Please check your API key permissions and enable Generative Language API in Google Cloud Console."
                        404 -> "Model not found. The model may not be available for your API key. Check: https://ai.google.dev/gemini-api/docs/models"
                        429 -> "Rate limit exceeded. Please try again later."
                        500 -> "Gemini API server error. Please try again later."
                        else -> "Error ${response.code()}: ${response.message()}"
                    }
                    
                    android.util.Log.e("AIAssistantManager", "AI request failed (Ask Gemini): HTTP ${response.code()} - $errorMsg")
                    android.util.Log.e("AIAssistantManager", "Error details: $errorDetails")
                    "Sorry, I'm having trouble right now. Please try again later."
                }
                
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

