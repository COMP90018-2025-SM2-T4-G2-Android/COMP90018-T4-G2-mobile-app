package com.cashpal.app.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.auth.MfaGuard
import com.cashpal.app.data.Contact
import com.cashpal.app.data.ContactRepository
import com.cashpal.app.data.PaymentRequest
import com.cashpal.app.utils.DuplicatePaymentGuard
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

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
    ): View? = inflater.inflate(R.layout.fragment_pay, container, false)

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
        frequentContactsAdapter = ContactAdapter(requireContext()) { contact -> onContactSelected(contact) }
        frequentContactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = frequentContactsAdapter
        }

        allContactsAdapter = ContactAdapter(requireContext()) { contact -> onContactSelected(contact) }
        allContactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = allContactsAdapter
        }
    }

    private fun setupClickListeners() {
        addContactCard.setOnClickListener { showAddContactDialog() }
        cancelButton.setOnClickListener { hidePaymentSection() }
        payButton.setOnClickListener { processPayment() }
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
            val filtered = contactRepository.searchContacts(query)
            allContactsAdapter.updateContacts(filtered)

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
        selectedContactName.text = getString(R.string.pay_send_to_fmt, contact.name)
        selectedContactView.visibility = View.VISIBLE
        amountInput.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(amountInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hidePaymentSection() {
        selectedContactView.visibility = View.GONE
        selectedContact = null
        amountInput.text?.clear()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(amountInput.windowToken, 0)
    }

    // ---------- Payment flow with duplicate check + MFA ----------
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

        val paymentRequest = PaymentRequest(contact = contact, amount = amount)

        // If this looks like a duplicate (same contact + same amount within window), require MFA.
        if (DuplicatePaymentGuard.isDuplicate(requireContext(), paymentRequest)) {
            MfaGuard.requireAuth(requireActivity()) {
                finalizePayment(paymentRequest)
                DuplicatePaymentGuard.save(requireContext(), paymentRequest)
            }
        } else {
            finalizePayment(paymentRequest)
            DuplicatePaymentGuard.save(requireContext(), paymentRequest)
        }
    }

    private fun finalizePayment(paymentRequest: PaymentRequest) {
        val message = "Payment of $${String.format(Locale.US, "%.2f", paymentRequest.amount)} to ${paymentRequest.contact.name} has been processed!"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        hidePaymentSection()

        // If you show a local notification for "payment sent", you can call it here.
        // NotificationService.showPaymentSentNotification(requireContext(), paymentRequest.contact.name, paymentRequest.amount)
    }

    private fun showAddContactDialog() {
        Toast.makeText(requireContext(), "Add Contact feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    // -------- RecyclerView Adapter --------
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

                if (!contact.lastTransactionDate.isNullOrEmpty()) {
                    lastTransaction.text = "Last transaction: ${contact.lastTransactionDate}"
                    lastTransaction.visibility = View.VISIBLE
                } else {
                    lastTransaction.visibility = View.GONE
                }

                frequentBadge.visibility = if (contact.isFrequent) View.VISIBLE else View.GONE
                itemView.setOnClickListener { onContactClick(contact) }
                contactAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }
}
