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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.adapters.TransactionHistoryAdapter
import com.cashpal.app.models.TransactionHistory
import com.cashpal.app.repository.CashPalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayout
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
    private lateinit var repository: CashPalRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    // Going through CashPallApp provides implicit check that we're in app's authentication flow
    private fun setupData() {
        repository = (requireActivity().application as com.cashpal.app.CashPalApp)
            .ServiceLocator.getCashPalRepository()
            
        transactionAdapter = TransactionHistoryAdapter(emptyList(), parentFragmentManager)
        transactionsRecyclerView.adapter = transactionAdapter

        loadTransactions()
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

    private fun loadTransactions() {
        val userId = repository.getCurrentUserId() ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            repository.getUserTransactions(userId).collect { result ->
                result.onSuccess { transactions ->
                    // Convert Firestore transactions to UI model
                    allTransactions = transactions.map { transaction ->
                        val isIncoming = transaction.toUserId == userId
                        val amount = if (isIncoming) "+$${transaction.amount}" else "-$${transaction.amount}"
                        
                        TransactionHistory(
                            name = transaction.merchantName ?: "Unknown",
                            reference = transaction.id,
                            amount = amount,
                            status = transaction.status.toString().lowercase(),
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(transaction.createdAt.toDate()),
                            type = if (isIncoming) "received" else "sent"
                        )
                    }
                    
                    transactionAdapter.updateTransactions(allTransactions)
                    calculateTotals()
                    filterTransactions("all", "")
                }
            }
        }
        
        // Set up real-time listener for updates
        repository.listenToUserTransactions(userId) { transactions ->
            // Convert and update transactions same as above
            allTransactions = transactions.map { transaction ->
                val isIncoming = transaction.toUserId == userId
                val amount = if (isIncoming) "+$${transaction.amount}" else "-$${transaction.amount}"
                
                TransactionHistory(
                    name = transaction.merchantName ?: "Unknown",
                    reference = transaction.id,
                    amount = amount,
                    status = transaction.status.toString().lowercase(),
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(transaction.createdAt.toDate()),
                    type = if (isIncoming) "received" else "sent"
                )
            }
            
            transactionAdapter.updateTransactions(allTransactions)
            calculateTotals()
            filterTransactions("all", searchEditText.text.toString())
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
}
