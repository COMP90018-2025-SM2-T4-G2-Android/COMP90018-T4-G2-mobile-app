package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cashpal.app.R

class ReceiptFragment : Fragment() {

    // Transaction data class
    data class TransactionDetails(
        val id: String,
        val status: TransactionStatus,
        val amount: Double,
        val type: TransactionType,
        val recipientName: String,
        val recipientInitials: String,
        val recipientPhone: String?,
        val recipientAccount: String?,
        val senderAccount: String,
        val reference: String,
        val date: String,
        val time: String,
        val transactionFee: Double,
        val paymentMethod: String
    )

    enum class TransactionStatus { COMPLETED, PENDING, FAILED }
    enum class TransactionType { SENT, RECEIVED }

    // Default mock
    private val mock = TransactionDetails(
        id = "TXN-2024-0115-12345",
        status = TransactionStatus.COMPLETED,
        amount = 125.50,
        type = TransactionType.SENT,
        recipientName = "Sarah Johnson",
        recipientInitials = "SJ",
        recipientPhone = "+1 (555) 987-6543",
        recipientAccount = "****4892",
        senderAccount = "****1234",
        reference = "Dinner split payment",
        date = "January 15, 2024",
        time = "2:45 PM",
        transactionFee = 0.00,
        paymentMethod = "CashPal Balance"
    )

    private lateinit var transaction: TransactionDetails

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipt, container, false)

        // Build "transaction" from arguments if present
        transaction = arguments?.let { b ->
            val id = b.getString("transactionId") ?: mock.id
            val statusStr = (b.getString("status") ?: "completed").lowercase()
            val status = when (statusStr) {
                "pending" -> TransactionStatus.PENDING
                "failed" -> TransactionStatus.FAILED
                else -> TransactionStatus.COMPLETED
            }
            val typeStr = (b.getString("type") ?: "sent").lowercase()
            val type = if (typeStr == "received") TransactionType.RECEIVED else TransactionType.SENT

            val amount = b.getDouble("amount", mock.amount)
            val recipientName = b.getString("recipientName")
                ?: b.getString("senderName") // for received
                ?: mock.recipientName

            val initials = recipientName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }

            TransactionDetails(
                id = id,
                status = status,
                amount = amount,
                type = type,
                recipientName = recipientName,
                recipientInitials = initials,
                recipientPhone = b.getString("recipientPhone"),
                recipientAccount = b.getString("recipientAccount"),
                senderAccount = b.getString("fromAccount") ?: mock.senderAccount,
                reference = b.getString("reference") ?: "—",
                date = b.getString("date") ?: "Today",
                time = b.getString("time") ?: (b.getString("timestamp") ?: "Just now"),
                transactionFee = b.getDouble("fee", mock.transactionFee),
                paymentMethod = b.getString("paymentMethod") ?: mock.paymentMethod
            )
        } ?: mock

        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        // Back
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Status Card
        setupStatusCard(view, transaction)
        // Details
        setupTransactionDetails(view, transaction)

        view.findViewById<LinearLayout>(R.id.btn_share_receipt).setOnClickListener {
            shareReceipt(transaction)
        }
        view.findViewById<LinearLayout>(R.id.btn_download_receipt).setOnClickListener {
            downloadReceipt(transaction)
        }
        view.findViewById<Button>(R.id.btn_view_all_transactions).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<Button>(R.id.btn_done).setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
        view.findViewById<ImageButton>(R.id.btn_copy_id).setOnClickListener {
            copyToClipboard(transaction.id)
        }
    }

    private fun setupStatusCard(view: View, tx: TransactionDetails) {
        val statusCard = view.findViewById<CardView>(R.id.cv_status)
        val statusIcon = view.findViewById<ImageView>(R.id.iv_status_icon)
        val statusBadge = view.findViewById<LinearLayout>(R.id.ll_status_badge)
        val statusText = view.findViewById<TextView>(R.id.tv_status)
        val amountLabel = view.findViewById<TextView>(R.id.tv_amount_label)
        val amountText = view.findViewById<TextView>(R.id.tv_amount)
        val statusMessage = view.findViewById<TextView>(R.id.tv_status_message)
        val helpText = view.findViewById<TextView>(R.id.tv_help_text)

        when (tx.status) {
            TransactionStatus.COMPLETED -> {
                statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.received_background))
                statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                statusBadge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.success)
                statusText.text = "Completed"
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                statusMessage.text = if (tx.type == TransactionType.SENT) "✓ Payment sent successfully" else "✓ Payment received successfully"
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                helpText.text = "This transaction has been completed. If you have any questions, please contact support."
            }
            TransactionStatus.PENDING -> {
                statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning))
                statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning))
                statusIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                statusBadge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.warning)
                statusText.text = "Pending"
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                statusMessage.text = "⏱ Transaction is being processed"
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                helpText.text = "Your transaction is being processed. You'll receive a notification once it's complete."
            }
            TransactionStatus.FAILED -> {
                statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sent_background))
                statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                statusIcon.setImageResource(android.R.drawable.ic_delete)
                statusBadge.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error)
                statusText.text = "Failed"
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                statusMessage.text = "✗ Transaction failed - funds not transferred"
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                helpText.text = "This transaction failed. Your funds have been returned to your account."
            }
        }

        amountLabel.text = if (tx.type == TransactionType.SENT) "Amount Sent" else "Amount Received"
        amountText.text = if (tx.type == TransactionType.SENT)
            "-$${String.format("%.2f", tx.amount)}" else "+$${String.format("%.2f", tx.amount)}"
        amountText.setTextColor(ContextCompat.getColor(requireContext(),
            if (tx.type == TransactionType.SENT) R.color.sent_color else R.color.received_color
        ))
    }

    private fun setupTransactionDetails(view: View, tx: TransactionDetails) {
        view.findViewById<TextView>(R.id.tv_recipient_initials).text = tx.recipientInitials
        view.findViewById<TextView>(R.id.tv_recipient_name).text = tx.recipientName

        val phone = view.findViewById<TextView>(R.id.tv_recipient_phone)
        phone.text = tx.recipientPhone ?: ""
        phone.visibility = if (tx.recipientPhone.isNullOrBlank()) View.GONE else View.VISIBLE

        val acct = view.findViewById<TextView>(R.id.tv_recipient_account)
        acct.text = tx.recipientAccount?.let { "Account: $it" } ?: ""
        acct.visibility = if (tx.recipientAccount.isNullOrBlank()) View.GONE else View.VISIBLE

        view.findViewById<TextView>(R.id.tv_reference).text = tx.reference
        view.findViewById<TextView>(R.id.tv_transaction_id).text = tx.id
        view.findViewById<TextView>(R.id.tv_date).text = tx.date
        view.findViewById<TextView>(R.id.tv_time).text = tx.time
        view.findViewById<TextView>(R.id.tv_payment_method).text = tx.paymentMethod
        view.findViewById<TextView>(R.id.tv_from_account).text = tx.senderAccount
        view.findViewById<TextView>(R.id.tv_transaction_fee).text = "$${String.format("%.2f", tx.transactionFee)}"
        view.findViewById<TextView>(R.id.tv_total_amount).text = "$${String.format("%.2f", tx.amount + tx.transactionFee)}"
    }

    private fun shareReceipt(tx: TransactionDetails) {
        val text = """
            CashPal Transaction Receipt
            ---------------------------
            Transaction ID: ${tx.id}
            Status: ${tx.status.name}
            Amount: $${String.format("%.2f", tx.amount)}
            ${if (tx.type == TransactionType.SENT) "To" else "From"}: ${tx.recipientName}
            Date: ${tx.date} at ${tx.time}
            Reference: ${tx.reference}
        """.trimIndent()

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "CashPal Receipt")
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(intent, "Share Receipt"))
        Toast.makeText(requireContext(), "Sharing receipt...", Toast.LENGTH_SHORT).show()
    }

    private fun downloadReceipt(@Suppress("UNUSED_PARAMETER") tx: TransactionDetails) {
        Toast.makeText(requireContext(), "Receipt downloaded as PDF", Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Transaction ID", text))
        Toast.makeText(requireContext(), "Transaction ID copied", Toast.LENGTH_SHORT).show()
    }
}
