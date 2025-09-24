package com.cashpal.app.data

import android.content.Context

class ContactRepository(private val context: Context) {

    // Mock contact data - in our production app, this would come from contacts API or server
    fun getContacts(): List<Contact> {
        return listOf(
            Contact(
                id = "1",
                name = "Sarah Wilson",
                email = "sarah.wilson@email.com",
                phone = "+1 (555) 123-4567",
                avatar = "ic_person",
                lastTransactionDate = "2 days ago",
                isFrequent = true
            ),
            Contact(
                id = "2",
                name = "John Doe",
                email = "john.doe@email.com",
                phone = "+1 (555) 987-6543",
                avatar = "ic_person",
                lastTransactionDate = "1 week ago",
                isFrequent = true
            ),
            Contact(
                id = "3",
                name = "Emma Thompson",
                email = "emma.t@email.com",
                phone = "+1 (555) 456-7890",
                avatar = "ic_person",
                lastTransactionDate = "3 days ago",
                isFrequent = false
            ),
            Contact(
                id = "4",
                name = "Michael Chen",
                email = "m.chen@email.com",
                phone = "+1 (555) 234-5678",
                avatar = "ic_person",
                lastTransactionDate = "1 month ago",
                isFrequent = false
            ),
            Contact(
                id = "5",
                name = "Lisa Rodriguez",
                email = "lisa.r@email.com",
                phone = "+1 (555) 345-6789",
                avatar = "ic_person",
                lastTransactionDate = "5 days ago",
                isFrequent = true
            ),
            Contact(
                id = "6",
                name = "David Kim",
                email = "david.kim@email.com",
                phone = "+1 (555) 567-8901",
                avatar = "ic_person",
                lastTransactionDate = "2 weeks ago",
                isFrequent = false
            )
        )
    }

    fun getFrequentContacts(): List<Contact> {
        return getContacts().filter { it.isFrequent }
    }

    fun searchContacts(query: String): List<Contact> {
        if (query.isBlank()) return getContacts()

        return getContacts().filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.email?.contains(query, ignoreCase = true) == true ||
                    contact.phone?.contains(query, ignoreCase = true) == true
        }
    }

    fun addContact(contact: Contact): Boolean {
        // In a real app, you'd save this to database/server
        // For now, just return success
        return true
    }
}