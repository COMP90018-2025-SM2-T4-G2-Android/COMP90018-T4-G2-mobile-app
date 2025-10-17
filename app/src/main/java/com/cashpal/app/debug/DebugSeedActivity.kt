package com.cashpal.app.debug

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.R
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.Contact
import com.cashpal.app.models.PaymentRequest
import com.cashpal.app.models.PaymentRequestStatus
import com.cashpal.app.models.Transaction
import com.cashpal.app.models.TransactionCategory
import com.cashpal.app.models.TransactionStatus
import com.cashpal.app.models.TransactionType
import com.cashpal.app.models.User
import com.cashpal.app.models.UserPreferences
import com.google.firebase.Timestamp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Random

class DebugSeedActivity : AppCompatActivity() {
    private lateinit var repository: com.cashpal.app.repository.CashPalRepository
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var seedButton: Button
    private lateinit var clearButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_debug_seed)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        repository = ServiceLocator.getRepository()
        remoteConfig = FirebaseRemoteConfig.getInstance()
        
        initializeViews()
        setupClickListeners()
        checkRemoteConfig()
    }
    
    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        seedButton = findViewById(R.id.seedButton)
        clearButton = findViewById(R.id.clearButton)
        
        progressBar.visibility = android.view.View.GONE
        statusText.text = "Ready to seed mock data"
        logText.text = ""
    }
    
    private fun setupClickListeners() {
        seedButton.setOnClickListener {
            if (isDebugSeedingEnabled()) {
                seedMockData()
            } else {
                Toast.makeText(this, "Debug seeding is disabled in Remote Config", Toast.LENGTH_LONG).show()
            }
        }
        
        clearButton.setOnClickListener {
            clearLog()
        }
    }
    
    private fun checkRemoteConfig() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                logMessage("Remote Config updated: ${if (isDebugSeedingEnabled()) "Seeding enabled" else "Seeding disabled"}")
                seedButton.isEnabled = isDebugSeedingEnabled()
            } else {
                logMessage("Failed to fetch Remote Config: ${task.exception?.message}")
                // Default to enabled for debug builds
                seedButton.isEnabled = true
            }
        }
    }
    
    private fun isDebugSeedingEnabled(): Boolean {
        return remoteConfig.getBoolean("enable_debug_seeding")
    }
    
    private fun seedMockData() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = android.view.View.VISIBLE
                seedButton.isEnabled = false
                statusText.text = "Seeding mock data..."
                
                logMessage("Starting mock data seeding...")
                
                // Create mock users
                val mockUsers = createMockUsers()
                logMessage("Created ${mockUsers.size} mock users")
                
                // Create mock contacts
                createMockContacts(mockUsers)
                logMessage("Created mock contacts")
                
                // Create mock transactions
                createMockTransactions(mockUsers)
                logMessage("Created mock transactions")
                
                // Create mock payment requests
                createMockPaymentRequests(mockUsers)
                logMessage("Created mock payment requests")
                
                statusText.text = "Mock data seeding completed!"
                logMessage("Mock data seeding completed successfully!")
                
            } catch (e: Exception) {
                statusText.text = "Seeding failed: ${e.message}"
                logMessage("Error: ${e.message}")
                Toast.makeText(this@DebugSeedActivity, "Seeding failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
                seedButton.isEnabled = true
            }
        }
    }
    
    private suspend fun createMockUsers(): List<User> {
        val users = mutableListOf<User>()
        val random = Random()
        
        for (i in 1..5) {
            val balance = 100.0 + random.nextDouble() * 900.0 // $100-$1000 AUD
            val user = User(
                id = "test_user_$i",
                email = "test$i@cashpal.com",
                displayName = "Test User $i",
                balance = balance,
                currency = "AUD",
                isVerified = true,
                preferences = UserPreferences(currency = "AUD")
            )
            
            repository.createUser(user).collect { result ->
                result.fold(
                    onSuccess = {
                        users.add(user)
                        logMessage("Created user: ${user.displayName} (${user.balance} AUD)")
                    },
                    onFailure = { error ->
                        logMessage("Failed to create user ${user.displayName}: ${error.message}")
                    }
                )
            }
        }
        
        return users
    }
    
    private suspend fun createMockContacts(users: List<User>) {
        val random = Random()
        
        users.forEach { user ->
            val otherUsers = users.filter { it.id != user.id }
            val contactCount = 2 + random.nextInt(3) // 2-4 contacts per user
            
            repeat(contactCount) {
                val contactUser = otherUsers.random()
                val isFrequent = random.nextBoolean()
                
                val contact = Contact(
                    userId = user.id,
                    contactUserId = contactUser.id,
                    name = contactUser.displayName,
                    email = contactUser.email,
                    isFrequent = isFrequent,
                    totalTransactions = random.nextInt(10)
                )
                
                repository.createContact(contact).collect { result ->
                    result.fold(
                        onSuccess = {
                            logMessage("Created contact: ${contact.name} for ${user.displayName}")
                        },
                        onFailure = { error ->
                            logMessage("Failed to create contact: ${error.message}")
                        }
                    )
                }
            }
        }
    }
    
    private suspend fun createMockTransactions(users: List<User>) {
        val random = Random()
        val categories = TransactionCategory.values()
        
        repeat(20) { // Create 20 transactions
            val fromUser = users.random()
            val toUser = users.filter { it.id != fromUser.id }.random()
            val amount = 5.0 + random.nextDouble() * 95.0 // $5-$100 AUD
            val category = categories.random()
            
            val transaction = Transaction(
                fromUserId = fromUser.id,
                toUserId = toUser.id,
                amount = amount,
                currency = "AUD",
                description = "${category.name.lowercase()} payment",
                category = category,
                status = if (random.nextBoolean()) TransactionStatus.COMPLETED else TransactionStatus.PENDING,
                type = TransactionType.TRANSFER,
                createdAt = Timestamp(Date(System.currentTimeMillis() - random.nextLong() % (30L * 24 * 60 * 60 * 1000))) // Random date within last 30 days
            )
            
            repository.createTransaction(transaction).collect { result ->
                result.fold(
                    onSuccess = {
                        logMessage("Created transaction: ${fromUser.displayName} -> ${toUser.displayName} (${amount} AUD)")
                    },
                    onFailure = { error ->
                        logMessage("Failed to create transaction: ${error.message}")
                    }
                )
            }
        }
    }
    
    private suspend fun createMockPaymentRequests(users: List<User>) {
        val random = Random()
        
        repeat(10) { // Create 10 payment requests
            val fromUser = users.random()
            val toUser = users.filter { it.id != fromUser.id }.random()
            val amount = 10.0 + random.nextDouble() * 90.0 // $10-$100 AUD
            
            val paymentRequest = PaymentRequest(
                fromUserId = fromUser.id,
                toUserId = toUser.id,
                amount = amount,
                currency = "AUD",
                description = "Payment request for services",
                status = if (random.nextBoolean()) PaymentRequestStatus.PENDING else PaymentRequestStatus.ACCEPTED,
                expiresAt = Timestamp(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)) // Expires in 7 days
            )
            
            repository.createPaymentRequest(paymentRequest).collect { result ->
                result.fold(
                    onSuccess = {
                        logMessage("Created payment request: ${fromUser.displayName} -> ${toUser.displayName} (${amount} AUD)")
                    },
                    onFailure = { error ->
                        logMessage("Failed to create payment request: ${error.message}")
                    }
                )
            }
        }
    }
    
    private fun logMessage(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            logText.text = "${logText.text}\n[$timestamp] $message"
            
            // Auto-scroll to bottom
            val scrollView = findViewById<ScrollView>(R.id.logScrollView)
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
    
    private fun clearLog() {
        logText.text = ""
        statusText.text = "Ready to seed mock data"
    }
}
