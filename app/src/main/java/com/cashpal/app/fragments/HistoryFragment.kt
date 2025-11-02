package com.cashpal.app.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.adapters.TransactionHistoryAdapter
import com.cashpal.app.data.DataRepository
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.TransactionHistory
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var totalSentTextView: TextView
    private lateinit var totalReceivedTextView: TextView

    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private var allTransactions = listOf<TransactionHistory>()
    private var filteredTransactions = listOf<TransactionHistory>() // Track currently filtered transactions
    
    // Track active filters
    private var activeMonthFilter: String? = null // MM format
    private var activeDateFilter: String? = null // yyyy-MM-dd format
    private var activeStateFilter: String? = null // "success", "fail", or null
    
    private lateinit var repository: com.cashpal.app.repository.CashPalRepository
    private lateinit var dataRepository: DataRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = ServiceLocator.getRepository()
        dataRepository = DataRepository(requireContext(), repository)

        initViews()
        setupData()
        setupSearch()
        setupTabs()
        setupButtons()
    }

    private fun initViews() {
        searchEditText = requireView().findViewById(R.id.et_search)
        tabLayout = requireView().findViewById(R.id.tab_layout)
        transactionsRecyclerView = requireView().findViewById(R.id.rv_transactions)
        totalSentTextView = requireView().findViewById(R.id.tv_total_sent)
        totalReceivedTextView = requireView().findViewById(R.id.tv_total_received)

        transactionsRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Initialize adapter with empty list
        transactionAdapter = TransactionHistoryAdapter(emptyList()) { tx ->
            openReceipt(tx)
        }
        transactionsRecyclerView.adapter = transactionAdapter
    }

    private fun setupData() {
        val currentUserId = dataRepository.getCurrentUserId()
        
        if (currentUserId == null || !dataRepository.isUserSignedIn()) {
            // Show empty state if user not signed in
            allTransactions = emptyList()
            transactionAdapter.updateTransactions(emptyList())
            filteredTransactions = emptyList()
            calculateTotals(emptyList())
            Toast.makeText(requireContext(), "Please sign in to view transaction history", Toast.LENGTH_SHORT).show()
            return
        }

        // Load transactions from Firebase
        lifecycleScope.launch {
            repository.getUserTransactions(currentUserId, 1000).collect { result ->
                result.fold(
                    onSuccess = { transactions ->
                        android.util.Log.d("HistoryFragment", "Loaded ${transactions.size} transactions from Firebase")
                        // Convert Firebase Transaction to TransactionHistory
                        allTransactions = transactions.map { transaction ->
                            convertToTransactionHistory(transaction, currentUserId)
                        }
                        filteredTransactions = allTransactions // Initialize filtered list
                        activeMonthFilter = null
                        activeDateFilter = null
                        activeStateFilter = null
                        transactionAdapter.updateTransactions(allTransactions)
                        calculateTotals(filteredTransactions)
                        filterTransactions("all", searchEditText.text.toString())
                    },
                    onFailure = { error ->
                        android.util.Log.e("HistoryFragment", "Failed to load transactions", error)
                        Toast.makeText(
                            requireContext(),
                            "Failed to load transactions: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        allTransactions = emptyList()
                        transactionAdapter.updateTransactions(emptyList())
                        filteredTransactions = emptyList()
                        calculateTotals(emptyList())
                    }
                )
            }
        }
    }
    
    private fun convertToTransactionHistory(
        transaction: com.cashpal.app.models.Transaction,
        currentUserId: String
    ): TransactionHistory {
        val isReceived = transaction.toUserId == currentUserId
        val isSent = transaction.fromUserId == currentUserId && transaction.fromUserId != "system"
        
        // Determine transaction type
        val type = when {
            isReceived -> "received"
            isSent -> "sent"
            else -> "sent" // Default for system transactions
        }
        
        // Format amount with +/- prefix
        val amountPrefix = if (isReceived) "+" else "-"
        val formattedAmount = "$amountPrefix$${String.format("%.2f", transaction.amount)} ${transaction.currency}"
        
        // Convert status
        val statusString = when (transaction.status) {
            com.cashpal.app.models.TransactionStatus.COMPLETED -> "completed"
            com.cashpal.app.models.TransactionStatus.PENDING -> "pending"
            com.cashpal.app.models.TransactionStatus.FAILED -> "failed"
            com.cashpal.app.models.TransactionStatus.CANCELLED -> "failed"
            com.cashpal.app.models.TransactionStatus.REFUNDED -> "completed"
        }
        
        // Format date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(transaction.createdAt.toDate())
        
        // Get merchant/name from description or other user
        val merchantName = transaction.description.ifEmpty { 
            if (isReceived) "Received Payment" else "Sent Payment"
        }
        
        // Create reference from transaction ID
        val reference = "TXN-${transaction.id.take(8)}"
        
        return TransactionHistory(
            name = merchantName,
            reference = reference,
            amount = formattedAmount,
            status = statusString,
            date = dateString,
            type = type
        )
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val selectedTab = when (tabLayout.selectedTabPosition) {
                    1 -> "sent"
                    2 -> "received"
                    else -> "all"
                }
                filterTransactions(selectedTab, s.toString())
            }
        })
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))
        tabLayout.addTab(tabLayout.newTab().setText("Received"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabType = when (tab?.position) {
                    1 -> "sent"
                    2 -> "received"
                    else -> "all"
                }
                filterTransactions(tabType, searchEditText.text.toString())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        // Filter by State (Success/Fail)
        requireView().findViewById<View>(R.id.btn_filter_state).setOnClickListener {
            showFilterOptionsDialog()
        }
        
        // Download/Export button
        requireView().findViewById<View>(R.id.btn_download_csv).setOnClickListener {
            showExportOptionsDialog()
        }
    }

    /** ---------------- FILTERS ---------------- */
    private fun showFilterOptionsDialog() {
        val options = arrayOf("Filter by State", "Filter by Date", "Filter by Month")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Filter")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showStateFilterDialog()
                1 -> showDateFilterDialog()
                2 -> showMonthFilterDialog()
            }
        }
        builder.show()
    }

    private fun showStateFilterDialog() {
        val states = arrayOf("All", "Success", "Fail")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter by State")
            .setItems(states) { _, which ->
                val selectedState = states[which].lowercase()
                filterTransactionsByState(selectedState)
            }
            .show()
    }

    private fun showDateFilterDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                filterTransactionsByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showMonthFilterDialog() {
        val months = arrayOf(
            "All Time", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter by Month")
            .setItems(months) { _, which ->
                if (which == 0) {
                    // All Time - clear month filter
                    activeMonthFilter = null
                    activeDateFilter = null
                    applyAllFilters()
                } else {
                    // Filter by selected month
                    // Array index 1 = January = "01", index 2 = February = "02", etc.
                    val selectedMonth = String.format("%02d", which) // 01-12
                    filterTransactionsByMonth(selectedMonth)
                }
            }
            .show()
    }

    private fun filterTransactionsByState(state: String) {
        activeStateFilter = when (state) {
            "all" -> null
            else -> state
        }
        applyAllFilters()
    }

    private fun filterTransactionsByDate(date: String) {
        activeDateFilter = date
        activeMonthFilter = null // Clear month filter when date is selected
        applyAllFilters()
    }
    
    private fun filterTransactionsByMonth(month: String) {
        activeMonthFilter = month
        activeDateFilter = null // Clear date filter when month is selected
        applyAllFilters()
    }
    
    /**
     * Apply all active filters and update the filtered transactions list
     */
    private fun applyAllFilters() {
        var filtered = allTransactions
        
        // Apply date filter
        activeDateFilter?.let { date ->
            filtered = filtered.filter { it.date == date }
        }
        
        // Apply month filter
        activeMonthFilter?.let { month ->
            filtered = filtered.filter { transaction ->
                val dateParts = transaction.date.split("-")
                if (dateParts.size >= 2) {
                    dateParts[1] == month // Compare month part (MM)
                } else {
                    false
                }
            }
        }
        
        // Apply state filter
        activeStateFilter?.let { state ->
            filtered = when (state) {
                "success" -> filtered.filter { it.status == "completed" }
                "fail" -> filtered.filter { it.status == "pending" || it.status == "failed" }
                else -> filtered
            }
        }
        
        filteredTransactions = filtered
        
        // Apply search and tab filters
        val selectedTab = when (tabLayout.selectedTabPosition) {
            1 -> "sent"
            2 -> "received"
            else -> "all"
        }
        filterTransactions(selectedTab, searchEditText.text.toString())
    }

    /** ---------------- SEARCH + TABS ---------------- */

    private fun filterTransactions(type: String, query: String) {
        // Start with the base filtered transactions (from date/month/state filters)
        var filtered = filteredTransactions
        
        // Apply search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { transaction ->
                transaction.name.contains(query, ignoreCase = true) ||
                transaction.reference.contains(query, ignoreCase = true)
            }
        }
        
        // Apply tab filter (type)
        if (type != "all") {
            filtered = filtered.filter { it.type == type }
        }
        
        // Update the display
        transactionAdapter.updateTransactions(filtered)
        calculateTotals(filtered)
    }

    /** ---------------- EXPORT ---------------- */
    
    private fun showExportOptionsDialog() {
        val options = arrayOf("Export as CSV", "Export as PDF")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Export Transactions")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> exportTransactionsToCSV()
                1 -> exportTransactionsToPDF()
            }
        }
        builder.show()
    }

    private fun exportTransactionsToCSV() {
        try {
            // Use filtered transactions for export
            val transactionsToExport = filteredTransactions.ifEmpty { allTransactions }
            
            if (transactionsToExport.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                return
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "transactions_$timestamp.csv"
            
            // Use app's external files directory (accessible via file manager)
            val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null) {
                Toast.makeText(requireContext(), "Failed to access downloads directory", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create Downloads directory if it doesn't exist
            downloadsDir.mkdirs()
            
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Name,Reference,Amount,Status,Date,Type\n")
                
                // Write transaction data
                for (transaction in transactionsToExport) {
                    // Escape CSV special characters (commas, quotes, newlines)
                    val name = escapeCsvField(transaction.name)
                    val reference = escapeCsvField(transaction.reference)
                    val amount = escapeCsvField(transaction.amount)
                    val status = escapeCsvField(transaction.status)
                    val date = escapeCsvField(transaction.date)
                    val type = escapeCsvField(transaction.type)
                    
                    writer.append("$name,$reference,$amount,$status,$date,$type\n")
                }
            }

            // Share the file
            shareFile(file, "text/csv", "Transaction History CSV")
            
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportTransactionsToPDF() {
        try {
            // Use filtered transactions for export
            val transactionsToExport = filteredTransactions.ifEmpty { allTransactions }
            
            if (transactionsToExport.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                return
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "transactions_$timestamp.pdf"
            
            // Use app's external files directory
            val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null) {
                Toast.makeText(requireContext(), "Failed to access downloads directory", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create Downloads directory if it doesn't exist
            downloadsDir.mkdirs()
            
            val file = File(downloadsDir, fileName)
            
            // Create PDF content
            val pdfContent = StringBuilder()
            pdfContent.append("Transaction History\n")
            pdfContent.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            pdfContent.append("=".repeat(80)).append("\n\n")
            
            // Add summary
            val totalSent = transactionsToExport
                .filter { it.type == "sent" && it.status == "completed" }
                .sumOf { 
                    val amountStr = it.amount.replace(Regex("[^0-9.-]"), "")
                    amountStr.toDoubleOrNull() ?: 0.0
                }
            
            val totalReceived = transactionsToExport
                .filter { it.type == "received" && it.status == "completed" }
                .sumOf { 
                    val amountStr = it.amount.replace(Regex("[^0-9.-]"), "")
                    amountStr.toDoubleOrNull() ?: 0.0
                }
            
            pdfContent.append("Summary:\n")
            pdfContent.append("Total Sent: $${String.format("%.2f", totalSent)}\n")
            pdfContent.append("Total Received: $${String.format("%.2f", totalReceived)}\n")
            pdfContent.append("=".repeat(80)).append("\n\n")
            
            // Add transactions
            pdfContent.append("Transactions:\n\n")
            transactionsToExport.forEachIndexed { index, transaction ->
                pdfContent.append("${index + 1}. ${transaction.name}\n")
                pdfContent.append("   Reference: ${transaction.reference}\n")
                pdfContent.append("   Amount: ${transaction.amount}\n")
                pdfContent.append("   Status: ${transaction.status}\n")
                pdfContent.append("   Date: ${transaction.date}\n")
                pdfContent.append("   Type: ${transaction.type}\n")
                pdfContent.append("\n")
            }
            
            // Write PDF file (simple text-based PDF)
            file.writeText(pdfContent.toString())
            
            // Share the file
            shareFile(file, "application/pdf", "Transaction History PDF")
            
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Escape CSV field values to handle commas, quotes, and newlines
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
    
    /**
     * Share file using Android's share intent
     */
    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share $title")
            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            startActivity(chooserIntent)
            
            Toast.makeText(
                requireContext(),
                "$title exported successfully!\nFile saved to: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Failed to share file", e)
            Toast.makeText(
                requireContext(),
                "File saved to: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** ---------------- TOTALS ---------------- */

    /**
     * Calculate totals from the provided transactions list
     * This allows totals to reflect filtered transactions (by date, month, state, etc.)
     */
    private fun calculateTotals(transactions: List<TransactionHistory> = filteredTransactions) {
        val totalSent = transactions
            .filter { it.type == "sent" && it.status == "completed" }
            .sumOf { 
                // Extract numeric amount from string like "-$25.00 AUD" or "-$25.00"
                val amountStr = it.amount.replace(Regex("[^0-9.-]"), "")
                amountStr.toDoubleOrNull() ?: 0.0
            }

        val totalReceived = transactions
            .filter { it.type == "received" && it.status == "completed" }
            .sumOf { 
                // Extract numeric amount from string like "+$25.00 AUD" or "+$25.00"
                val amountStr = it.amount.replace(Regex("[^0-9.-]"), "")
                amountStr.toDoubleOrNull() ?: 0.0
            }

        totalSentTextView.text = "$%.2f".format(totalSent)
        totalReceivedTextView.text = "$%.2f".format(totalReceived)
    }
    private fun openReceipt(tx: TransactionHistory) {
        // amount in your model is a string like "+$25.00" or "-$4.50"
        val rawAmount = tx.amount.replace(Regex("[^0-9.-]"), "")
        val amountDouble = rawAmount.toDoubleOrNull() ?: 0.0

        // infer type/status from your model fields
        val isReceived = tx.type.equals("received", true) || tx.amount.trim().startsWith("+")
        val typeStr = if (isReceived) "received" else "sent"
        val statusStr = when (tx.status.lowercase(Locale.ROOT)) {
            "completed", "success" -> "completed"
            "pending" -> "pending"
            else -> "failed"
        }

        // Build args that ReceiptFragment expects (same keys used by Notification deep-link)
        val args = Bundle().apply {
            putString("transactionId", "TXN-${System.currentTimeMillis()}")
            putString("senderName", tx.name)                 // shown as counterparty on the receipt
            putDouble("amount", abs(amountDouble))
            putString("timestamp", tx.date)                  // you can add time if you have it
            putString("status", statusStr)                   // "completed" | "pending" | "failed"
            putString("type", typeStr)                       // "received" | "sent"
            // optional extras you may want later:
            putString("reference", tx.reference)
        }

        val receiptFragment = com.cashpal.app.fragments.ReceiptFragment().apply {
            arguments = args
        }

        // Navigate to the receipt page
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, receiptFragment)
            .addToBackStack(null)
            .commit()
    }

}
