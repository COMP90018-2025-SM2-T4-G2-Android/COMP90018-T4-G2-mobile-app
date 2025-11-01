package com.cashpal.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cashpal.app.adapters.ChatMessageAdapter
import com.cashpal.app.ai.AIAssistantManager
import com.cashpal.app.databinding.ActivityAiChatBinding
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.ChatMessage
import kotlinx.coroutines.launch

class AIChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAiChatBinding
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var aiManager: AIAssistantManager
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get user ID from intent
        userId = intent.getStringExtra(EXTRA_USER_ID)
        if (userId == null) {
            userId = ServiceLocator.getRepository().getCurrentUser()?.uid
        }
        
        // Initialize AI Manager
        aiManager = AIAssistantManager()
        
        // Setup toolbar
        setupToolbar()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup click listeners
        setupClickListeners()
        
        // Initialize user context
        initializeChat()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(mutableListOf())
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerViewMessages.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
        
        binding.editTextMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }
    
    private fun initializeChat() {
        lifecycleScope.launch {
            try {
                binding.tvStatus.text = "Initializing..."
                userId?.let { id ->
                    aiManager.initializeUserContext(id)
                    
                    // Add welcome message
                    val welcomeMsg = ChatMessage(
                        content = "Hello! I'm CashPal AI Assistant. I can help you with:\n\n" +
                                "• Understanding your transactions\n" +
                                "• Guiding you through app features\n" +
                                "• Providing spending insights\n\n" +
                                "How can I help you today?",
                        isUser = false
                    )
                    adapter.addMessage(welcomeMsg)
                    
                    binding.tvStatus.text = "Online"
                } ?: run {
                    Toast.makeText(this@AIChatActivity, "User not found", Toast.LENGTH_SHORT).show()
                    binding.tvStatus.text = "Offline"
                }
            } catch (e: Exception) {
                android.util.Log.e("AIChatActivity", "Failed to initialize", e)
                binding.tvStatus.text = "Offline"
                Toast.makeText(this@AIChatActivity, "Failed to initialize chat", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sendMessage() {
        val message = binding.editTextMessage.text?.toString()?.trim()
        if (message.isNullOrEmpty()) {
            return
        }
        
        // Hide keyboard
        hideKeyboard()
        
        // Add user message to UI
        val userMessage = ChatMessage(message, isUser = true)
        adapter.addMessage(userMessage)
        
        // Clear input
        binding.editTextMessage.setText("")
        
        // Scroll to bottom
        binding.recyclerViewMessages.smoothScrollToPosition(adapter.itemCount - 1)
        
        // Show loading indicator
        binding.loadingIndicator.visibility = android.view.View.VISIBLE
        
        // Send to AI
        lifecycleScope.launch {
            try {
                val result = aiManager.sendMessage(message)
                result.onSuccess { aiResponse ->
                    // Add AI response to UI
                    val aiMessage = ChatMessage(aiResponse, isUser = false)
                    adapter.addMessage(aiMessage)
                    
                    // Scroll to bottom
                    binding.recyclerViewMessages.smoothScrollToPosition(adapter.itemCount - 1)
                }.onFailure { error ->
                    // Show error message
                    val errorMsg = ChatMessage(
                        content = "Sorry, I'm having trouble right now. Please try again later.",
                        isUser = false
                    )
                    adapter.addMessage(errorMsg)
                    Toast.makeText(this@AIChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AIChatActivity", "Failed to send message", e)
                val errorMsg = ChatMessage(
                    content = "Sorry, an error occurred. Please check your connection and try again.",
                    isUser = false
                )
                adapter.addMessage(errorMsg)
                Toast.makeText(this@AIChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Hide loading indicator
                binding.loadingIndicator.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextMessage.windowToken, 0)
    }
    
    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        
        fun start(context: Context, userId: String? = null) {
            val intent = Intent(context, AIChatActivity::class.java).apply {
                userId?.let { putExtra(EXTRA_USER_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}

