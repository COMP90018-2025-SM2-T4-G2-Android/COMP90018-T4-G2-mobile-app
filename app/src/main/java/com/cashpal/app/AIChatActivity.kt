package com.cashpal.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cashpal.app.adapters.ChatMessageAdapter
import com.cashpal.app.adapters.ChatRecommendationAdapter
import com.cashpal.app.ai.AIAssistantManager
import com.cashpal.app.databinding.ActivityAiChatBinding
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.ChatMessage
import kotlinx.coroutines.launch

class AIChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAiChatBinding
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var recommendationAdapter: ChatRecommendationAdapter
    private lateinit var aiManager: AIAssistantManager
    private var userId: String? = null
    
    private val recommendations = listOf(
        "Check balance",
        "Guide app use",
        "Recent transactions",
        "Spending insights",
        "How to send money",
        "How to scan QR code"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge mode
        enableEdgeToEdge()
        
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup window insets handling
        setupWindowInsets()
        
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
        
        // Setup Recommendations RecyclerView
        setupRecommendationsRecyclerView()
        
        // Setup click listeners
        setupClickListeners()
        
        // Initialize user context
        initializeChat()
    }
    
    private fun setupWindowInsets() {
        // Handle status bar insets for toolbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        
        // Handle navigation bar insets for input container
        // Store the original padding before any insets are applied
        binding.inputContainer.post {
            val innerLayout = binding.inputContainer.getChildAt(0) as? android.view.ViewGroup
            innerLayout?.let { layout ->
                val originalPaddingLeft = layout.paddingLeft
                val originalPaddingTop = layout.paddingTop
                val originalPaddingRight = layout.paddingRight
                val originalPaddingBottom = layout.paddingBottom
                
                ViewCompat.setOnApplyWindowInsetsListener(binding.inputContainer) { v, insets ->
                    val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    val tappableElement = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
                    // Use the maximum to handle both gesture and 3-button navigation
                    val bottomInset = maxOf(navigationBars.bottom, tappableElement.bottom)
                    
                    // Get the inner LinearLayout
                    val inner = (v as? android.view.ViewGroup)?.getChildAt(0) as? android.view.ViewGroup
                    inner?.let {
                        // Always use original padding values to prevent accumulation
                        it.setPadding(
                            originalPaddingLeft,
                            originalPaddingTop,
                            originalPaddingRight,
                            originalPaddingBottom + bottomInset
                        )
                    }
                    insets
                }
            }
        }
        
        // Ensure status bar content is visible on dark background (using non-deprecated API)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController?.isAppearanceLightStatusBars = false
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
    
    private fun setupRecommendationsRecyclerView() {
        recommendationAdapter = ChatRecommendationAdapter(recommendations) { recommendation ->
            onRecommendationClicked(recommendation)
        }
        binding.recommendationsContainer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recommendationsContainer.adapter = recommendationAdapter
    }
    
    private fun onRecommendationClicked(recommendation: String) {
        // Hide recommendations after clicking
        binding.recommendationsContainer.visibility = android.view.View.GONE
        
        // Send the recommendation as a message
        binding.editTextMessage.setText(recommendation)
        sendMessage()
    }
    
    private fun showRecommendations() {
        binding.recommendationsContainer.visibility = android.view.View.VISIBLE
    }
    
    private fun hideRecommendations() {
        binding.recommendationsContainer.visibility = android.view.View.GONE
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
                    
                    // Show recommendations initially
                    showRecommendations()
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
        
        // Hide recommendations when user sends a message
        hideRecommendations()
        
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
                    
                    // Show recommendations again after AI responds
                    showRecommendations()
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

