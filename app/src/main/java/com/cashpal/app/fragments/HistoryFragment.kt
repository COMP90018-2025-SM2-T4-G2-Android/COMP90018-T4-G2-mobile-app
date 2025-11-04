package com.cashpal.app.fragments

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
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
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.adapters.TransactionHistoryAdapter
import com.cashpal.app.models.TransactionHistory
import com.cashpal.app.ui.CsvViewerActivity
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.Charsets

class HistoryFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var totalSentTextView: TextView
    private lateinit var totalReceivedTextView: TextView

    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private var allTransactions = listOf<TransactionHistory>()
    private var pendingExportTransactions: List<TransactionHistory>? = null

    companion object {
        private const val REQUEST_WRITE_EXTERNAL = 2001
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

        transactionAdapter = TransactionHistoryAdapter(allTransactions) { tx ->
            openReceipt(tx)
        }
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
        // Filter by State (Success/Fail)
// Filter button handles both state + date
        requireView().findViewById<View>(R.id.btn_filter_state).setOnClickListener {
            showFilterOptionsDialog()
        }

        requireView().findViewById<View>(R.id.btn_download_csv).setOnClickListener {
            val currentList = transactionAdapter.currentTransactions()
            handleExportRequest(currentList)
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

    private fun exportTransactionsToCSV(transactions: List<TransactionHistory>) {
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "No transactions to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "cashpal_transactions_$timestamp.csv"

        var exportedUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/CashPal"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                                writeTransactionsToCsv(writer, transactions)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        exportedUri = uri
                        Toast.makeText(
                            requireContext(),
                            "CSV saved to Downloads/CashPal/$fileName",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: IOException) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    throw IOException("Unable to create download entry")
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw IOException("Downloads directory unavailable")
                val cashPalDir = File(downloadsDir, "CashPal").apply {
                    if (!exists() && !mkdirs()) {
                        throw IOException("Unable to create export folder")
                    }
                }
                val file = File(cashPalDir, fileName)

                FileWriter(file).use { writer ->
                    writeTransactionsToCsv(writer, transactions)
                }

                MediaScannerConnection.scanFile(
                    requireContext(),
                    arrayOf(file.absolutePath),
                    arrayOf("text/csv"),
                    null
                )

                exportedUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                Toast.makeText(
                    requireContext(),
                    "CSV saved: ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export CSV", Toast.LENGTH_SHORT).show()
            exportedUri = null
        }

        launchCsvPreview(transactions, exportedUri)
    }

    private fun writeTransactionsToCsv(writer: Appendable, transactions: List<TransactionHistory>) {
        writer.append('\uFEFF')
        writer.append("Name,Reference,Amount,Status,Date,Type\r\n")
        for (transaction in transactions) {
            writer.append(
                listOf(
                    transaction.name,
                    transaction.reference,
                    transaction.amount,
                    transaction.status,
                    transaction.date,
                    transaction.type
                ).joinToString(",") { value -> escapeCsv(value) }
            )
            writer.append("\r\n")
        }
    }

    private fun escapeCsv(raw: String?): String {
        val value = raw ?: ""
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun handleExportRequest(transactions: List<TransactionHistory>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val writeGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!writeGranted) {
                pendingExportTransactions = transactions
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL
                )
                return
            }
        }

        exportTransactionsToCSV(transactions)
        pendingExportTransactions = null
    }

    private fun launchCsvPreview(transactions: List<TransactionHistory>, uri: Uri?) {
        val html = buildCsvHtml(transactions)
        val previewIntent = android.content.Intent(requireContext(), CsvViewerActivity::class.java).apply {
            putExtra(CsvViewerActivity.EXTRA_HTML, html)
            uri?.let { putExtra(CsvViewerActivity.EXTRA_URI, it.toString()) }
        }
        startActivity(previewIntent)
    }

    private fun buildCsvHtml(transactions: List<TransactionHistory>): String {
        val builder = StringBuilder()
        builder.append(
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8"/>
            <style>
            body { font-family: sans-serif; margin: 0; padding: 16px; background: #f7f7f7; }
            table { width: 100%; border-collapse: collapse; background: white; }
            th, td { padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: left; }
            th { background: #0d47a1; color: white; position: sticky; top: 0; }
            tr:nth-child(even) { background: #fafafa; }
            .amount-positive { color: #22C55E; }
            .amount-negative { color: #EF4444; }
            </style>
            </head>
            <body>
            <h2>CashPal Transactions</h2>
            <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Reference</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Date</th>
                <th>Type</th>
              </tr>
            </thead>
            <tbody>
            """.trimIndent()
        )

        transactions.forEach { transaction ->
            val amountClass = if (transaction.amount.trim().startsWith("+")) "amount-positive" else "amount-negative"
            builder.append("<tr>")
            builder.append("<td>${escapeHtml(transaction.name)}</td>")
            builder.append("<td>${escapeHtml(transaction.reference)}</td>")
            builder.append("<td class=\"$amountClass\">${escapeHtml(transaction.amount)}</td>")
            builder.append("<td>${escapeHtml(transaction.status)}</td>")
            builder.append("<td>${escapeHtml(transaction.date)}</td>")
            builder.append("<td>${escapeHtml(transaction.type)}</td>")
            builder.append("</tr>")
        }

        builder.append(
            """
            </tbody>
            </table>
            </body>
            </html>
            """.trimIndent()
        )
        return builder.toString()
    }

    private fun escapeHtml(raw: String?): String {
        return raw
            ?.replace("&", "&amp;")
            ?.replace("<", "&lt;")
            ?.replace(">", "&gt;")
            ?.replace("\"", "&quot;")
            ?.replace("'", "&#39;")
            ?: ""
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                pendingExportTransactions?.let { exportTransactionsToCSV(it) }
            } else {
                Toast.makeText(requireContext(), "Storage permission denied.", Toast.LENGTH_SHORT).show()
            }
            pendingExportTransactions = null
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
