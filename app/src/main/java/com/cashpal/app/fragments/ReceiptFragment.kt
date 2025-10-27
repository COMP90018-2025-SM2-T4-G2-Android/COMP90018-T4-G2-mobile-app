package com.cashpal.app.fragments

import android.content.ClipData
import com.itextpdf.layout.property.TextAlignment
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import java.io.File
import com.cashpal.app.R
import com.cashpal.app.models.Transaction
import com.cashpal.app.models.TransactionStatus
import com.cashpal.app.models.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class ReceiptFragment : Fragment() {
    private var transaction: Transaction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_receipt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get transaction from arguments
        transaction = arguments?.getParcelable(ARG_TRANSACTION)
        
        setupViews(view)
    }

    private fun setupViews(view: View) {
        transaction?.let { transaction ->
            // Header
            view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            // Status Card
            setupStatusCard(view, transaction)

            // Transaction Details
            setupTransactionDetails(view, transaction)

            // Action Buttons
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

            // Copy Transaction ID
            view.findViewById<ImageButton>(R.id.btn_copy_id).setOnClickListener {
                copyToClipboard(transaction.id)
            }
        }
    }

    private fun setupStatusCard(view: View, transaction: Transaction) {
        val statusCard = view.findViewById<CardView>(R.id.cv_status)
        val statusIcon = view.findViewById<ImageView>(R.id.iv_status_icon)
        val statusBadge = view.findViewById<LinearLayout>(R.id.ll_status_badge)
        val statusText = view.findViewById<TextView>(R.id.tv_status)
        val amountLabel = view.findViewById<TextView>(R.id.tv_amount_label)
        val amountText = view.findViewById<TextView>(R.id.tv_amount)
        val statusMessage = view.findViewById<TextView>(R.id.tv_status_message)
        val helpText = view.findViewById<TextView>(R.id.tv_help_text)

        when (transaction.status) {
            TransactionStatus.COMPLETED -> {
                setupCompletedState(statusCard, statusIcon, statusBadge, statusText, 
                    statusMessage, helpText, transaction)
            }
            TransactionStatus.PENDING -> {
                setupPendingState(statusCard, statusIcon, statusBadge, statusText, 
                    statusMessage, helpText)
            }
            else -> {
                setupFailedState(statusCard, statusIcon, statusBadge, statusText, 
                    statusMessage, helpText)
            }
        }

        // Set amount
        val isIncoming = transaction.toUserId == requireContext()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", "") ?: ""

        amountLabel.text = if (isIncoming) "Amount Received" else "Amount Sent"
        
        val amountPrefix = if (isIncoming) "+" else "-"
        amountText.text = "$amountPrefix$${String.format("%.2f", transaction.amount)}"
        
        val amountColor = if (isIncoming) R.color.received_color else R.color.sent_color
        amountText.setTextColor(ContextCompat.getColor(requireContext(), amountColor))
    }

    private fun setupCompletedState(
        statusCard: CardView,
        statusIcon: ImageView,
        statusBadge: LinearLayout,
        statusText: TextView,
        statusMessage: TextView,
        helpText: TextView,
        transaction: Transaction
    ) {
        statusCard.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.received_background)
        )
        statusIcon.setImageResource(R.drawable.ic_check_circle)
        statusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.success)
        )
        statusBadge.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.success)
        statusText.text = "COMPLETED"
        statusText.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.success)
        )
        
        val isIncoming = transaction.toUserId == requireContext()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", "") ?: ""
            
        statusMessage.text = if (isIncoming) {
            "✓ Payment received successfully"
        } else {
            "✓ Payment sent successfully"
        }
        statusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.success)
        )
        helpText.text = "This transaction has been completed. If you have any questions, please contact support."
    }

    private fun setupPendingState(
        statusCard: CardView,
        statusIcon: ImageView,
        statusBadge: LinearLayout,
        statusText: TextView,
        statusMessage: TextView,
        helpText: TextView
    ) {
        statusCard.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.warning_background)
        )
        statusIcon.setImageResource(R.drawable.ic_clock)
        statusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.warning)
        )
        statusBadge.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.warning)
        statusText.text = "PENDING"
        statusText.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.warning)
        )
        statusMessage.text = "⏱ Transaction is being processed"
        statusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.warning)
        )
        helpText.text = "Your transaction is being processed. You'll receive a notification once it's complete."
    }

    private fun setupFailedState(
        statusCard: CardView,
        statusIcon: ImageView,
        statusBadge: LinearLayout,
        statusText: TextView,
        statusMessage: TextView,
        helpText: TextView
    ) {
        statusCard.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.error_background)
        )
        statusIcon.setImageResource(R.drawable.ic_x_circle)
        statusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
        statusBadge.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.error)
        statusText.text = "FAILED"
        statusText.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
        statusMessage.text = "✗ Transaction failed - funds not transferred"
        statusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
        helpText.text = "This transaction failed. Your funds have been returned to your account. Please try again or contact support if you need help."
    }

    private fun setupTransactionDetails(view: View, transaction: Transaction) {
        // Recipient info
        val recipientInitials = transaction.merchantName?.take(2)?.uppercase() ?: "??"
        view.findViewById<TextView>(R.id.tv_recipient_initials).text = recipientInitials
        view.findViewById<TextView>(R.id.tv_recipient_name).text = transaction.merchantName ?: "Unknown"

        // Reference and metadata
        view.findViewById<TextView>(R.id.tv_reference).text = transaction.description
        view.findViewById<TextView>(R.id.tv_transaction_id).text = transaction.id
        
        // Format date and time
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = transaction.createdAt.toDate()
        
        view.findViewById<TextView>(R.id.tv_date).text = dateFormat.format(date)
        view.findViewById<TextView>(R.id.tv_time).text = timeFormat.format(date)
        
        // Payment details
        view.findViewById<TextView>(R.id.tv_payment_method).text = transaction.paymentMethod ?: "CashPal Balance"
        view.findViewById<TextView>(R.id.tv_from_account).text = transaction.fromAccount ?: "****1234"
        view.findViewById<TextView>(R.id.tv_transaction_fee).text = "$${String.format("%.2f", transaction.transactionFee)}"
        
        // Calculate and display total amount
        val totalAmount = transaction.amount + transaction.transactionFee
        view.findViewById<TextView>(R.id.tv_total_amount).text = "$${String.format("%.2f", totalAmount)}"
    }

    private fun shareReceipt(transaction: Transaction) {
        // Create options dialog
        val options = arrayOf("Share as Text", "Share as PDF")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Share Receipt")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> shareAsText(transaction)
                    1 -> shareAsPDF(transaction)
                }
            }
            .show()
    }
    
    private fun shareAsText(transaction: Transaction) {
        val receiptText = """
            CashPal Transaction Receipt
            ---------------------------
            Transaction ID: ${transaction.id}
            Status: ${transaction.status}
            Amount: $${String.format("%.2f", transaction.amount)}
            To: ${transaction.merchantName ?: "Unknown"}
            Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                .format(transaction.createdAt.toDate())}
            Reference: ${transaction.description}
            Payment Method: ${transaction.paymentMethod ?: "CashPal Balance"}
            Transaction Fee: $${String.format("%.2f", transaction.transactionFee)}
            Total Amount: $${String.format("%.2f", transaction.amount + transaction.transactionFee)}
            ---------------------------
            Thank you for using CashPal!
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            putExtra(Intent.EXTRA_SUBJECT, "CashPal Receipt")
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
        Toast.makeText(requireContext(), "Sharing receipt as text...", Toast.LENGTH_SHORT).show()
    }

    private fun shareAsPDF(transaction: Transaction) {
        try {
            // Create PDF file in cache directory
            val filename = "CashPal_Receipt_${transaction.id}.pdf"
            val cacheDir = requireContext().cacheDir
            val file = File(cacheDir, filename)
            
            // Initialize PDF writer and document
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Add content
            document.add(Paragraph("CashPal Transaction Receipt")
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            
            document.add(Paragraph("Transaction Details")
                .setFontSize(18f)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold()
                .setMarginTop(20f))
            
            // Add transaction details
            val details = listOf(
                "Transaction ID: ${transaction.id}",
                "Status: ${transaction.status}",
                "Amount: $${String.format("%.2f", transaction.amount)}",
                "To: ${transaction.merchantName ?: "Unknown"}",
                "Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    .format(transaction.createdAt.toDate())}",
                "Reference: ${transaction.description}",
                "Payment Method: ${transaction.paymentMethod ?: "CashPal Balance"}",
                "From Account: ${transaction.fromAccount ?: "****1234"}",
                "Transaction Fee: $${String.format("%.2f", transaction.transactionFee)}",
                "Total Amount: $${String.format("%.2f", transaction.amount + transaction.transactionFee)}"
            )
            
            details.forEach { detail ->
                document.add(Paragraph(detail)
                    .setFontSize(12f)
                    .setMarginTop(5f))
            }
            
            // Close document
            document.close()
            
            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            // Share PDF
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "CashPal Receipt")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
            Toast.makeText(requireContext(), "Sharing receipt as PDF...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            // Fallback to text sharing
            shareAsText(transaction)
        }
    }

    private fun downloadReceipt(transaction: Transaction) {
        try {
            // Create PDF file
            val filename = "CashPal_Receipt_${transaction.id}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            
            // Initialize PDF writer and document
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Add content
            document.add(Paragraph("CashPal Transaction Receipt")
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold())
            
            document.add(Paragraph("Transaction Details")
                .setFontSize(18f)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold()
                .setMarginTop(20f))
            
            // Add transaction details
            val details = listOf(
                "Transaction ID: ${transaction.id}",
                "Status: ${transaction.status}",
                "Amount: $${String.format("%.2f", transaction.amount)}",
                "To: ${transaction.merchantName ?: "Unknown"}",
                "Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    .format(transaction.createdAt.toDate())}",
                "Reference: ${transaction.description}",
                "Payment Method: ${transaction.paymentMethod ?: "CashPal Balance"}",
                "From Account: ${transaction.fromAccount ?: "****1234"}",
                "Transaction Fee: $${String.format("%.2f", transaction.transactionFee)}",
                "Total Amount: $${String.format("%.2f", transaction.amount + transaction.transactionFee)}"
            )
            
            details.forEach { detail ->
                document.add(Paragraph(detail)
                    .setFontSize(12f)
                    .setMarginTop(5f))
            }
            
            // Close document
            document.close()
            
            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            // Open PDF with system viewer
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            startActivity(Intent.createChooser(intent, "View Receipt"))
            Toast.makeText(requireContext(), "Receipt downloaded as PDF", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Transaction ID", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Transaction ID copied", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_TRANSACTION = "arg_transaction"

        fun newInstance(transaction: Transaction): ReceiptFragment {
            return ReceiptFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction)
                }
            }
        }
    }
}
