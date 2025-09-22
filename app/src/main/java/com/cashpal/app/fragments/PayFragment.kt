package com.cashpal.app.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.data.Contact
import com.cashpal.app.data.ContactRepository
import com.cashpal.app.data.PaymentRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class PayFragment : Fragment() {

    private lateinit var contactRepository: ContactRepository

    // UI Views
    private lateinit var searchInput: TextInputEditText
    private lateinit var addContactCard: MaterialCardView
    private lateinit var frequentContactsList: RecyclerView
    private lateinit var allContactsList: RecyclerView
    private lateinit var selectedContactView: LinearLayout
    private lateinit var selectedContactName: TextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var cancelButton: MaterialButton
    private lateinit var payButton: MaterialButton

    // Adapters
    private lateinit var frequentContactsAdapter: ContactAdapter
    private lateinit var allContactsAdapter: ContactAdapter

    // Data
    private var allContacts: List<Contact> = emptyList()
    private var frequentContacts: List<Contact> = emptyList()
    private var selectedContact: Contact? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRepository()
        setupRecyclerViews()
        setupClickListeners()
        loadContacts()
        setupSearch()
    }

    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)
        addContactCard = view.findViewById(R.id.addContactCard)
        frequentContactsList = view.findViewById(R.id.frequentContactsList)
        allContactsList = view.findViewById(R.id.allContactsList)
        selectedContactView = view.findViewById(R.id.selectedContactView)
        selectedContactName = view.findViewById(R.id.selectedContactName)
        amountInput = view.findViewById(R.id.amountInput)
        cancelButton = view.findViewById(R.id.cancelButton)
        payButton = view.findViewById(R.id.payButton)
    }

    private fun setupRepository() {
        contactRepository = ContactRepository(requireContext())
    }

    private fun setupRecyclerViews() {
        // Setup frequent contacts list
        frequentContactsAdapter = ContactAdapter(requireContext()) { contact ->
            onContactSelected(contact)
        }
        frequentContactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = frequentContactsAdapter
        }

        // Setup all contacts list
        allContactsAdapter = ContactAdapter(requireContext()) { contact ->
            onContactSelected(contact)
        }
        allContactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = allContactsAdapter
        }
    }

    private fun setupClickListeners() {
        addContactCard.setOnClickListener {
            showAddContactDialog()
        }

        cancelButton.setOnClickListener {
            hidePaymentSection()
        }

        payButton.setOnClickListener {
            processPayment()
        }
    }

    private fun loadContacts() {
        allContacts = contactRepository.getContacts()
        frequentContacts = contactRepository.getFrequentContacts()

        frequentContactsAdapter.updateContacts(frequentContacts)
        allContactsAdapter.updateContacts(allContacts)
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            val filteredContacts = contactRepository.searchContacts(query)
            allContactsAdapter.updateContacts(filteredContacts)

            // Hide frequent contacts when searching
            if (query.isNotEmpty()) {
                frequentContactsList.visibility = View.GONE
                view?.findViewById<TextView>(R.id.frequentTitle)?.visibility = View.GONE
            } else {
                frequentContactsList.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.frequentTitle)?.visibility = View.VISIBLE
                allContactsAdapter.updateContacts(allContacts)
            }
        }
    }

    private fun onContactSelected(contact: Contact) {
        selectedContact = contact
        selectedContactName.text = "Send money to ${contact.name}"
        selectedContactView.visibility = View.VISIBLE
        amountInput.requestFocus()

        // Hide keyboard helper
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(amountInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hidePaymentSection() {
        selectedContactView.visibility = View.GONE
        selectedContact = null
        amountInput.text?.clear()

        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(amountInput.windowToken, 0)
    }

    private fun processPayment() {
        val contact = selectedContact
        val amountText = amountInput.text?.toString()

        if (contact == null) {
            Toast.makeText(requireContext(), "Please select a contact", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountText.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Create payment request
        val paymentRequest = PaymentRequest(
            contact = contact,
            amount = amount
        )

        // In a real app, you would process the payment here
        showPaymentConfirmation(paymentRequest)
    }

    private fun showPaymentConfirmation(paymentRequest: PaymentRequest) {
        val message = "Payment of $${String.format("%.2f", paymentRequest.amount)} to ${paymentRequest.contact.name} has been processed!"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        hidePaymentSection()

        // Optional: Navigate back to home or show success screen
    }

    private fun showAddContactDialog() {
        // For now, just show a toast. In a real app, you'd show a dialog or new screen
        Toast.makeText(requireContext(), "Add Contact feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    // Contact Adapter for RecyclerView
    private class ContactAdapter(
        private val context: Context,
        private val onContactClick: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

        private var contacts: List<Contact> = emptyList()

        fun updateContacts(newContacts: List<Contact>) {
            contacts = newContacts
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val contact = contacts[position]
            holder.bind(contact, onContactClick)
        }

        override fun getItemCount(): Int = contacts.size

        class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val contactAvatar: ImageView = itemView.findViewById(R.id.contactAvatar)
            private val contactName: TextView = itemView.findViewById(R.id.contactName)
            private val contactEmail: TextView = itemView.findViewById(R.id.contactEmail)
            private val lastTransaction: TextView = itemView.findViewById(R.id.lastTransaction)
            private val frequentBadge: TextView = itemView.findViewById(R.id.frequentBadge)

            fun bind(contact: Contact, onContactClick: (Contact) -> Unit) {
                contactName.text = contact.name
                contactEmail.text = contact.email ?: contact.phone ?: "No contact info"

                // Show last transaction if available
                if (!contact.lastTransactionDate.isNullOrEmpty()) {
                    lastTransaction.text = "Last transaction: ${contact.lastTransactionDate}"
                    lastTransaction.visibility = View.VISIBLE
                } else {
                    lastTransaction.visibility = View.GONE
                }

                // Show frequent badge if contact is frequent
                if (contact.isFrequent) {
                    frequentBadge.visibility = View.VISIBLE
                } else {
                    frequentBadge.visibility = View.GONE
                }

                // Set click listener
                itemView.setOnClickListener {
                    onContactClick(contact)
                }

                // Set avatar (for now using default icon, could be enhanced with real images)
                contactAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }
}