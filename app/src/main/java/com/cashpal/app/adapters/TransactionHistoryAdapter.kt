package com.cashpal.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.models.TransactionHistory

import androidx.fragment.app.FragmentManager
import com.cashpal.app.fragments.ReceiptFragment

class TransactionHistoryAdapter(
    private var transactions: List<TransactionHistory>,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<TransactionHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.tv_avatar)
        val name: TextView = view.findViewById(R.id.tv_name)
        val reference: TextView = view.findViewById(R.id.tv_reference)
        val date: TextView = view.findViewById(R.id.tv_date)
        val amount: TextView = view.findViewById(R.id.tv_amount)
        val status: TextView = view.findViewById(R.id.tv_status)
        val container: View = view.findViewById(R.id.container_transaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        // Set avatar initials
        val initials = transaction.name.split(' ').mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
        holder.avatar.text = initials.take(2).uppercase()

        holder.name.text = transaction.name
        holder.reference.text = transaction.reference
        holder.date.text = transaction.date
        holder.amount.text = transaction.amount
        holder.status.text = transaction.status.uppercase()

        // Set amount color based on type
        val isPositive = transaction.amount.startsWith("+")
        holder.amount.setTextColor(
            if (isPositive) Color.parseColor("#22C55E") else Color.parseColor("#EF4444")
        )

        // Set status styling
        if (transaction.status == "completed") {
            holder.status.setBackgroundResource(R.drawable.status_completed_background)
            holder.status.setTextColor(Color.parseColor("#16A34A"))
        } else {
            holder.status.setBackgroundResource(R.drawable.status_pending_background)
            holder.status.setTextColor(Color.parseColor("#CA8A04"))
        }

        holder.container.setOnClickListener {
            // Navigate to receipt fragment
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ReceiptFragment.newInstance(transaction.toTransaction()))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<TransactionHistory>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    private fun TransactionHistory.toTransaction(): com.cashpal.app.models.Transaction {
        return com.cashpal.app.models.Transaction(
            id = reference,  // Using reference as transaction ID
            fromUserId = if (amount.startsWith("-")) "user_id" else "other_user",
            toUserId = if (amount.startsWith("-")) "other_user" else "user_id",
            amount = amount.replace(Regex("[^0-9.]"), "").toDouble(),
            description = reference,
            status = when (status.lowercase()) {
                "completed" -> com.cashpal.app.models.TransactionStatus.COMPLETED
                "pending" -> com.cashpal.app.models.TransactionStatus.PENDING
                else -> com.cashpal.app.models.TransactionStatus.FAILED
            },
            merchantName = name
        )
    }
}