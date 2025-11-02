package com.cashpal.app.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.adapters.ContactAdapter
import com.cashpal.app.adapters.PayContactItem
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.Contact
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class PayFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var recentContactsRecyclerView: RecyclerView
    private lateinit var recentContactsCard: View
    private lateinit var favoritesEmptyView: TextView
    private lateinit var recentsEmptyView: TextView
    private lateinit var sendProgressIndicator: CircularProgressIndicator
    private lateinit var recipientNameTextView: TextView
    private lateinit var recipientSubtitleTextView: TextView
    private lateinit var sendPaymentButton: MaterialButton
    private lateinit var nfcCardView: View
    private lateinit var contactOptionView: View
    private lateinit var phoneOptionView: View
    private lateinit var qrOptionView: View
    private lateinit var vendorOptionView: View
    private lateinit var favoritesAdapter: ContactAdapter
    private lateinit var recentContactsAdapter: ContactAdapter

    private val repository by lazy { ServiceLocator.getRepository() }
    private var currentUserId: String? = null
    private var rootView: View? = null
    private var contactsJob: Job? = null
    private var isProcessingTransfer = false
    private var cachedContacts: List<Contact> = emptyList()
    private var hasLoadedContacts = false
    private var selectedRecipient: PayContactItem? = null

    private val colorPalette = listOf(
        "#6C5CE7",
        "#7E57FF",
        "#00B894",
        "#FDCB6E",
        "#FF7675",
        "#74B9FF",
        "#A29BFE"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        initViews(view)

        currentUserId = repository.getCurrentUser()?.uid
        if (currentUserId.isNullOrBlank()) {
            showMessage(getString(R.string.pay_user_not_signed_in))
            toggleProcessing(false)
            return
        }

        loadContacts(currentUserId!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contactsJob?.cancel()
        rootView = null
    }

    private fun initViews(root: View) {
        searchEditText = root.findViewById(R.id.et_search)
        amountEditText = root.findViewById(R.id.et_amount)
        favoritesRecyclerView = root.findViewById(R.id.rv_favorites)
        recentContactsRecyclerView = root.findViewById(R.id.rv_recent_contacts)
        recentContactsCard = root.findViewById(R.id.card_recent_contacts)
        favoritesEmptyView = root.findViewById(R.id.tv_favorites_empty)
        recentsEmptyView = root.findViewById(R.id.tv_recents_empty)
        sendProgressIndicator = root.findViewById(R.id.progress_send)
        recipientNameTextView = root.findViewById(R.id.tv_recipient_name)
        recipientSubtitleTextView = root.findViewById(R.id.tv_recipient_subtitle)
        sendPaymentButton = root.findViewById(R.id.btn_send_payment)
        nfcCardView = root.findViewById(R.id.card_nfc)
        contactOptionView = root.findViewById(R.id.option_contact)
        phoneOptionView = root.findViewById(R.id.option_phone)
        qrOptionView = root.findViewById(R.id.option_qr)
        vendorOptionView = root.findViewById(R.id.option_vendor)

        searchEditText.addTextChangedListener { text ->
            applyContactFilter(text?.toString().orEmpty())
        }

        contactOptionView.setOnClickListener { showContactSelectionSheet() }
        phoneOptionView.setOnClickListener { showPhoneEntrySheet() }
        qrOptionView.setOnClickListener { navigateToScanner() }
        nfcCardView.setOnClickListener { navigateToNfcPayment() }
        vendorOptionView.setOnClickListener { showMessage(getString(R.string.pay_vendor_not_available)) }
        sendPaymentButton.setOnClickListener { performTransfer() }
        root.findViewById<View>(R.id.btn_use_nfc).setOnClickListener { navigateToNfcPayment() }
        updateSelectedRecipientUI()

        favoritesAdapter = ContactAdapter(
            isFavorites = true,
            onContactClick = ::handleContactSelection
        )

        favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = favoritesAdapter
        }

        recentContactsAdapter = ContactAdapter(
            isFavorites = false,
            onContactClick = ::handleContactSelection,
            onSendClick = ::handleContactSelection
        )

        recentContactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentContactsAdapter
        }
    }

    private fun loadContacts(userId: String) {
        contactsJob?.cancel()
        contactsJob = viewLifecycleOwner.lifecycleScope.launch {
            // Load both contacts and all users
            launch {
                repository.getUserContacts(userId).collect { contactsResult ->
                    contactsResult.fold(
                        onSuccess = { contacts ->
                            android.util.Log.d("PayFragment", "Loaded ${contacts.size} contacts from Firebase")
                            
                            // Always load all users to show in contact list
                            launch {
                                repository.getAllUsers(excludeUserId = userId, limit = 100).collect { usersResult ->
                                    usersResult.fold(
                                        onSuccess = { users ->
                                            android.util.Log.d("PayFragment", "Loaded ${users.size} users from Firebase")
                                            
                                            // Convert users to contacts and combine with existing contacts
                                            val userContacts = users.map { user ->
                                                convertUserToContact(user, userId)
                                            }
                                            
                                            // Combine contacts and users, avoiding duplicates
                                            // Prefer existing contacts over user conversions (contacts may have transaction history)
                                            val contactMap = contacts.associateBy { it.contactUserId ?: it.id }
                                            val userMap = userContacts.associateBy { it.contactUserId ?: it.id }
                                            
                                            // Merge: prefer contacts, then add users that aren't in contacts
                                            val combinedContacts = contacts + userMap.values.filter { userContact ->
                                                val key = userContact.contactUserId ?: userContact.id
                                                !contactMap.containsKey(key)
                                            }
                                            
                                            android.util.Log.d("PayFragment", "Total combined contacts: ${combinedContacts.size}")
                                            
                                            // Use combined contacts (don't fall back to dummy data)
                                            updateContactLists(combinedContacts)
                                        },
                                        onFailure = { usersError ->
                                            android.util.Log.e("PayFragment", "Failed to load users", usersError)
                                            // Show contacts even if users fail (don't use dummy data)
                                            android.util.Log.d("PayFragment", "Showing ${contacts.size} contacts only")
                                            updateContactLists(contacts)
                                        }
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("PayFragment", "Failed to load contacts", error)
                            val reason = error.localizedMessage?.takeIf { it.isNotBlank() }
                            val message = reason?.let {
                                getString(R.string.pay_contacts_error, it)
                            } ?: getString(R.string.pay_contacts_error_generic)
                            showMessage(message)
                            
                            // Try to load users even if contacts fail
                            launch {
                                repository.getAllUsers(excludeUserId = userId, limit = 100).collect { usersResult ->
                                    usersResult.fold(
                                        onSuccess = { users ->
                                            android.util.Log.d("PayFragment", "Loaded ${users.size} users as fallback")
                                            val userContacts = users.map { user ->
                                                convertUserToContact(user, userId)
                                            }
                                            updateContactLists(userContacts)
                                        },
                                        onFailure = { usersError ->
                                            android.util.Log.e("PayFragment", "Failed to load users as fallback", usersError)
                                            // Show empty state instead of dummy data
                                            updateContactLists(emptyList())
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Convert a User model to a Contact model so it can be displayed in the contact list
     */
    private fun convertUserToContact(user: com.cashpal.app.models.User, currentUserId: String): Contact {
        return Contact(
            id = "user-${user.id}", // Prefix to avoid conflicts
            userId = currentUserId,
            contactUserId = user.id, // This is the key field - links to the user
            name = user.displayName.ifEmpty { user.email },
            email = user.email.takeIf { it.isNotEmpty() },
            phone = user.phoneNumber,
            avatarUrl = user.avatarUrl,
            isFrequent = false, // New users won't be frequent yet
            lastTransactionDate = null,
            totalTransactions = 0,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }

    private fun updateContactLists(contacts: List<Contact>) {
        cachedContacts = contacts
        hasLoadedContacts = true
        val query = searchEditText.text?.toString().orEmpty()
        applyContactFilter(query)
    }

    private fun applyContactFilter(query: String) {
        val normalized = query.trim().lowercase(Locale.getDefault())
        val isFiltering = normalized.isNotEmpty()
        if (cachedContacts.isEmpty()) {
            renderContactLists(emptyList(), isFiltering)
            return
        }
        if (!isFiltering) {
            renderContactLists(cachedContacts, false)
            return
        }
        val filtered = cachedContacts.filter { contact ->
            val fields = listOfNotNull(
                contact.name,
                contact.email,
                contact.phone
            )
            fields.any { it.contains(normalized, ignoreCase = true) }
        }
        renderContactLists(filtered, true)
    }

    private fun renderContactLists(contacts: List<Contact>, isFiltering: Boolean) {
        val favoriteContacts = contacts
            .filter { it.isFrequent }
            .sortedByDescending { it.totalTransactions }
            .take(8)
            .map { mapToPayContactItem(it) }

        val recentContacts = contacts
            .sortedByDescending { it.lastTransactionDate?.toDate()?.time ?: 0L }
            .take(10)
            .map { mapToPayContactItem(it) }

        favoritesAdapter.submitList(favoriteContacts)
        favoritesRecyclerView.isVisible = favoriteContacts.isNotEmpty()
        val showFavoritesEmpty = favoriteContacts.isEmpty() && (isFiltering || hasLoadedContacts)
        favoritesEmptyView.isVisible = showFavoritesEmpty
        if (showFavoritesEmpty) {
            favoritesEmptyView.text = getString(
                if (isFiltering) R.string.pay_no_favorites_search else R.string.pay_no_favorites
            )
        }

        recentContactsAdapter.submitList(recentContacts)
        val hasRecentContacts = recentContacts.isNotEmpty()
        recentContactsRecyclerView.isVisible = hasRecentContacts
        recentContactsCard.isVisible = hasRecentContacts
        val showRecentsEmpty = !hasRecentContacts && (isFiltering || hasLoadedContacts)
        recentsEmptyView.isVisible = showRecentsEmpty
        if (showRecentsEmpty) {
            recentsEmptyView.text = getString(
                if (isFiltering) R.string.pay_no_recent_contacts_search else R.string.pay_no_recent_contacts
            )
        }
        updateSelectedRecipientUI()
    }

    private fun mapToPayContactItem(contact: Contact): PayContactItem {
        val displayName = when {
            contact.name.isNotBlank() -> contact.name
            !contact.email.isNullOrBlank() -> contact.email!!
            !contact.phone.isNullOrBlank() -> contact.phone!!
            else -> getString(R.string.pay_unknown_contact)
        }

        val initials = buildInitials(displayName)
        val colorSeed = displayName + contact.id
        val colorHex = colorPalette.randomFromSeed(colorSeed)
        val subtitle = when {
            !contact.phone.isNullOrBlank() -> contact.phone
            !contact.email.isNullOrBlank() -> contact.email
            else -> null
        }

        return PayContactItem(
            id = contact.id,
            name = displayName,
            initials = initials,
            colorHex = colorHex,
            subtitle = subtitle,
            contactUserId = contact.contactUserId,
            phone = contact.phone
        )
    }

    private fun handleContactSelection(contact: PayContactItem) {
        if (isProcessingTransfer) return
        selectedRecipient = contact
        updateSelectedRecipientUI()
        if (contact.contactUserId.isNullOrBlank()) {
            showMessage(getString(R.string.pay_contact_not_available))
        }
    }

    private fun performTransfer() {
        if (isProcessingTransfer) return

        val recipient = selectedRecipient
        if (recipient == null) {
            showMessage(getString(R.string.pay_select_recipient_prompt))
            return
        }

        val amountValue = amountEditText.text?.toString()?.trim().orEmpty()
        val amount = amountValue.toDoubleOrNull()

        if (amount == null || amount <= 0.0) {
            amountEditText.error = getString(R.string.pay_amount_error)
            showMessage(getString(R.string.pay_amount_error))
            return
        }

        val senderId = currentUserId
        if (senderId.isNullOrBlank()) {
            showMessage(getString(R.string.pay_user_not_signed_in))
            return
        }

        val recipientId = recipient.contactUserId
        if (recipientId.isNullOrBlank()) {
            showMessage(getString(R.string.pay_contact_not_available))
            return
        }

        if (senderId == recipientId) {
            showMessage(getString(R.string.pay_self_transfer_error))
            return
        }

        amountEditText.error = null
        view?.clearFocus()
        toggleProcessing(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.processTransaction(
                    fromUserId = senderId,
                    toUserId = recipientId,
                    amount = amount,
                    description = getString(R.string.pay_transaction_description, recipient.name)
                ).collect { result ->
                    result.fold(
                        onSuccess = {
                            amountEditText.text?.clear()
                            playSuccessSound()
                            showMessage(getString(R.string.pay_transfer_success, recipient.name))
                            refreshContactsAfterTransfer()
                            // Refresh balance in MainActivity
                            (activity as? com.cashpal.app.MainActivity)?.refreshBalance()
                        },
                        onFailure = { error ->
                            playFailureSound()
                            val reason = error.localizedMessage?.takeIf { it.isNotBlank() }
                            val message = reason?.let {
                                getString(R.string.pay_transfer_failed, it)
                            } ?: getString(R.string.pay_transfer_failed_generic)
                            showMessage(message)
                        }
                    )
                }
            } catch (e: Exception) {
                playFailureSound()
                val reason = e.localizedMessage?.takeIf { it.isNotBlank() }
                val message = reason?.let {
                    getString(R.string.pay_transfer_failed, it)
                } ?: getString(R.string.pay_transfer_failed_generic)
                showMessage(message)
            } finally {
                toggleProcessing(false)
            }
        }
    }

    private fun refreshContactsAfterTransfer() {
        currentUserId?.let { loadContacts(it) }
    }

    private fun playSuccessSound() {
        try {
            val successSound = MediaPlayer.create(requireContext(), R.raw.success_sound)
            successSound?.start()
            successSound?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            // Silently fail if sound can't be played
        }
    }

    private fun playFailureSound() {
        try {
            val failSound = MediaPlayer.create(requireContext(), R.raw.failure_sound)
            failSound?.start()
            failSound?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            // Silently fail if sound can't be played
        }
    }

    private fun showContactSelectionSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_contact_list, null)
        val recyclerView = sheetView.findViewById<RecyclerView>(R.id.rv_contact_list)
        val searchInput = sheetView.findViewById<TextInputEditText>(R.id.et_contact_search)
        val emptyView = sheetView.findViewById<TextView>(R.id.tv_contact_list_empty)

        val adapter = ContactAdapter(
            isFavorites = false,
            onContactClick = { item ->
                dialog.dismiss()
                handleContactSelection(item)
            },
            onSendClick = { item ->
                dialog.dismiss()
                handleContactSelection(item)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        // Show loading state initially
        emptyView.isVisible = false
        recyclerView.isVisible = false

        fun render(items: List<PayContactItem>) {
            android.util.Log.d("PayFragment", "Rendering ${items.size} contacts in selection sheet")
            adapter.submitList(items)
            emptyView.isVisible = items.isEmpty()
            recyclerView.isVisible = items.isNotEmpty()
        }

        // Load contacts directly when showing the sheet
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = currentUserId ?: run {
                    showMessage(getString(R.string.pay_user_not_signed_in))
                    dialog.dismiss()
                    return@launch
                }

                // Load contacts (will return empty list if collection doesn't exist)
                val contactsResult = repository.getUserContacts(userId).first()
                val contacts = contactsResult.getOrNull() ?: emptyList()
                
                android.util.Log.d("PayFragment", "Loaded ${contacts.size} contacts from Firebase")

                // Always load all users to show in contact list
                val usersResult = repository.getAllUsers(excludeUserId = userId, limit = 100).first()
                val users = usersResult.getOrNull() ?: emptyList()
                
                android.util.Log.d("PayFragment", "Loaded ${users.size} users from Firebase")

                // Convert users to contacts
                val userContacts = users.map { user ->
                    convertUserToContact(user, userId)
                }

                // Combine contacts and users, avoiding duplicates
                val contactMap = contacts.associateBy { it.contactUserId ?: it.id }
                val combinedContacts = contacts + userContacts.filter { userContact ->
                    val key = userContact.contactUserId ?: userContact.id
                    !contactMap.containsKey(key)
                }

                android.util.Log.d("PayFragment", "Total combined contacts: ${combinedContacts.size}")

                // Update cached contacts
                cachedContacts = combinedContacts
                hasLoadedContacts = true

                // Render all contacts in the sheet
                val allItems = combinedContacts.map { mapToPayContactItem(it) }
                render(allItems)

                // Setup search
                searchInput?.addTextChangedListener { text ->
                    val query = text?.toString()?.trim().orEmpty()
                    if (query.isBlank()) {
                        render(allItems)
                    } else {
                        val filtered = allItems.filter { item ->
                            item.name.contains(query, ignoreCase = true) ||
                                (item.subtitle?.contains(query, ignoreCase = true) == true) ||
                                (item.phone?.contains(query, ignoreCase = true) == true)
                        }
                        render(filtered)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("PayFragment", "Error loading contacts for selection sheet", e)
                emptyView.isVisible = true
                recyclerView.isVisible = false
                showMessage("Failed to load contacts: ${e.message}")
            }
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }
    
    private fun showContactSelectionSheetInternal() {
        // This method is no longer needed, but keeping for backward compatibility
        showContactSelectionSheet()
    }

    private fun showPhoneEntrySheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_phone_entry, null)
        val inputLayout = sheetView.findViewById<TextInputLayout>(R.id.til_phone_number)
        val editText = sheetView.findViewById<TextInputEditText>(R.id.et_phone_number)
        val continueButton = sheetView.findViewById<MaterialButton>(R.id.btn_continue_phone)

        continueButton.setOnClickListener {
            val rawInput = editText?.text?.toString()?.trim().orEmpty()
            val digits = rawInput.filter { it.isDigit() }
            if (digits.isBlank()) {
                inputLayout?.error = getString(R.string.pay_phone_enter_number)
                return@setOnClickListener
            }

            inputLayout?.error = null
            
            // Normalize phone number (preserve + prefix if present)
            val normalizedPhone = normalizePhoneForSearch(rawInput)
            
            // Check cached contacts first
            val matchedContact = cachedContacts.firstOrNull { contact ->
                contact.phone?.let { normalizePhoneForSearch(it) == normalizedPhone } == true
            }

            dialog.dismiss()

            if (matchedContact != null) {
                handleContactSelection(mapToPayContactItem(matchedContact))
            } else {
                // Try to find user by phone number in Firebase
                viewLifecycleOwner.lifecycleScope.launch {
                    // Try multiple formats: with +, without +, and digits only
                    val searchVariants = mutableListOf<String>()
                    
                    if (normalizedPhone.startsWith("+")) {
                        searchVariants.add(normalizedPhone) // With +
                        searchVariants.add(normalizedPhone.substring(1)) // Without +
                    } else {
                        searchVariants.add("+$normalizedPhone") // Try with +
                        searchVariants.add(normalizedPhone) // Without +
                    }
                    
                    // Remove duplicates
                    val uniqueVariants = searchVariants.distinct()
                    
                    var foundUser: com.cashpal.app.models.User? = null
                    
                    // Try each variant sequentially
                    for (searchPhone in uniqueVariants) {
                        if (foundUser != null) break
                        
                        val result = repository.findUserByPhoneNumber(searchPhone).first()
                        result.fold(
                            onSuccess = { user ->
                                if (user != null) {
                                    foundUser = user
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.e("PayFragment", "Failed to find user by phone number: $searchPhone", error)
                            }
                        )
                    }
                    
                    // Update UI based on result
                    if (foundUser != null) {
                        val displayNumber = if (normalizedPhone.startsWith("+")) normalizedPhone else "+$normalizedPhone"
                        selectedRecipient = PayContactItem(
                            id = foundUser!!.id,
                            name = foundUser!!.displayName.ifEmpty { foundUser!!.email },
                            initials = buildInitials(foundUser!!.displayName.ifEmpty { foundUser!!.email }),
                            colorHex = colorPalette.randomFromSeed(foundUser!!.id),
                            subtitle = displayNumber,
                            contactUserId = foundUser!!.id,
                            phone = foundUser!!.phoneNumber ?: displayNumber
                        )
                        updateSelectedRecipientUI()
                        showMessage(getString(R.string.pay_phone_user_found))
                    } else {
                        // User not found in any variant
                        val displayNumber = if (normalizedPhone.startsWith("+")) normalizedPhone else "+$normalizedPhone"
                        selectedRecipient = PayContactItem(
                            id = "phone-${normalizedPhone.hashCode()}",
                            name = displayNumber,
                            initials = buildInitials(displayNumber),
                            colorHex = colorPalette.randomFromSeed(displayNumber),
                            subtitle = getString(R.string.pay_phone_trailing_note),
                            contactUserId = null,
                            phone = displayNumber
                        )
                        updateSelectedRecipientUI()
                        showMessage(getString(R.string.pay_phone_contact_not_found))
                    }
                }
            }
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun navigateToScanner() {
        (activity as? com.cashpal.app.MainActivity)?.openScanTab()
    }

    private fun navigateToNfcPayment() {
        (activity as? com.cashpal.app.MainActivity)?.openNfcPayment()
    }
    
    /**
     * Normalize phone number for search/comparison
     * Preserves + prefix if present, removes spaces, dashes, parentheses
     */
    private fun normalizePhoneForSearch(phone: String): String {
        return if (phone.startsWith("+")) {
            "+" + phone.substring(1).filter { it.isDigit() }
        } else {
            phone.filter { it.isDigit() }
        }
    }


    private fun updateSelectedRecipientUI() {
        if (!this::recipientNameTextView.isInitialized) return
        val recipient = selectedRecipient
        if (recipient == null) {
            recipientNameTextView.text = getString(R.string.pay_recipient_placeholder)
            recipientSubtitleTextView.isVisible = false
        } else {
            recipientNameTextView.text = recipient.name
            val subtitle = recipient.subtitle ?: recipient.phone
            if (!subtitle.isNullOrBlank()) {
                recipientSubtitleTextView.text = subtitle
                recipientSubtitleTextView.isVisible = true
            } else {
                recipientSubtitleTextView.isVisible = false
            }
        }
        updateSendButtonState()
    }

    private fun updateSendButtonState() {
        if (!this::sendPaymentButton.isInitialized) return
        val canSend = !isProcessingTransfer &&
            (selectedRecipient?.contactUserId?.isNotBlank() == true)
        sendPaymentButton.isEnabled = canSend
    }

    private fun toggleProcessing(active: Boolean) {
        isProcessingTransfer = active
        sendProgressIndicator.isVisible = active
        updateSendButtonState()
    }

    private fun showMessage(message: String) {
        val host = rootView ?: return
        if (message.isBlank()) return
        Snackbar.make(host, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun createDummyContacts(): List<Contact> {
        val ownerId = currentUserId ?: "demo-owner"
        val now = Timestamp.now()
        return listOf(
            Contact(
                id = "demo-alex",
                userId = ownerId,
                contactUserId = "demo-recipient-alex",
                name = "Alex Johnson",
                email = "alex.johnson@example.com",
                phone = "+1 (555) 101-1111",
                isFrequent = true,
                totalTransactions = 18,
                lastTransactionDate = now
            ),
            Contact(
                id = "demo-sarah",
                userId = ownerId,
                contactUserId = "demo-recipient-sarah",
                name = "Sarah Williams",
                email = "sarah.williams@example.com",
                phone = "+1 (555) 202-2222",
                isFrequent = true,
                totalTransactions = 15,
                lastTransactionDate = now
            ),
            Contact(
                id = "demo-mike",
                userId = ownerId,
                contactUserId = "demo-recipient-mike",
                name = "Mike Johnson",
                email = "mike.johnson@example.com",
                phone = "+1 (555) 303-3333",
                isFrequent = false,
                totalTransactions = 6,
                lastTransactionDate = now
            ),
            Contact(
                id = "demo-emma",
                userId = ownerId,
                contactUserId = "demo-recipient-emma",
                name = "Emma Davis",
                email = "emma.davis@example.com",
                phone = "+1 (555) 404-4444",
                isFrequent = false,
                totalTransactions = 3,
                lastTransactionDate = now
            ),
            Contact(
                id = "demo-john",
                userId = ownerId,
                contactUserId = "demo-recipient-john",
                name = "John Smith",
                email = "john.smith@example.com",
                phone = "+1 (555) 505-5555",
                isFrequent = true,
                totalTransactions = 9,
                lastTransactionDate = now
            )
        )
    }

    private fun buildInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.isEmpty()) {
            return name.take(2).ifBlank { "CP" }.uppercase(Locale.getDefault())
        }
        return parts.take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
    }

    private fun List<String>.randomFromSeed(seed: String): String {
        if (isEmpty()) return "#6C5CE7"
        val index = (seed.hashCode() and Int.MAX_VALUE) % size
        return this[index]
    }
}
