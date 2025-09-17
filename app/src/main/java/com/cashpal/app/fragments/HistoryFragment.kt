package com.cashpal.app.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.data.DataRepository
import com.cashpal.app.data.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var repository: DataRepository
    private lateinit var listView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private lateinit var searchInput: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipSent: Chip
    private lateinit var chipReceived: Chip
    private lateinit var totalSentView: TextView
    private lateinit var totalReceivedView: TextView
    private lateinit var downloadButton: MaterialButton
    private lateinit var statusDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView

    private var allTransactions: List<Transaction> = emptyList()
    private var filteredTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = DataRepository(requireContext())
        bindViews(view)
        setupList()
        loadData()
        setupInteractions()
    }

    private fun bindViews(root: View) {
        listView = root.findViewById(R.id.transactionsList)
        searchInput = root.findViewById(R.id.searchInput)
        chipGroup = root.findViewById(R.id.filterGroup)
        chipAll = root.findViewById(R.id.chipAll)
        chipSent = root.findViewById(R.id.chipSent)
        chipReceived = root.findViewById(R.id.chipReceived)
        totalSentView = root.findViewById(R.id.totalSent)
        totalReceivedView = root.findViewById(R.id.totalReceived)
        downloadButton = root.findViewById(R.id.downloadButton)
        statusDropdown = root.findViewById(R.id.statusDropdown)
    }

    private fun setupList() {
        adapter = TransactionsAdapter(requireContext())
        listView.layoutManager = LinearLayoutManager(requireContext())
        listView.adapter = adapter
    }

    private fun loadData() {
        val data = repository.loadAppData()
        allTransactions = data?.recentTransactions ?: emptyList()
        filteredTransactions = allTransactions
        applyFiltersAndSearch()
        updateTotals()
    }

    private fun setupInteractions() {
        searchInput.addTextChangedListener { applyFiltersAndSearch() }
        chipGroup.setOnCheckedStateChangeListener { _, _ -> applyFiltersAndSearch() }
        downloadButton.setOnClickListener { exportCsvAndShare() }
        setupStatusDropdown()
    }

    private fun setupStatusDropdown() {
        val items = listOf("All", "Completed", "Failed", "Pending")
        (statusDropdown.adapter as? android.widget.ArrayAdapter<String>) ?: run {
            statusDropdown.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items))
        }
        statusDropdown.setText("All", false)
        statusDropdown.setOnItemClickListener { _, _, _, _ -> applyFiltersAndSearch() }
    }

    private fun applyFiltersAndSearch() {
        val query = (searchInput.text?.toString() ?: "").trim().lowercase(Locale.getDefault())
        val selectedId = chipGroup.checkedChipId
        val mode: FilterMode = when (selectedId) {
            R.id.chipSent -> FilterMode.SENT
            R.id.chipReceived -> FilterMode.RECEIVED
            else -> FilterMode.ALL
        }

        val selectedStatus = statusDropdown.text?.toString()?.lowercase(Locale.getDefault()) ?: "all"
        filteredTransactions = allTransactions.filter { t ->
            val matchesMode = when (mode) {
                FilterMode.ALL -> true
                FilterMode.SENT -> !t.isIncoming
                FilterMode.RECEIVED -> t.isIncoming
            }
            val matchesStatus = when (selectedStatus) {
                "completed" -> t.status.equals("Completed", true)
                "failed" -> t.status.equals("Failed", true)
                "pending" -> t.status.equals("Pending", true)
                else -> true
            }
            val matchesQuery = if (query.isEmpty()) true else {
                t.merchant.lowercase(Locale.getDefault()).contains(query) ||
                    t.amount.lowercase(Locale.getDefault()).contains(query) ||
                    t.status.lowercase(Locale.getDefault()).contains(query)
            }
            matchesMode && matchesStatus && matchesQuery
        }
        adapter.submitList(filteredTransactions)
    }

    private fun updateTotals() {
        var sentCents = 0
        var receivedCents = 0
        allTransactions.forEach { t ->
            val amountCents = parseCurrencyToCents(t.amount)
            if (t.isIncoming) receivedCents += amountCents else sentCents += amountCents
        }
        totalSentView.text = formatCents(sentCents)
        totalReceivedView.text = formatCents(receivedCents)
    }

    private fun parseCurrencyToCents(s: String): Int {
        val cleaned = s.replace(Regex("[^0-9.-]"), "")
        val value = cleaned.toDoubleOrNull() ?: 0.0
        return (value * 100).toInt()
    }

    private fun formatCents(cents: Int): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.US)
        return nf.format(cents / 100.0)
    }

    private fun exportCsvAndShare() {
        if (allTransactions.isEmpty()) {
            Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }
        val header = "id,merchant,amount,timeAgo,status,isIncoming\n"
        val rows = allTransactions.joinToString("\n") { t ->
            listOf(
                escapeCsv(t.id),
                escapeCsv(t.merchant),
                escapeCsv(t.amount),
                escapeCsv(t.timeAgo),
                escapeCsv(t.status),
                t.isIncoming.toString()
            ).joinToString(",")
        }
        val csv = header + rows
        try {
            val file = createExportFile(requireContext(), "transactions.csv")
            FileOutputStream(file).use { it.write(csv.toByteArray()) }
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".provider",
                file
            )
            val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(share, "Export transactions"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createExportFile(context: Context, name: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, name)
    }

    private fun escapeCsv(s: String): String {
        val needsQuotes = s.contains(',') || s.contains('"') || s.contains('\n')
        var out = s.replace("\"", "\"\"")
        if (needsQuotes) out = "\"$out\""
        return out
    }

    private enum class FilterMode { ALL, SENT, RECEIVED }

    private class TransactionsAdapter(private val context: Context) : RecyclerView.Adapter<TransactionsAdapter.VH>() {
        private var items: List<Transaction> = emptyList()

        fun submitList(list: List<Transaction>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.merchant.text = item.merchant
            holder.time.text = item.timeAgo
            holder.amount.text = item.amount
            holder.status.text = item.status
            val amountColor = if (item.isIncoming) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            holder.amount.setTextColor(holder.itemView.resources.getColor(amountColor, null))
            val iconRes = when (item.icon) {
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
            holder.icon.setImageResource(iconRes)
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.icon)
            val merchant: TextView = itemView.findViewById(R.id.merchant)
            val time: TextView = itemView.findViewById(R.id.time)
            val amount: TextView = itemView.findViewById(R.id.amount)
            val status: TextView = itemView.findViewById(R.id.status)
        }
    }
}

