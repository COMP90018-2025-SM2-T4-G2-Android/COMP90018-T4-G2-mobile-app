package com.cashpal.app.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.cashpal.app.R

class PaymentMethodsDialogFragment : DialogFragment() {
    
    private lateinit var btnClosePayment: ImageView
    private lateinit var btnEditVisa: TextView
    private lateinit var btnRemoveVisa: TextView
    private lateinit var btnEditMastercard: TextView
    private lateinit var btnRemoveMastercard: TextView
    private lateinit var btnSetPrimaryMastercard: Button
    private lateinit var btnAddNewCard: Button
    private lateinit var btnLinkBank: Button
    
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
        return inflater.inflate(R.layout.dialog_payment_methods, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    
    private fun initViews(view: View) {
        btnClosePayment = view.findViewById(R.id.btn_close_payment)
        btnEditVisa = view.findViewById(R.id.btn_edit_visa)
        btnRemoveVisa = view.findViewById(R.id.btn_remove_visa)
        btnEditMastercard = view.findViewById(R.id.btn_edit_mastercard)
        btnRemoveMastercard = view.findViewById(R.id.btn_remove_mastercard)
        btnSetPrimaryMastercard = view.findViewById(R.id.btn_set_primary_mastercard)
        btnAddNewCard = view.findViewById(R.id.btn_add_new_card)
        btnLinkBank = view.findViewById(R.id.btn_link_bank)
    }
    
    private fun setupClickListeners() {
        btnClosePayment.setOnClickListener {
            dismiss()
        }
        
        // Visa Card Actions
        btnEditVisa.setOnClickListener {
            showToast("Edit Visa card")
            // TODO: Open edit card dialog
        }
        
        btnRemoveVisa.setOnClickListener {
            showToast("Remove Visa card")
            // TODO: Show confirmation dialog and remove card
        }
        
        // Mastercard Actions
        btnEditMastercard.setOnClickListener {
            showToast("Edit Mastercard")
            // TODO: Open edit card dialog
        }
        
        btnRemoveMastercard.setOnClickListener {
            showToast("Remove Mastercard")
            // TODO: Show confirmation dialog and remove card
        }
        
        btnSetPrimaryMastercard.setOnClickListener {
            showToast("Mastercard set as primary")
            // TODO: Update primary card in database
            // Refresh the UI to show the updated primary status
        }
        
        // Add New Card
        btnAddNewCard.setOnClickListener {
            openAddCardDialog()
        }
        
        // Link Bank
        btnLinkBank.setOnClickListener {
            showToast("Link bank account - Coming soon!")
            // TODO: Open bank linking flow (e.g., Plaid integration)
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun openAddCardDialog() {
        val addCardDialog = AddCardDialogFragment.newInstance()
        addCardDialog.show(childFragmentManager, AddCardDialogFragment.TAG)
    }
    
    companion object {
        const val TAG = "PaymentMethodsDialogFragment"
        
        fun newInstance(): PaymentMethodsDialogFragment {
            return PaymentMethodsDialogFragment()
        }
    }
}

