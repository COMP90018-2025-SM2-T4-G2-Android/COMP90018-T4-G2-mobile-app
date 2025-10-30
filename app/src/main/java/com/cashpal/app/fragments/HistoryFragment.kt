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
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi


class HistoryFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var totalSentTextView: TextView
    private lateinit var totalReceivedTextView: TextView

    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private var allTransactions = listOf<TransactionHistory>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot save CSV.", Toast.LENGTH_SHORT).show()
            }
        }


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


        val downloadCsvButton: Button = view.findViewById(R.id.btn_download_csv)

        downloadCsvButton.setOnClickListener {
            if (allTransactions.isNotEmpty()) {
                exportTransactionsToCSV(allTransactions)
            } else {
                Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun initViews() {
        searchEditText = requireView().findViewById(R.id.et_search)
        tabLayout = requireView().findViewById(R.id.tab_layout)
        transactionsRecyclerView = requireView().findViewById(R.id.rv_transactions)
        totalSentTextView = requireView().findViewById(R.id.tv_total_sent)
        totalReceivedTextView = requireView().findViewById(R.id.tv_total_received)

        transactionsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupData() {
        allTransactions = listOf(
            TransactionHistory("Coffee Shop", "Order #12345", "-$4.50", "completed", "2024-01-15", "sent"),
            TransactionHistory("John Doe", "Split dinner bill", "+$25.00", "completed", "2024-01-14", "received"),
            TransactionHistory("Online Store", "Order #67890", "-$89.99", "pending", "2024-01-13", "sent"),
            TransactionHistory("Sarah Wilson", "Movie tickets", "+$15.00", "completed", "2024-01-12", "received"),
            TransactionHistory("Gas Station", "Fuel purchase", "-$45.67", "completed", "2024-01-11", "sent"),
            TransactionHistory("Freelance Client", "Project payment", "+$350.00", "completed", "2024-01-10", "received")
        )

        transactionAdapter = TransactionHistoryAdapter(allTransactions) {}
        transactionsRecyclerView.adapter = transactionAdapter

        calculateTotals()
        filterTransactions("all", "")
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
        requireView().findViewById<View>(R.id.btn_filter_state).setOnClickListener {
            showFilterOptionsDialog()
        }
        requireView().findViewById<View>(R.id.btn_download_csv).setOnClickListener {
            if (allTransactions.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to export yet", Toast.LENGTH_SHORT).show()
            } else {
                exportTransactionsToCSV()
            }
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

    @SuppressLint("Range")
    private fun exportTransactionsToCSV(transactions: List<TransactionHistory>) {
        val fileName = "transactions_${System.currentTimeMillis()}.csv"

        val csvContent = StringBuilder()
        csvContent.append("Name,Reference,Amount,Status,Date,Type\n")
        for (tx in transactions) {
            csvContent.append("${tx.name},${tx.reference},${tx.amount},${tx.status},${tx.date},${tx.type}\n")
        }

        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv") // CSV mime type
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toString().toByteArray())
                outputStream.flush()
            }

            Toast.makeText(requireContext(), "CSV saved to Downloads/$fileName", Toast.LENGTH_SHORT).show()

            // Try to open with CSV viewer
            val csvIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(csvIntent)
            } catch (e: Exception) {
                // Fallback: open as plain text
                val textIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/plain")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(textIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "File saved but cannot be opened on this device", Toast.LENGTH_LONG).show()
                }
            }

        } else {
            Toast.makeText(requireContext(), "Failed to save CSV", Toast.LENGTH_SHORT).show()
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
