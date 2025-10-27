package com.cashpal.app.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.data.Contact

class ContactAdapter(
    private val isFavoriteAdapter: Boolean,
    private val onContactClick: (Contact) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val colorPalette = listOf(
        "#6C5CE7", "#00B894", "#E17055", "#FDCB6E", "#74B9FF"
    )

    private var contacts: List<Contact> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isFavoriteAdapter) {
            val view = inflater.inflate(R.layout.item_favorite_contact, parent, false)
            FavoriteViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_recent_contact, parent, false)
            RecentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val contact = contacts[position]
        val color = Color.parseColor(colorPalette[position % colorPalette.size])
        if (holder is FavoriteViewHolder) {
            holder.bind(contact, color, onContactClick)
        } else if (holder is RecentViewHolder) {
            holder.bind(contact, color, onContactClick)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    private fun getInitials(name: String): String {
        if (name.isBlank()) return "?"
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            else -> parts[0].take(2).uppercase()
        }
    }

    private inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val initialsText: TextView = itemView.findViewById(R.id.tvContactInitials)
        private val nameText: TextView = itemView.findViewById(R.id.tvContactName)

        fun bind(contact: Contact, color: Int, onContactClick: (Contact) -> Unit) {
            initialsText.text = getInitials(contact.name)
            ViewCompat.setBackgroundTintList(initialsText, ColorStateList.valueOf(color))

            nameText.text = contact.name
            itemView.setOnClickListener { onContactClick(contact) }
        }
    }

    private inner class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val initialsText: TextView = itemView.findViewById(R.id.tvRecentInitials)
        private val nameText: TextView = itemView.findViewById(R.id.tvRecentName)
        private val detailText: TextView = itemView.findViewById(R.id.tvRecentDetail)
        private val lastTransactionText: TextView = itemView.findViewById(R.id.tvRecentLastTransaction)

        fun bind(contact: Contact, color: Int, onContactClick: (Contact) -> Unit) {
            initialsText.text = getInitials(contact.name)
            ViewCompat.setBackgroundTintList(initialsText, ColorStateList.valueOf(color))

            nameText.text = contact.name
            detailText.text = contact.email ?: contact.phone ?: itemView.context.getString(R.string.pay_contact_no_detail)

            if (!contact.lastTransactionDate.isNullOrEmpty()) {
                lastTransactionText.visibility = View.VISIBLE
                lastTransactionText.text = itemView.context.getString(
                    R.string.pay_last_transaction_template,
                    contact.lastTransactionDate
                )
            } else {
                lastTransactionText.visibility = View.GONE
            }

            itemView.setOnClickListener { onContactClick(contact) }
        }
    }
}
