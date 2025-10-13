package com.cashpal.app.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.cashpal.app.R

class AddCardDialogFragment : DialogFragment() {
    
    // UI Components
    private lateinit var btnCloseAddCard: ImageView
    private lateinit var btnScanCard: Button
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var etCvv: EditText
    private lateinit var etCardholderName: EditText
    private lateinit var etBillingAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etPostalCode: EditText
    private lateinit var etState: EditText
    private lateinit var etCountry: EditText
    private lateinit var cbSetAsPrimary: CheckBox
    private lateinit var btnCancelAddCard: Button
    private lateinit var btnSaveCard: Button
    
    // Card Preview Components
    private lateinit var tvCardPreviewNumber: TextView
    private lateinit var tvCardPreviewExpiry: TextView
    private lateinit var tvCardPreviewCvv: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set normal dialog style
        setStyle(STYLE_NORMAL, R.style.Theme_CashPal)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_card, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupTextWatchers()
        setupClickListeners()
    }
    
    override fun onStart() {
        super.onStart()
        // Set dialog to take up full screen
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
    
    private fun initViews(view: View) {
        btnCloseAddCard = view.findViewById(R.id.btn_close_add_card)
        btnScanCard = view.findViewById(R.id.btn_scan_card)
        etCardNumber = view.findViewById(R.id.et_card_number)
        etExpiryDate = view.findViewById(R.id.et_expiry_date)
        etCvv = view.findViewById(R.id.et_cvv)
        etCardholderName = view.findViewById(R.id.et_cardholder_name)
        etBillingAddress = view.findViewById(R.id.et_billing_address)
        etCity = view.findViewById(R.id.et_city)
        etPostalCode = view.findViewById(R.id.et_postal_code)
        etState = view.findViewById(R.id.et_state)
        etCountry = view.findViewById(R.id.et_country)
        cbSetAsPrimary = view.findViewById(R.id.cb_set_as_primary)
        btnCancelAddCard = view.findViewById(R.id.btn_cancel_add_card)
        btnSaveCard = view.findViewById(R.id.btn_save_card)
        
        tvCardPreviewNumber = view.findViewById(R.id.tv_card_preview_number)
        tvCardPreviewExpiry = view.findViewById(R.id.tv_card_preview_expiry)
        tvCardPreviewCvv = view.findViewById(R.id.tv_card_preview_cvv)
    }
    
    private fun setupTextWatchers() {
        // Card Number - Format as 1234 5678 9012 3456
        etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val input = s.toString().replace(" ", "")
                val formatted = StringBuilder()
                
                for (i in input.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ")
                    }
                    formatted.append(input[i])
                }
                
                etCardNumber.removeTextChangedListener(this)
                etCardNumber.setText(formatted.toString())
                etCardNumber.setSelection(formatted.length)
                etCardNumber.addTextChangedListener(this)
                
                // Update preview
                updateCardPreview()
                isFormatting = false
            }
        })
        
        // Expiry Date - Format as MM/YY
        etExpiryDate.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val input = s.toString().replace("/", "")
                val formatted = StringBuilder()
                
                for (i in input.indices) {
                    if (i == 2) {
                        formatted.append("/")
                    }
                    if (i < 4) {
                        formatted.append(input[i])
                    }
                }
                
                etExpiryDate.removeTextChangedListener(this)
                etExpiryDate.setText(formatted.toString())
                etExpiryDate.setSelection(formatted.length)
                etExpiryDate.addTextChangedListener(this)
                
                // Update preview
                updateCardPreview()
                isFormatting = false
            }
        })
        
        // CVV - Update preview
        etCvv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCardPreview()
            }
        })
    }
    
    private fun updateCardPreview() {
        // Update card number preview
        val cardNumber = etCardNumber.text.toString()
        tvCardPreviewNumber.text = if (cardNumber.isEmpty()) {
            "•••• •••• •••• ••••"
        } else {
            cardNumber.padEnd(19, '•')
        }
        
        // Update expiry preview
        val expiry = etExpiryDate.text.toString()
        tvCardPreviewExpiry.text = if (expiry.isEmpty()) "MM/YY" else expiry
        
        // Update CVV preview
        val cvv = etCvv.text.toString()
        tvCardPreviewCvv.text = if (cvv.isEmpty()) {
            "•••"
        } else {
            "•".repeat(cvv.length)
        }
    }
    
    private fun setupClickListeners() {
        btnCloseAddCard.setOnClickListener {
            dismiss()
        }
        
        btnScanCard.setOnClickListener {
            showToast("Card scanning - Coming soon!")
            // TODO: Implement card scanning with ML Kit or similar
            // You can use ML Kit's text recognition to scan card details
        }
        
        btnCancelAddCard.setOnClickListener {
            dismiss()
        }
        
        btnSaveCard.setOnClickListener {
            if (validateInputs()) {
                saveCard()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val cardNumber = etCardNumber.text.toString().replace(" ", "")
        val expiryDate = etExpiryDate.text.toString()
        val cvv = etCvv.text.toString()
        val cardholderName = etCardholderName.text.toString().trim()
        val billingAddress = etBillingAddress.text.toString().trim()
        val city = etCity.text.toString().trim()
        val postalCode = etPostalCode.text.toString().trim()
        val state = etState.text.toString().trim()
        val country = etCountry.text.toString().trim()
        
        return when {
            cardNumber.isEmpty() -> {
                etCardNumber.error = "Card number is required"
                etCardNumber.requestFocus()
                false
            }
            cardNumber.length < 13 -> {
                etCardNumber.error = "Invalid card number"
                etCardNumber.requestFocus()
                false
            }
            expiryDate.isEmpty() -> {
                etExpiryDate.error = "Expiry date is required"
                etExpiryDate.requestFocus()
                false
            }
            expiryDate.length < 5 -> {
                etExpiryDate.error = "Invalid expiry date"
                etExpiryDate.requestFocus()
                false
            }
            cvv.isEmpty() -> {
                etCvv.error = "CVV is required"
                etCvv.requestFocus()
                false
            }
            cvv.length < 3 -> {
                etCvv.error = "Invalid CVV"
                etCvv.requestFocus()
                false
            }
            cardholderName.isEmpty() -> {
                etCardholderName.error = "Cardholder name is required"
                etCardholderName.requestFocus()
                false
            }
            billingAddress.isEmpty() -> {
                etBillingAddress.error = "Billing address is required"
                etBillingAddress.requestFocus()
                false
            }
            city.isEmpty() -> {
                etCity.error = "City is required"
                etCity.requestFocus()
                false
            }
            postalCode.isEmpty() -> {
                etPostalCode.error = "Postal code is required"
                etPostalCode.requestFocus()
                false
            }
            state.isEmpty() -> {
                etState.error = "State is required"
                etState.requestFocus()
                false
            }
            country.isEmpty() -> {
                etCountry.error = "Country is required"
                etCountry.requestFocus()
                false
            }
            else -> true
        }
    }
    
    private fun saveCard() {
        val cardNumber = etCardNumber.text.toString().replace(" ", "")
        val lastFour = cardNumber.takeLast(4)
        val isPrimary = cbSetAsPrimary.isChecked
        
        // TODO: Implement actual card saving logic
        // - Tokenize card with payment processor (Stripe, Braintree, etc.)
        // - Save to database
        // - Update UI
        
        showToast("Card •••• $lastFour added successfully!")
        dismiss()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        const val TAG = "AddCardDialogFragment"
        
        fun newInstance(): AddCardDialogFragment {
            return AddCardDialogFragment()
        }
    }
}

