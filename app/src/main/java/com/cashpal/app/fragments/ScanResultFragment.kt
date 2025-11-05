package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cashpal.app.MainActivity
import com.cashpal.app.R
import com.cashpal.app.NotificationService
import com.cashpal.app.BuildConfig
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.QRPayload
import com.cashpal.app.models.TransactionType
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class ScanResultFragment : Fragment() {

    private var qrPayload: QRPayload? = null
    private var qrContact: String? = null
    private lateinit var amountInput: TextInputEditText
    private lateinit var sendButton: Button
    private lateinit var saveFavoriteButton: Button
    private lateinit var sendProgress: ProgressBar
    private var isProcessingTransfer = false
    private var rawQrData: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scan_result, container, false)

        // ----- get data from arguments -----
        val qrData = arguments?.getString("qrData")
        rawQrData = qrData

        amountInput = view.findViewById(R.id.et_transfer_amount)
        sendButton = view.findViewById(R.id.btn_send_money)
        saveFavoriteButton = view.findViewById(R.id.btn_save_favorite)
        sendProgress = view.findViewById(R.id.progress_send_money)

        try {
            val json = JSONObject(qrData ?: "{}")

            val payload = QRPayload(
                id = json.optString("id"),
                name = json.optString("name", "Unknown"),
                phone = json.optString("phone", ""),
                type = json.optString("type", "personal"),
                amount = if (json.has("amount") && !json.isNull("amount")) json.optDouble("amount") else null
            )
            qrPayload = payload
            qrContact = json.optString("contact", "").takeIf { it.isNotBlank() }

            // ----- bind views -----
            val tvName          = view.findViewById<TextView>(R.id.tv_scanned_name)
            val tvIdentifier    = view.findViewById<TextView>(R.id.tv_scanned_identifier)
            val tvAmount        = view.findViewById<TextView>(R.id.tv_scanned_amount)
            val llAmountBadge   = view.findViewById<LinearLayout>(R.id.ll_amount_badge)

            val flAvatar        = view.findViewById<FrameLayout>(R.id.fl_person_avatar)
            val ivStore         = view.findViewById<ImageView>(R.id.iv_store_icon)
            val ivQr            = view.findViewById<ImageView>(R.id.iv_qr_icon)

            // ----- populate -----
            tvName.text = payload.name
            val identifierIcon = view.findViewById<ImageView>(R.id.iv_identifier_icon)
            val identifier = qrContact
                ?: payload.phone.takeIf { it.isNotBlank() }
                ?: payload.id.ifBlank { "N/A" }
            tvIdentifier.text = identifier

            var identifierIconRes = when {
                !qrContact.isNullOrBlank() && qrContact!!.contains("@") -> R.drawable.ic_mail
                !qrContact.isNullOrBlank() -> R.drawable.ic_phone
                payload.type.equals("payment", ignoreCase = true) -> R.drawable.ic_qr_scanner
                payload.type.equals("store", ignoreCase = true) -> R.drawable.ic_store
                else -> R.drawable.ic_phone
            }

            if ((payload.amount ?: 0.0) > 0.0) {
                llAmountBadge.visibility = View.VISIBLE
                tvAmount.text = "$" + String.format("%.2f", payload.amount)
            } else {
                llAmountBadge.visibility = View.GONE
            }

            // ----- switch icon/action set by type -----
            when (payload.type.lowercase()) {
                "store" -> {
                    ivStore.visibility = View.VISIBLE
                    ivQr.visibility = View.GONE
                    flAvatar.visibility = View.GONE
                    if (qrContact.isNullOrBlank()) {
                        identifierIconRes = R.drawable.ic_store
                    }
                }
                "payment" -> {
                    ivQr.visibility = View.VISIBLE
                    ivStore.visibility = View.GONE
                    flAvatar.visibility = View.GONE
                    if (qrContact.isNullOrBlank()) {
                        identifierIconRes = R.drawable.ic_qr_scanner
                    }
                }
                else -> { // personal
                    flAvatar.visibility = View.VISIBLE
                    ivStore.visibility = View.GONE
                    ivQr.visibility = View.GONE

                    val initialsView = view.findViewById<TextView>(R.id.tv_avatar_initials)
                    initialsView.text = payload.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "?" }
                }
            }

            identifierIcon.setImageResource(identifierIconRes)

            setupAmountInput(payload)
            configureActionButtons(payload)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid QR data", Toast.LENGTH_SHORT).show()
        }

        // ----- header back -----
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ----- quick actions -----
        view.findViewById<LinearLayout>(R.id.btn_scan_again).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ScanFragment())
                .addToBackStack("scan_again")
                .commit()
        }

        view.findViewById<LinearLayout>(R.id.btn_view_history).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment())
                .addToBackStack("history")
                .commit()
        }

        // "Scan Another Code" button in the card
        view.findViewById<View>(R.id.btn_scan_another)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ScanFragment())
                .addToBackStack("scan_again")
                .commit()
        }

        return view
    }

    private fun setupAmountInput(payload: QRPayload) {
        val preset = payload.amount
        if (preset != null && preset > 0) {
            amountInput.setText(String.format("%.2f", preset))
        }
        amountInput.doAfterTextChanged {
            updateSendButtonState()
        }
        updateSendButtonState()
    }

    private fun configureActionButtons(payload: QRPayload) {
        val isVendor = payload.type.equals("store", ignoreCase = true) || payload.type.equals("payment", ignoreCase = true)
        saveFavoriteButton.visibility = if (isVendor) View.VISIBLE else View.GONE
        saveFavoriteButton.setOnClickListener {
            Toast.makeText(requireContext(), "Saving to favorites...", Toast.LENGTH_SHORT).show()
        }

        sendButton.setOnClickListener {
            initiateTransfer()
        }

        updateSendButtonState()
    }

    private fun updateSendButtonState() {
        val amount = amountInput.text?.toString()?.trim()?.toDoubleOrNull()
        val enabled = amount != null && amount > 0 && !isProcessingTransfer
        sendButton.isEnabled = enabled
        val buttonText = if (amount != null && amount > 0) {
            getString(
                R.string.send_money_cta_with_amount,
                getString(R.string.send_money_currency_format, amount)
            )
        } else {
            getString(R.string.send_money_cta)
        }
        sendButton.text = buttonText
    }

    private fun initiateTransfer() {
        val payload = qrPayload ?: run {
            Toast.makeText(requireContext(), "Scan information unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val amountValue = amountInput.text?.toString()?.trim().orEmpty()
        val amount = amountValue.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), R.string.send_money_amount_error, Toast.LENGTH_SHORT).show()
            return
        }

        val repository = try {
            ServiceLocator.getRepository()
        } catch (e: IllegalStateException) {
            Toast.makeText(requireContext(), "Service unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = repository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(requireContext(), R.string.pay_user_not_signed_in, Toast.LENGTH_SHORT).show()
            return
        }

        if (payload.id.isBlank()) {
            Toast.makeText(requireContext(), R.string.send_money_missing_recipient, Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser.uid == payload.id) {
            Toast.makeText(requireContext(), R.string.pay_self_transfer_error, Toast.LENGTH_SHORT).show()
            return
        }

        setProcessing(true)

        val description = getString(R.string.send_money_default_note, payload.name)
        val recipientContact = qrContact ?: payload.phone.takeIf { it.isNotBlank() }
        val metadata = mutableMapOf<String, Any>(
            "qrSource" to "scan"
        ).apply {
            val qrType = payload.type.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
            if (!qrType.isNullOrBlank()) {
                this["qrType"] = qrType
            }
            qrContact?.takeIf { it.isNotBlank() }?.let { contact ->
                this["qrContact"] = contact
            }
            payload.phone.takeIf { it.isNotBlank() }?.let { phone ->
                this["qrPhone"] = phone
            }
            payload.id.takeIf { it.isNotBlank() }?.let { id ->
                this["qrRecipientId"] = id
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.processTransaction(
                    fromUserId = currentUser.uid,
                    toUserId = payload.id,
                    amount = amount,
                    description = description,
                    transactionType = TransactionType.PAYMENT,
                    qrCodeData = rawQrData,
                    metadata = if (metadata.isEmpty()) emptyMap() else metadata
                ).collect { result ->
                    result.fold(
                        onSuccess = { transactionId ->
                            Toast.makeText(requireContext(), getString(R.string.send_money_success, payload.name), Toast.LENGTH_SHORT).show()
                            (activity as? MainActivity)?.refreshBalance()
                            openReceipt(transactionId, payload, amount, description, recipientContact)
                            NotificationService.showPaymentSentNotification(requireContext(), payload.name, amount)

                            if (BuildConfig.DEBUG) {
                                NotificationService.showPaymentReceivedNotification(
                                    requireContext(),
                                    senderName = payload.name,
                                    amount = amount,
                                    timestamp = getString(R.string.notification_mock_timestamp)
                                )
                            }
                        },
                        onFailure = { error ->
                            val reason = error.localizedMessage?.takeIf { it.isNotBlank() }
                            val message = reason?.let {
                                getString(R.string.pay_transfer_failed, it)
                            } ?: getString(R.string.send_money_failed_generic)
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                val message = e.localizedMessage?.takeIf { it.isNotBlank() } ?: getString(R.string.send_money_failed_generic)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } finally {
                setProcessing(false)
            }
        }
    }

    private fun openReceipt(
        transactionId: String?,
        payload: QRPayload,
        amount: Double,
        description: String,
        recipientContact: String?
    ) {
        val receiptArgs = Bundle().apply {
            putString("transactionId", transactionId ?: "")
            putString("status", "completed")
            putString("type", "sent")
            putDouble("amount", amount)
            putString("recipientName", payload.name)
            putString("recipientPhone", recipientContact)
            putString("reference", description)
            putString("paymentMethod", getString(R.string.send_money_payment_method))
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ReceiptFragment().apply { arguments = receiptArgs })
            .addToBackStack("receipt")
            .commit()
    }

    private fun setProcessing(loading: Boolean) {
        isProcessingTransfer = loading
        sendProgress.visibility = if (loading) View.VISIBLE else View.GONE
        amountInput.isEnabled = !loading
        updateSendButtonState()
    }
}
