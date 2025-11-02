package com.cashpal.app.fragments

import android.app.DatePickerDialog
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
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var totalSentTextView: TextView
    private lateinit var totalReceivedTextView: TextView

    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private var allTransactions = listOf<TransactionHistory>()
    
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
            calculateTotals()
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
                        transactionAdapter.updateTransactions(allTransactions)
                        calculateTotals()
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
                        calculateTotals()
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
// Filter button handles both state + date
        requireView().findViewById<View>(R.id.btn_filter_state).setOnClickListener {
            showFilterOptionsDialog()
        }

    }

    /** ---------------- FILTERS ---------------- */
    private fun showFilterOptionsDialog() {
        val options = arrayOf("Filter by State", "Filter by Date")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Filter")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showStateFilterDialog()
                1 -> showDateFilterDialog()
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

    private fun filterTransactionsByState(state: String) {
        val filtered = when (state) {
            "success" -> allTransactions.filter { it.status == "completed" }
            "fail" -> allTransactions.filter { it.status == "pending" || it.status == "failed" }
            else -> allTransactions
        }
        transactionAdapter.updateTransactions(filtered)
    }

    private fun filterTransactionsByDate(date: String) {
        val filtered = allTransactions.filter { it.date == date }
        transactionAdapter.updateTransactions(filtered)
    }

    /** ---------------- SEARCH + TABS ---------------- */

    private fun filterTransactions(type: String, query: String) {
        val filtered = allTransactions.filter { transaction ->
            val matchesSearch = transaction.name.contains(query, ignoreCase = true) ||
                    transaction.reference.contains(query, ignoreCase = true)
            val matchesType = (type == "all" || transaction.type == type)
            matchesSearch && matchesType
        }
        transactionAdapter.updateTransactions(filtered)
    }

    /** ---------------- EXPORT ---------------- */

    private fun exportTransactionsToCSV() {
        try {
            val fileName = "transactions_${System.currentTimeMillis()}.csv"
            val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("Name,Reference,Amount,Status,Date,Type\n")
                for (transaction in allTransactions) {
                    writer.append("${transaction.name},${transaction.reference},${transaction.amount},${transaction.status},${transaction.date},${transaction.type}\n")
                }
            }

            Toast.makeText(requireContext(), "CSV saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }

    /** ---------------- TOTALS ---------------- */

    private fun calculateTotals() {
        val totalSent = allTransactions
            .filter { it.type == "sent" && it.status == "completed" }
            .sumOf { it.amount.removePrefix("-$").toDoubleOrNull() ?: 0.0 }

        val totalReceived = allTransactions
            .filter { it.type == "received" && it.status == "completed" }
            .sumOf { it.amount.removePrefix("+$").toDoubleOrNull() ?: 0.0 }

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
            putDouble("amount", kotlin.math.abs(amountDouble))
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
