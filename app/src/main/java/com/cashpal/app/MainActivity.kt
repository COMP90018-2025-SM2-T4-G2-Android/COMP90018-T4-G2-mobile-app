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
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.cashpal.app.data.DataRepository
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.fragments.HistoryFragment
import com.cashpal.app.fragments.MoreFragment
import com.cashpal.app.fragments.PayFragment
import com.cashpal.app.fragments.ScanFragment
import com.cashpal.app.fragments.ReceiptFragment
import com.cashpal.app.fragments.NfcPaymentFragment
import com.cashpal.app.utils.BiometricPreferences
import com.cashpal.app.utils.GooglePlayServicesUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import com.cashpal.app.services.FirebaseConfigService
import com.cashpal.app.utils.NotificationService
import com.cashpal.app.utils.DailySalesSummary
import com.cashpal.app.utils.SalesAnalytics
import com.cashpal.app.utils.SalesEntry
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var dataRepository: DataRepository
    private lateinit var firebaseRepository: com.cashpal.app.repository.CashPalRepository
    private lateinit var balanceValue: TextView
    private lateinit var monthlyChange: TextView
    private lateinit var pendingAmount: TextView
    private lateinit var reservedAmount: TextView
    private lateinit var quickActionsContainer: LinearLayout
    private lateinit var transactionsContainer: LinearLayout
    private lateinit var viewAllText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var dailySalesCard: MaterialCardView
    private lateinit var dailySalesTodayAmount: TextView
    private lateinit var dailySalesChange: TextView
    private lateinit var dailySalesPeriodSummary: TextView
    private lateinit var dailySalesEmpty: TextView
    private lateinit var dailySalesChartContainer: LinearLayout
    private var isDemoMode = false
    private var userCurrencyCode: String = "AUD"
    private var latestTransactions: List<com.cashpal.app.models.Transaction> = emptyList()

    override fun onResume() {
        super.onResume()
        // Refresh balance when returning to MainActivity
        if (::firebaseRepository.isInitialized && dataRepository.isUserSignedIn() && !isDemoMode) {
            android.util.Log.d("MainActivity", "onResume: Refreshing balance...")
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        NotificationService.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        isDemoMode = intent.getBooleanExtra("demo_mode", false)
        if (isDemoMode) {
            showDemoModeBanner()
        }

        GooglePlayServicesUtils.logGooglePlayServicesStatus(this)
        
        // Initialize Firebase Remote Config for API keys
        lifecycleScope.launch {
            FirebaseConfigService.fetchApiKeys().onSuccess {
                android.util.Log.d("MainActivity", "Firebase Remote Config initialized successfully")
            }.onFailure {
                android.util.Log.w("MainActivity", "Firebase Remote Config initialization failed, using fallback keys")
            }
        }
        
        initializeViews()
        setupBottomNavigation()
        setupClickListeners()
        loadData()

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showHomeContent()
                updateBottomNavigationSelection(R.id.nav_home)
            }
        }

        showHomeContent()
        openFromIntent(intent)
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
        dailySalesCard = findViewById(R.id.dailySalesCard)
        dailySalesTodayAmount = findViewById(R.id.dailySalesTodayAmount)
        dailySalesChange = findViewById(R.id.dailySalesChange)
        dailySalesPeriodSummary = findViewById(R.id.dailySalesPeriodSummary)
        dailySalesEmpty = findViewById(R.id.dailySalesEmpty)
        dailySalesChartContainer = findViewById(R.id.dailySalesChartContainer)

        showDailySalesEmptyState()
    }

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
            showFragment(HistoryFragment())
            updateBottomNavigationSelection(R.id.nav_history)
        }
    }

    private fun loadData() {
        // Always clear previous values first to avoid showing stale data
        balanceValue.text = "$0.00"
        monthlyChange.text = "+$0.00"
        pendingAmount.text = "$0.00"
        reservedAmount.text = "$0.00"
        
        if (dataRepository.isUserSignedIn() && !isDemoMode) {
            loadFirebaseData()
        } else if (isDemoMode) {
            loadDataFromJSON()
        } else {
            showEmptyState()
        }
    }

    private fun loadFirebaseData() {
        val currentUserId = dataRepository.getCurrentUserId()
        if (currentUserId == null) {
            android.util.Log.w("MainActivity", "User not found, showing empty state")
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "Loading Firebase data for user: $currentUserId")

                // Fetch user profile first to get stored balance
                launch {
                    firebaseRepository.getUserProfile(currentUserId).collect { userResult ->
                        userResult.fold(
                            onSuccess = { user ->
                                user?.let {
                                    android.util.Log.d(
                                        "MainActivity",
                                        "User profile loaded: ${it.email}, balance: ${it.balance}, currency: ${it.currency}"
                                    )
                                    // Show stored balance immediately, then update with calculated if different
                                    populateBalanceFromFirebase(it, it.balance)
                                } ?: run {
                                    android.util.Log.w("MainActivity", "User profile is null")
                                    showEmptyState()
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.e("MainActivity", "Failed to load user profile", error)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to load user data: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showEmptyState()
                            }
                        )
                    }
                }

                // Fetch all transactions for balance calculation
                launch {
                    firebaseRepository.getUserTransactions(currentUserId, 1000).collect { allTransactionsResult ->
                        allTransactionsResult.fold(
                            onSuccess = { allTransactions ->
                                android.util.Log.d(
                                    "MainActivity",
                                    "Loaded ${allTransactions.size} transactions for balance calculation"
                                )
                                
                                // Log transaction details for debugging
                                allTransactions.forEach { transaction ->
                                    android.util.Log.d(
                                        "MainActivity",
                                        "Transaction: ${transaction.description} - Amount: ${transaction.amount} - " +
                                                "Status: ${transaction.status} - From: ${transaction.fromUserId} - To: ${transaction.toUserId}"
                                    )
                                }

                                updateDailySalesCard(allTransactions)
                                
                                // Calculate balance from completed transactions
                                val calculatedBalance = calculateBalanceFromTransactions(
                                    allTransactions,
                                    currentUserId
                                )
                                
                                android.util.Log.d(
                                    "MainActivity",
                                    "Calculated balance: $calculatedBalance for user: $currentUserId"
                                )
                                
                                // Get user profile to get currency
                                try {
                                    val userResult = firebaseRepository.getUserProfile(currentUserId).first()
                                    userResult.fold(
                                        onSuccess = { user ->
                                            user?.let {
                                                android.util.Log.d(
                                                    "MainActivity",
                                                    "User balance in Firestore: ${it.balance}"
                                                )
                                                populateBalanceFromFirebase(it, calculatedBalance)
                                                
                                                // Always sync calculated balance to Firestore if different
                                                if (Math.abs(it.balance - calculatedBalance) > 0.01) {
                                                    android.util.Log.d(
                                                        "MainActivity",
                                                        "Updating user balance in Firestore from ${it.balance} to $calculatedBalance"
                                                    )
                                                    lifecycleScope.launch {
                                                        firebaseRepository.updateUserProfile(
                                                            currentUserId,
                                                            mapOf("balance" to calculatedBalance)
                                                        ).collect { }
                                                    }
                                                }
                                            }
                                        },
                                        onFailure = { error ->
                                            android.util.Log.e("MainActivity", "Failed to get user profile", error)
                                        }
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Failed to get user profile", e)
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.e("MainActivity", "Failed to load transactions for balance", error)
                                showDailySalesEmptyState()
                                // Still try to show user balance even if transactions fail
                                try {
                                    val userResult = firebaseRepository.getUserProfile(currentUserId).first()
                                    userResult.fold(
                                        onSuccess = { user ->
                                            user?.let {
                                                populateBalanceFromFirebase(it, it.balance)
                                            }
                                        },
                                        onFailure = { }
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Failed to get user profile as fallback", e)
                                }
                            }
                        )
                    }
                }

                // Fetch recent transactions for display
                launch {
                    firebaseRepository.getUserTransactions(currentUserId, 10).collect { transactionsResult ->
                        transactionsResult.fold(
                            onSuccess = { transactions ->
                                android.util.Log.d(
                                    "MainActivity",
                                    "Loaded ${transactions.size} recent transactions from Firebase"
                                )
                                populateTransactionsFromFirebase(transactions)
                            },
                            onFailure = { error ->
                                android.util.Log.e("MainActivity", "Failed to load transactions", error)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to load transactions: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showEmptyState()
                            }
                        )
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to load data from Firebase", e)
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyState()
            }
        }

        // Quick actions are UI elements, load them statically (not from Firebase)
        populateQuickActions(getDefaultQuickActions())
    }
    
    private fun getDefaultQuickActions(): List<com.cashpal.app.data.QuickAction> {
        return listOf(
            com.cashpal.app.data.QuickAction("send_money", "Send Money", "ic_send_money"),
            com.cashpal.app.data.QuickAction("qr_pay", "QR Pay", "ic_qr_pay"),
            com.cashpal.app.data.QuickAction("nfc_pay", "NFC Pay", "ic_nfc_pay"),
            com.cashpal.app.data.QuickAction("add_money", "Add Money", "ic_add_money")
        )
    }

    private fun loadDataFromJSON() {
        val appData = dataRepository.loadAppData()
        appData?.let { data ->
            populateBalanceInfo(data.balanceInfo)
            populateQuickActions(data.quickActions)
            populateRecentTransactions(data.recentTransactions)
            populateDemoDailySales()
        } ?: run {
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateBalanceFromFirebase(user: com.cashpal.app.models.User, calculatedBalance: Double) {
        android.util.Log.d(
            "MainActivity",
            "Populating balance: stored=${user.balance}, calculated=$calculatedBalance"
        )

        updateUserCurrency(user.currency)
        if (latestTransactions.isNotEmpty()) {
            updateDailySalesCard(latestTransactions)
        }
        
        // Priority: Use stored balance from Firestore first (most reliable)
        // If stored balance is 0 or invalid, use calculated balance from transactions
        val displayBalance = when {
            user.balance > 0 -> {
                android.util.Log.d("MainActivity", "Using stored balance from Firestore: ${user.balance}")
                user.balance
            }
            calculatedBalance > 0 -> {
                android.util.Log.d("MainActivity", "Using calculated balance from transactions: $calculatedBalance")
                calculatedBalance
            }
            else -> {
                android.util.Log.w("MainActivity", "Both stored and calculated balances are 0")
                0.0
            }
        }
        
        // Format balance properly - ensure we're setting text, not appending
        balanceValue.text = "$${String.format("%.2f", displayBalance)} ${user.currency}"
        monthlyChange.text = "+$0.00"
        pendingAmount.text = "$0.00"
        reservedAmount.text = "$0.00"
        
        android.util.Log.d(
            "MainActivity",
            "Balance displayed: ${balanceValue.text}"
        )
    }

    private fun calculateBalanceFromTransactions(
        transactions: List<com.cashpal.app.models.Transaction>,
        currentUserId: String
    ): Double {
        var balance = 0.0
        
        android.util.Log.d("MainActivity", "Calculating balance for user: $currentUserId from ${transactions.size} transactions")
        
        transactions.forEach { transaction ->
            // Only count COMPLETED transactions
            if (transaction.status == com.cashpal.app.models.TransactionStatus.COMPLETED) {
                val isReceived = transaction.toUserId == currentUserId
                val isSent = transaction.fromUserId == currentUserId
                
                android.util.Log.d(
                    "MainActivity",
                    "Processing transaction: ${transaction.description} - " +
                            "Amount: ${transaction.amount} - " +
                            "From: ${transaction.fromUserId} - " +
                            "To: ${transaction.toUserId} - " +
                            "IsReceived: $isReceived - " +
                            "IsSent: $isSent"
                )
                
                when {
                    isReceived -> {
                        // Received transaction - add to balance
                        balance += transaction.amount
                        android.util.Log.d("MainActivity", "Added ${transaction.amount}, balance now: $balance")
                    }
                    isSent && transaction.fromUserId != "system" -> {
                        // Sent transaction - subtract from balance (but not system transactions)
                        balance -= transaction.amount
                        android.util.Log.d("MainActivity", "Subtracted ${transaction.amount}, balance now: $balance")
                    }
                    // System transactions where user is sender are not counted (system gives money)
                }
            } else {
                android.util.Log.d(
                    "MainActivity",
                    "Skipping transaction ${transaction.id} - status: ${transaction.status}"
                )
            }
        }
        
        android.util.Log.d(
            "MainActivity",
            "Final calculated balance: $balance from ${transactions.size} transactions"
        )
        
        return balance
    }

    private fun populateBalanceInfo(balanceInfo: com.cashpal.app.data.BalanceInfo) {
        updateUserCurrency(null)
        balanceValue.text = balanceInfo.availableBalance
        monthlyChange.text = balanceInfo.thisMonthChange
        pendingAmount.text = balanceInfo.pendingAmount
        reservedAmount.text = balanceInfo.reservedAmount
    }

    private fun updateUserCurrency(currencyCode: String?) {
        userCurrencyCode = currencyCode
            ?.takeIf { it.length >= 3 }
            ?.uppercase(Locale.ROOT)
            ?: run {
                try {
                    Currency.getInstance(Locale.getDefault()).currencyCode
                } catch (e: Exception) {
                    "AUD"
                }
            }
    }

    private fun updateDailySalesCard(transactions: List<com.cashpal.app.models.Transaction>) {
        latestTransactions = transactions
        val summary = buildDailySalesSummary(transactions)
        if (summary != null) {
            renderDailySalesSummary(summary)
        } else {
            showDailySalesEmptyState()
        }
    }

    private fun buildDailySalesSummary(
        transactions: List<com.cashpal.app.models.Transaction>
    ): DailySalesSummary? {
        val currentUserId = dataRepository.getCurrentUserId() ?: return null
        val entries = transactions.asSequence()
            .filter { it.status == com.cashpal.app.models.TransactionStatus.COMPLETED }
            .filter { it.toUserId == currentUserId }
            .mapNotNull { transaction ->
                if (transaction.amount <= 0) {
                    return@mapNotNull null
                }
                val timestamp = transaction.completedAt ?: transaction.createdAt
                val instant = timestamp.toDate().toInstant()
                SalesEntry(instant, transaction.amount)
            }
            .toList()

        if (entries.isEmpty()) return null

        return SalesAnalytics.calculateDailySales(
            entries = entries,
            zoneId = ZoneId.systemDefault()
        )
    }

    private fun renderDailySalesSummary(summary: DailySalesSummary) {
        dailySalesChartContainer.isVisible = true
        dailySalesEmpty.isVisible = false
        dailySalesTodayAmount.text = formatCurrency(summary.todayTotal)
        dailySalesPeriodSummary.text = getString(
            R.string.daily_sales_period_summary,
            formatCurrency(summary.totalForPeriod)
        )
        dailySalesChange.text = formatTrendLabel(summary)
        applyTrendStyle(summary.trendDirection)
        renderDailySalesChart(summary.buckets)
    }

    private fun formatTrendLabel(summary: DailySalesSummary): String {
        val changePercent = summary.changePercent ?: return getString(R.string.daily_sales_change_no_data)
        val sign = if (summary.absoluteChange >= 0) "+" else "-"
        val absoluteText = formatCurrency(abs(summary.absoluteChange))
        val percentText = String.format(Locale.getDefault(), "%.1f%%", abs(changePercent))
        val combined = "$sign$absoluteText ($sign$percentText)"
        return getString(R.string.daily_sales_change_template, combined)
    }

    private fun applyTrendStyle(direction: DailySalesSummary.TrendDirection?) {
        val (backgroundRes, textRes) = when (direction) {
            DailySalesSummary.TrendDirection.UP -> R.color.received_background to R.color.received_color
            DailySalesSummary.TrendDirection.DOWN -> R.color.sent_background to R.color.sent_color
            else -> R.color.pay_soft_surface to R.color.muted_foreground
        }

        val backgroundColor = ContextCompat.getColor(this, backgroundRes)
        val textColor = ContextCompat.getColor(this, textRes)
        dailySalesChange.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        dailySalesChange.setTextColor(textColor)
    }

    private fun renderDailySalesChart(buckets: List<com.cashpal.app.utils.DailySalesBucket>) {
        dailySalesChartContainer.removeAllViews()
        if (buckets.isEmpty()) {
            dailySalesChartContainer.isVisible = false
            dailySalesEmpty.isVisible = true
            return
        }

        val maxTotal = buckets.maxOfOrNull { it.total } ?: 0.0
        val maxHeight = 96.dpToPx()
        val minBarHeight = 6.dpToPx()

        buckets.forEach { bucket ->
            val columnLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    val horizontalMargin = 6.dpToPx()
                    setMargins(horizontalMargin, 0, horizontalMargin, 0)
                }
            }

            val amountText = TextView(this).apply {
                text = if (bucket.total > 0) formatCurrency(bucket.total) else ""
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted_foreground))
                visibility = if (bucket.total > 0) View.VISIBLE else View.INVISIBLE
            }

            val ratio = if (maxTotal > 0) bucket.total / maxTotal else 0.0
            val barHeight = when {
                bucket.total <= 0 -> minBarHeight
                else -> max((ratio * maxHeight).roundToInt(), minBarHeight)
            }

            val barView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    18.dpToPx(),
                    barHeight
                ).apply {
                    topMargin = 8.dpToPx()
                    bottomMargin = 8.dpToPx()
                }
                setBackgroundResource(R.drawable.daily_sales_bar_background)
                alpha = if (bucket.total > 0) 1f else 0.25f
            }

            val labelText = TextView(this).apply {
                text = bucket.label()
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted_foreground))
                setPadding(0, 4.dpToPx(), 0, 0)
            }

            columnLayout.addView(amountText)
            columnLayout.addView(barView)
            columnLayout.addView(labelText)

            dailySalesChartContainer.addView(columnLayout)
        }
    }

    private fun showDailySalesEmptyState(message: String? = null) {
        dailySalesChartContainer.isVisible = false
        dailySalesEmpty.isVisible = true
        dailySalesEmpty.text = message ?: getString(R.string.daily_sales_empty)
        dailySalesTodayAmount.text = formatCurrency(0.0)
        dailySalesChange.text = getString(R.string.daily_sales_change_no_data)
        applyTrendStyle(null)
        dailySalesPeriodSummary.text = getString(R.string.daily_sales_period_summary_placeholder)
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val currencyCode = userCurrencyCode.ifBlank { "AUD" }
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            formatter.currency = Currency.getInstance("AUD")
        }
        return formatter.format(amount)
    }

    private fun populateDemoDailySales() {
        latestTransactions = emptyList()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val sampleAmounts = listOf(180.0, 220.0, 160.0, 240.0, 210.0, 260.0, 300.0)
        val entries = sampleAmounts.mapIndexed { index, amount ->
            val date = today.minusDays((sampleAmounts.size - 1 - index).toLong())
            SalesEntry(date.atStartOfDay(zone).toInstant(), amount)
        }
        val summary = SalesAnalytics.calculateDailySales(entries, zone, today)
        if (summary != null) {
            renderDailySalesSummary(summary)
        } else {
            showDailySalesEmptyState()
        }
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
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                handleQuickActionClick(action)
            }
        }

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }

        val iconImage = ImageView(this).apply {
            val resourceId = getDrawableResourceId(action.icon)
            setImageResource(resourceId)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.primary))
            layoutParams.width = 32.dpToPx()
            layoutParams.height = 32.dpToPx()
        }

        val titleText = TextView(this).apply {
            text = action.title
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.foreground))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        linearLayout.addView(iconImage)
        linearLayout.addView(titleText)
        cardView.addView(linearLayout)

        return cardView
    }

    private fun handleQuickActionClick(action: com.cashpal.app.data.QuickAction) {
        when (action.id) {
            "send_money" -> {
                showFragment(PayFragment())
                updateBottomNavigationSelection(R.id.nav_pay)
            }
            "qr_pay" -> {
                showFragment(ScanFragment())
                updateBottomNavigationSelection(R.id.nav_scan)
            }
            "nfc_pay" -> {
                openNfcPayment()
            }
            "add_money" -> {
                Toast.makeText(this, "Add Money clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateTransactionsFromFirebase(transactions: List<com.cashpal.app.models.Transaction>) {
        transactionsContainer.removeAllViews()

        if (transactions.isEmpty()) {
            showEmptyTransactionsState()
        } else {
            transactions.take(5).forEach { transaction ->
                val cardView = createFirebaseTransactionCard(transaction)
                transactionsContainer.addView(cardView)
            }
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
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
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
            setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.foreground))
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
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.foreground))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val timeText = TextView(this).apply {
            text = formatTimestamp(transaction.createdAt)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted_foreground))
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
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
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
            setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.foreground))
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
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.foreground))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val timeText = TextView(this).apply {
            text = transaction.timeAgo
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted_foreground))
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
        Toast.makeText(
            this,
            "Transaction clicked: ${transaction.description} - $${String.format("%.2f", transaction.amount)} ${transaction.currency}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleTransactionClick(transaction: com.cashpal.app.data.Transaction) {
        Toast.makeText(
            this,
            "Transaction clicked: ${transaction.merchant} - ${transaction.amount}",
            Toast.LENGTH_SHORT
        ).show()
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
            else -> R.drawable.ic_person
        }
    }

    private fun showFragment(fragment: Fragment) {
        scrollView.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun refreshBalance() {
        android.util.Log.d("MainActivity", "refreshBalance called")
        if (::firebaseRepository.isInitialized && dataRepository.isUserSignedIn() && !isDemoMode) {
            loadData()
        }
    }

    private fun showHomeContent() {
        scrollView.visibility = View.VISIBLE
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }

    fun openScanTab() {
        updateBottomNavigationSelection(R.id.nav_scan)
    }

    fun openNfcPayment() {
        updateBottomNavigationSelection(R.id.nav_pay)
        scrollView.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NfcPaymentFragment())
            .addToBackStack("nfcPayment")
            .commit()
    }
    
    private fun updateBottomNavigationSelection(selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent) {
        if (intent.getStringExtra("openTab") == "receipt") {
            scrollView.visibility = View.GONE
            findViewById<View>(R.id.fragmentContainer).bringToFront()

            val args = Bundle().apply {
                putString("transactionId", intent.getStringExtra("transactionId"))
                putString("senderName", intent.getStringExtra("senderName"))
                putDouble("amount", intent.getDoubleExtra("amount", 0.0))
                putString("timestamp", intent.getStringExtra("timestamp") ?: "Just now")
                putString("status", intent.getStringExtra("status") ?: "completed")
                putString("type", intent.getStringExtra("type") ?: "received")
            }

            val receiptFragment = ReceiptFragment().apply {
                arguments = args
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, receiptFragment)
                .addToBackStack(null)
                .commit()
        }
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

        val mainContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        if (mainContainer != null) {
            mainContainer.addView(demoBanner)

            val fragmentContainer =
                findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragmentContainer)
            if (fragmentContainer != null) {
                val layoutParams =
                    fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                layoutParams.topToBottom = demoBanner.id
                fragmentContainer.layoutParams = layoutParams
            }
        }
    }

    private fun showWelcomeMessage() {
        android.util.Log.d("MainActivity", "Showing welcome message for new user")
        transactionsContainer.removeAllViews()

        val welcomeCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }
            radius = 12f
            elevation = 4f
            setContentPadding(24.dpToPx(), 24.dpToPx(), 24.dpToPx(), 24.dpToPx())
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_light))

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER

                addView(TextView(this@MainActivity).apply {
                    text = "🎉"
                    textSize = 48f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 16.dpToPx())
                })

                addView(TextView(this@MainActivity).apply {
                    text = "Welcome to CashPal!"
                    textSize = 24f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 8.dpToPx())
                })

                addView(TextView(this@MainActivity).apply {
                    text =
                        "You've received a welcome bonus of \$100 AUD to get started. Start by sending money to friends or scanning a QR code!"
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 16.dpToPx())
                })

                addView(com.google.android.material.button.MaterialButton(this@MainActivity).apply {
                    text = "Get Started"
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                    setBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            com.cashpal.app.R.color.purple_500
                        )
                    )
                    setOnClickListener {
                        showHomeContent()
                    }
                })
            })
        }

        transactionsContainer.addView(welcomeCard)
    }

    private fun showEmptyState() {
        android.util.Log.d("MainActivity", "Showing empty state - user not signed in or no data")
        showDailySalesEmptyState()
        transactionsContainer.removeAllViews()

        val emptyStateCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }
            radius = 12f
            elevation = 4f
            setContentPadding(24.dpToPx(), 24.dpToPx(), 24.dpToPx(), 24.dpToPx())
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER

                addView(TextView(this@MainActivity).apply {
                    text = "No data available"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                    gravity = android.view.Gravity.CENTER
                })
            })
        }

        transactionsContainer.addView(emptyStateCard)
    }

    private fun showEmptyTransactionsState() {
        android.util.Log.d("MainActivity", "Showing empty transactions state")
        showDailySalesEmptyState()

        val emptyTransactionsCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }
            radius = 12f
            elevation = 4f
            setContentPadding(24.dpToPx(), 24.dpToPx(), 24.dpToPx(), 24.dpToPx())
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_light))

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER

                addView(TextView(this@MainActivity).apply {
                    text = "📱"
                    textSize = 48f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 16.dpToPx())
                })

                addView(TextView(this@MainActivity).apply {
                    text = "No transactions yet"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 8.dpToPx())
                })

                addView(TextView(this@MainActivity).apply {
                    text = "Start by sending or receiving money!"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    gravity = android.view.Gravity.CENTER
                })
            })
        }

        transactionsContainer.addView(emptyTransactionsCard)
    }
}
