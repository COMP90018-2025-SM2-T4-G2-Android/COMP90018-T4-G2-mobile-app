package com.cashpal.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.cashpal.app.data.AppData
import com.cashpal.app.data.DataRepository
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.fragments.HistoryFragment
import com.cashpal.app.fragments.MoreFragment
import com.cashpal.app.fragments.PayFragment
import com.cashpal.app.fragments.ScanFragment
import com.cashpal.app.utils.BiometricPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    
    private lateinit var dataRepository: DataRepository
    private lateinit var firebaseRepository: com.cashpal.app.repository.CashPalRepository
    private lateinit var biometricPreferences: BiometricPreferences
    private lateinit var balanceValue: TextView
    private lateinit var monthlyChange: TextView
    private lateinit var pendingAmount: TextView
    private lateinit var reservedAmount: TextView
    private lateinit var quickActionsContainer: LinearLayout
    private lateinit var transactionsContainer: LinearLayout
    private lateinit var viewAllText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isDemoMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(com.cashpal.app.R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        
        // Check for demo mode
        isDemoMode = intent.getBooleanExtra("demo_mode", false) || 
                     BiometricPreferences(this).isDemoMode()
        
        if (isDemoMode) {
            showDemoModeBanner()
        }

        initializeViews()
        setupBottomNavigation()
        setupClickListeners()
        loadData()
        showHomeContent() // Show home content by default
    }
    
    private fun initializeViews() {
        firebaseRepository = ServiceLocator.getRepository()
        dataRepository = DataRepository(this, firebaseRepository)
        balanceValue = findViewById(R.id.balanceValue)
        monthlyChange = findViewById(R.id.monthlyChange)
        pendingAmount = findViewById(R.id.pendingAmount)
        reservedAmount = findViewById(R.id.reservedAmount)
        quickActionsContainer = findViewById(R.id.quickActionsContainer)
        transactionsContainer = findViewById(R.id.transactionsContainer)
        viewAllText = findViewById(R.id.viewAllText)
        scrollView = findViewById(R.id.scrollView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }
    //commen
    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeContent()
                    true
                }
                R.id.nav_pay -> {
                    showFragment(PayFragment())
                    true
                }
                R.id.nav_scan -> {
                    showFragment(ScanFragment())
                    true
                }
                R.id.nav_history -> {
                    showFragment(HistoryFragment())
                    true
                }
                R.id.nav_more -> {
                    showFragment(MoreFragment())
                    true
                }
                else -> false
            }
        }
        
        bottomNavigationView.selectedItemId = R.id.nav_home
    }
    
    private fun setupClickListeners() {
        viewAllText.setOnClickListener {
            // Navigate to History page
            showFragment(HistoryFragment())
            updateBottomNavigationSelection(R.id.nav_history)
        }
    }
    
    private fun loadData() {
        // Try Firebase first, fallback to JSON
        if (dataRepository.isUserSignedIn()) {
            loadFirebaseData()
        } else {
            loadDataFromJSON()
        }
    }
    
    private fun loadFirebaseData() {
        val currentUserId = dataRepository.getCurrentUserId()
        if (currentUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            loadDataFromJSON()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Load user profile
                firebaseRepository.getUserProfile(currentUserId).collect { userResult ->
                    userResult.fold(
                        onSuccess = { user ->
                            user?.let {
                                populateBalanceFromFirebase(it)
                            }
                        },
                        onFailure = { error ->
                            Toast.makeText(this@MainActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                            loadDataFromJSON()
                        }
                    )
                }
                
                // Load transactions
                firebaseRepository.getUserTransactions(currentUserId, 10).collect { transactionsResult ->
                    transactionsResult.fold(
                        onSuccess = { transactions ->
                            populateTransactionsFromFirebase(transactions)
                        },
                        onFailure = { error ->
                            Toast.makeText(this@MainActivity, "Failed to load transactions: ${error.message}", Toast.LENGTH_SHORT).show()
                            loadDataFromJSON()
                        }
                    )
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                loadDataFromJSON()
            }
        }
        
        // Always load quick actions from JSON (they're static)
        val appData = dataRepository.loadAppData()
        appData?.let { data ->
            populateQuickActions(data.quickActions)
        }
    }
    
    private fun loadDataFromJSON() {
        val appData = dataRepository.loadAppData()
        appData?.let { data ->
            populateBalanceInfo(data.balanceInfo)
            populateQuickActions(data.quickActions)
            populateRecentTransactions(data.recentTransactions)
        } ?: run {
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun populateBalanceFromFirebase(user: com.cashpal.app.models.User) {
        balanceValue.text = "$${String.format("%.2f", user.balance)} ${user.currency}"
        monthlyChange.text = "+$0.00" // TODO: Calculate monthly change from transactions
        pendingAmount.text = "$0.00" // TODO: Calculate pending amount from pending transactions
        reservedAmount.text = "$0.00" // TODO: Calculate reserved amount
    }
    
    private fun populateBalanceInfo(balanceInfo: com.cashpal.app.data.BalanceInfo) {
        balanceValue.text = balanceInfo.availableBalance
        monthlyChange.text = balanceInfo.thisMonthChange
        pendingAmount.text = balanceInfo.pendingAmount
        reservedAmount.text = balanceInfo.reservedAmount
    }
    
    private fun populateQuickActions(quickActions: List<com.cashpal.app.data.QuickAction>) {
        quickActionsContainer.removeAllViews()
        
        quickActions.forEach { action ->
            val cardView = createQuickActionCard(action)
            quickActionsContainer.addView(cardView)
        }
    }
    
    private fun createQuickActionCard(action: com.cashpal.app.data.QuickAction): MaterialCardView {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8
            }
            radius = 24f
            elevation = 4f
            setCardBackgroundColor(getColor(android.R.color.white))
            
            // Make it clickable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                handleQuickActionClick(action)
            }
        }
        
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(24, 24, 24, 24) // Reduced padding for bigger buttons
        }
        
        val iconImage = ImageView(this).apply {
            val resourceId = getDrawableResourceId(action.icon)
            setImageResource(resourceId)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.black))
            // Set specific size for icons
            layoutParams.width = 32.dpToPx()
            layoutParams.height = 32.dpToPx()
        }
        
        val titleText = TextView(this).apply {
            text = action.title
            textSize = 11f // Slightly smaller text
            setTextColor(getColor(android.R.color.black))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 12, 0, 0) // Reduced padding
        }
        
        linearLayout.addView(iconImage)
        linearLayout.addView(titleText)
        cardView.addView(linearLayout)
        
        return cardView
    }
    
    private fun handleQuickActionClick(action: com.cashpal.app.data.QuickAction) {
        when (action.id) {
            "send_money" -> {
                // Navigate to Pay page
                showFragment(PayFragment())
                updateBottomNavigationSelection(R.id.nav_pay)
            }
            "qr_pay" -> {
                // Navigate to Scan page
                showFragment(ScanFragment())
                updateBottomNavigationSelection(R.id.nav_scan)
            }
            "nfc_pay" -> {
                Toast.makeText(this, "NFC Pay clicked", Toast.LENGTH_SHORT).show()
                // TODO: Initiate NFC payment
            }
            "add_money" -> {
                Toast.makeText(this, "Add Money clicked", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to add money screen
            }
        }
    }
    
    private fun populateTransactionsFromFirebase(transactions: List<com.cashpal.app.models.Transaction>) {
        transactionsContainer.removeAllViews()
        
        transactions.take(5).forEach { transaction ->
            val cardView = createFirebaseTransactionCard(transaction)
            transactionsContainer.addView(cardView)
        }
    }
    
    private fun populateRecentTransactions(transactions: List<com.cashpal.app.data.Transaction>) {
        transactionsContainer.removeAllViews()
        
        transactions.forEach { transaction ->
            val cardView = createTransactionCard(transaction)
            transactionsContainer.addView(cardView)
        }
    }
    
    private fun createFirebaseTransactionCard(transaction: com.cashpal.app.models.Transaction): MaterialCardView {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            radius = 24f
            elevation = 4f
            setCardBackgroundColor(getColor(android.R.color.white))
            
            // Make it clickable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                handleFirebaseTransactionClick(transaction)
            }
        }
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val iconImage = ImageView(this).apply {
            setImageResource(getTransactionIcon(transaction.category))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 32
            }
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.black))
        }
        
        val detailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val merchantText = TextView(this).apply {
            text = transaction.description
            textSize = 16f
            setTextColor(getColor(android.R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val timeText = TextView(this).apply {
            text = formatTimestamp(transaction.createdAt)
            textSize = 12f
            setTextColor(getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 0)
        }
        
        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.END
        }
        
        val currentUserId = dataRepository.getCurrentUserId()
        val isIncoming = transaction.toUserId == currentUserId
        
        val amountText = TextView(this).apply {
            text = "${if (isIncoming) "+" else "-"}$${String.format("%.2f", transaction.amount)} ${transaction.currency}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                if (isIncoming) getColor(android.R.color.holo_green_dark)
                else getColor(android.R.color.holo_red_dark)
            )
        }
        
        val statusText = TextView(this).apply {
            text = transaction.status.name
            textSize = 12f
            setTextColor(
                when (transaction.status) {
                    com.cashpal.app.models.TransactionStatus.COMPLETED -> getColor(android.R.color.holo_green_dark)
                    com.cashpal.app.models.TransactionStatus.PENDING -> getColor(android.R.color.holo_orange_dark)
                    else -> getColor(android.R.color.holo_red_dark)
                }
            )
            setPadding(0, 8, 0, 0)
        }
        
        detailsLayout.addView(merchantText)
        detailsLayout.addView(timeText)
        
        amountLayout.addView(amountText)
        amountLayout.addView(statusText)
        
        mainLayout.addView(iconImage)
        mainLayout.addView(detailsLayout)
        mainLayout.addView(amountLayout)
        
        cardView.addView(mainLayout)
        
        return cardView
    }
    
    private fun createTransactionCard(transaction: com.cashpal.app.data.Transaction): MaterialCardView {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            radius = 24f
            elevation = 4f
            setCardBackgroundColor(getColor(android.R.color.white))
            
            // Make it clickable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                handleTransactionClick(transaction)
            }
        }
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val iconImage = ImageView(this).apply {
            val resourceId = getDrawableResourceId(transaction.icon)
            setImageResource(resourceId)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 32
            }
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.black))
        }
        
        val detailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val merchantText = TextView(this).apply {
            text = transaction.merchant
            textSize = 16f
            setTextColor(getColor(android.R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val timeText = TextView(this).apply {
            text = transaction.timeAgo
            textSize = 12f
            setTextColor(getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 0)
        }
        
        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.END
        }
        
        val amountText = TextView(this).apply {
            text = transaction.amount
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                if (transaction.isIncoming) getColor(android.R.color.holo_green_dark)
                else getColor(android.R.color.holo_red_dark)
            )
        }
        
        val statusText = TextView(this).apply {
            text = transaction.status
            textSize = 12f
            setTextColor(getColor(android.R.color.holo_green_dark))
            setPadding(0, 8, 0, 0)
        }
        
        detailsLayout.addView(merchantText)
        detailsLayout.addView(timeText)
        
        amountLayout.addView(amountText)
        amountLayout.addView(statusText)
        
        mainLayout.addView(iconImage)
        mainLayout.addView(detailsLayout)
        mainLayout.addView(amountLayout)
        
        cardView.addView(mainLayout)
        
        return cardView
    }
    
    private fun handleFirebaseTransactionClick(transaction: com.cashpal.app.models.Transaction) {
        Toast.makeText(this, "Transaction clicked: ${transaction.description} - $${String.format("%.2f", transaction.amount)} ${transaction.currency}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to transaction details screen
    }
    
    private fun handleTransactionClick(transaction: com.cashpal.app.data.Transaction) {
        Toast.makeText(this, "Transaction clicked: ${transaction.merchant} - ${transaction.amount}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to transaction details screen
    }
    
    private fun getTransactionIcon(category: com.cashpal.app.models.TransactionCategory): Int {
        return when (category) {
            com.cashpal.app.models.TransactionCategory.FOOD -> R.drawable.ic_coffee_shop
            com.cashpal.app.models.TransactionCategory.TRANSPORT -> R.drawable.ic_send_money
            com.cashpal.app.models.TransactionCategory.SHOPPING -> R.drawable.ic_shopping_cart
            com.cashpal.app.models.TransactionCategory.ENTERTAINMENT -> R.drawable.ic_online_store
            com.cashpal.app.models.TransactionCategory.BILLS -> R.drawable.ic_person
            com.cashpal.app.models.TransactionCategory.HEALTHCARE -> R.drawable.ic_person
            com.cashpal.app.models.TransactionCategory.EDUCATION -> R.drawable.ic_person
            com.cashpal.app.models.TransactionCategory.TRAVEL -> R.drawable.ic_send_money
            com.cashpal.app.models.TransactionCategory.OTHER -> R.drawable.ic_person
        }
    }
    
    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
        val now = java.util.Date()
        val diff = now.time - timestamp.toDate().time
        val days = diff / (24 * 60 * 60 * 1000)
        
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            days < 30L -> "${days / 7} weeks ago"
            else -> "${days / 30} months ago"
        }
    }
    
    private fun getDrawableResourceId(iconName: String): Int {
        return when (iconName) {
            "ic_send_money" -> R.drawable.ic_send_money
            "ic_qr_pay" -> R.drawable.ic_qr_pay
            "ic_nfc_pay" -> R.drawable.ic_nfc_pay
            "ic_add_money" -> R.drawable.ic_add_money
            "ic_coffee_shop" -> R.drawable.ic_coffee_shop
            "ic_person" -> R.drawable.ic_person
            "ic_shopping_cart" -> R.drawable.ic_shopping_cart
            "ic_online_store" -> R.drawable.ic_online_store
            else -> R.drawable.ic_person // Default fallback
        }
    }
    
    private fun showFragment(fragment: Fragment) {
        scrollView.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun showHomeContent() {
        scrollView.visibility = View.VISIBLE
        // Clear any existing fragments
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }
    
    private fun updateBottomNavigationSelection(selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    private fun showDemoModeBanner() {
        val demoBanner = MaterialCardView(this).apply {
            id = View.generateViewId()
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 8)
                topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            }
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_light))
            radius = 12f
            elevation = 4f
            setContentPadding(16, 12, 16, 12)
            
            addView(TextView(this@MainActivity).apply {
                text = "🔍 Demo Mode - Using sample data"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                setPadding(8, 8, 8, 8)
            })
        }
        
        // Add banner to the main container and update constraints
        val mainContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        if (mainContainer != null) {
            mainContainer.addView(demoBanner)
            
            // Update the fragment container to be below the banner
            val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer)
            if (fragmentContainer != null) {
                val layoutParams = fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                layoutParams.topToBottom = demoBanner.id
                fragmentContainer.layoutParams = layoutParams
            }
        }
    }
}
