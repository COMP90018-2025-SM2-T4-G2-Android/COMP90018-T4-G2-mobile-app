package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.adapters.ContactAdapter
import com.cashpal.app.data.Contact
import com.cashpal.app.data.ContactRepository
import com.cashpal.app.data.PaymentRequest
import com.google.android.material.textfield.TextInputEditText

class PayFragment : Fragment() {

    private lateinit var contactRepository: ContactRepository

    private lateinit var searchInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var recentContactsRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: ContactAdapter
    private lateinit var recentContactsAdapter: ContactAdapter

    private var allContacts: List<Contact> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRepository()
        initializeViews(view)
        setupRecyclerViews()
        setupQuickAmountButtons(view)
        setupSendOptions(view)
        loadContacts()
        setupSearch()
    }

    private fun initializeRepository() {
        contactRepository = ContactRepository(requireContext())
    }

    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.et_search)
        amountInput = view.findViewById(R.id.et_amount)
        favoritesRecyclerView = view.findViewById(R.id.rv_favorites)
        recentContactsRecyclerView = view.findViewById(R.id.rv_recent_contacts)
    }

    private fun setupRecyclerViews() {
        favoritesAdapter = ContactAdapter(true) { contact ->
            onContactSelected(contact)
        }
        favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = favoritesAdapter
            setHasFixedSize(true)
        }

        recentContactsAdapter = ContactAdapter(false) { contact ->
            onContactSelected(contact)
        }
        recentContactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentContactsAdapter
        }
    }

    private fun setupQuickAmountButtons(view: View) {
        val quickAmounts = listOf("10", "25", "50", "100")
        val buttonIds = listOf(
            R.id.btn_amount_10,
            R.id.btn_amount_25,
            R.id.btn_amount_50,
            R.id.btn_amount_100
        )

        buttonIds.forEachIndexed { index, buttonId ->
            view.findViewById<Button>(buttonId).setOnClickListener {
                amountInput.setText(quickAmounts[index])
                amountInput.setSelection(amountInput.text?.length ?: 0)
            }
        }
    }

    private fun setupSendOptions(view: View) {
        val infoMessage = getString(R.string.pay_coming_soon)
        view.findViewById<ImageButton>(R.id.btn_send_phone).setOnClickListener {
            Toast.makeText(requireContext(), infoMessage, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<ImageButton>(R.id.btn_send_qr).setOnClickListener {
            Toast.makeText(requireContext(), infoMessage, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<ImageButton>(R.id.btn_send_contacts).setOnClickListener {
            Toast.makeText(requireContext(), infoMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts() {
        allContacts = contactRepository.getContacts()
        val favoriteContacts = contactRepository.getFrequentContacts()

        favoritesAdapter.updateContacts(favoriteContacts)
        recentContactsAdapter.updateContacts(allContacts)
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener { text ->
            val query = text?.toString().orEmpty()
            if (query.isBlank()) {
                recentContactsAdapter.updateContacts(allContacts)
                return@addTextChangedListener
            }

            val filteredContacts = contactRepository.searchContacts(query)
            recentContactsAdapter.updateContacts(filteredContacts)
        }
    }

    private fun onContactSelected(contact: Contact) {
        val amountValue = amountInput.text?.toString()?.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            val message = getString(R.string.pay_enter_amount_message, contact.name)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            return
        }

        val paymentRequest = PaymentRequest(contact, amountValue)
        showPaymentConfirmation(paymentRequest)
    }

    private fun showPaymentConfirmation(paymentRequest: PaymentRequest) {
        val message = getString(
            R.string.pay_success_message,
            paymentRequest.amount,
            paymentRequest.contact.name
        )
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        amountInput.text?.clear()
    }
}
