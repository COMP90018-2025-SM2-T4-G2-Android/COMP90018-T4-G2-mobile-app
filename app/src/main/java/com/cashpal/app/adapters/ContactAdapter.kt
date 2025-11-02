package com.cashpal.app.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.google.android.material.button.MaterialButton
import java.util.Locale

data class PayContactItem(
    val id: String = "",
    val name: String,
    val initials: String,
    val colorHex: String? = null,
    val subtitle: String? = null,
    val actionLabel: String? = null,
    val contactUserId: String? = null,
    val phone: String? = null
)

class ContactAdapter(
    private val isFavorites: Boolean,
    private val onContactClick: (PayContactItem) -> Unit,
    private val onSendClick: ((PayContactItem) -> Unit)? = null
) : RecyclerView.Adapter<ContactAdapter.BaseViewHolder>() {

    private val contacts = mutableListOf<PayContactItem>()

    fun submitList(items: List<PayContactItem>) {
        contacts.clear()
        contacts.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isFavorites) {
            val view = inflater.inflate(R.layout.item_favorite_contact, parent, false)
            FavoriteViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_recent_contact, parent, false)
            RecentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarContainer: View? = itemView.findViewById(R.id.avatar_container)
        protected val avatarTextView: TextView = itemView.findViewById(R.id.tv_avatar)
        protected val nameTextView: TextView = itemView.findViewById(R.id.tv_name)

        open fun bind(contact: PayContactItem) {
            val context = itemView.context
            val baseColor = parseColor(context, contact.colorHex)

            nameTextView.text = contact.name
            avatarTextView.text = contact.initials.uppercase(Locale.getDefault())
            avatarTextView.setTextColor(baseColor)

            avatarContainer?.background = createAvatarBackground(baseColor)

            itemView.setOnClickListener { onContactClick(contact) }
        }
    }

    inner class FavoriteViewHolder(itemView: View) : BaseViewHolder(itemView)

    inner class RecentViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private val subtitleTextView: TextView? = itemView.findViewById(R.id.tv_subtitle)
        private val sendButton: MaterialButton? = itemView.findViewById(R.id.btn_send)

        override fun bind(contact: PayContactItem) {
            super.bind(contact)

            subtitleTextView?.apply {
                if (contact.subtitle.isNullOrBlank()) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    text = contact.subtitle
                }
            }

            sendButton?.apply {
                text = contact.actionLabel ?: context.getString(R.string.pay_send_cta)
                val clickHandler = onSendClick ?: onContactClick
                setOnClickListener { clickHandler(contact) }
            }
        }
    }

    private fun createAvatarBackground(baseColor: Int): GradientDrawable {
        val softened = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.75f)
        val strokeColor = ColorUtils.setAlphaComponent(baseColor, 180)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(softened)
            setStroke(2, strokeColor)
        }
    }

    private fun parseColor(context: android.content.Context, colorHex: String?): Int {
        if (colorHex.isNullOrBlank()) {
            return ContextCompat.getColor(context, R.color.primary)
        }
        return try {
            Color.parseColor(colorHex)
        } catch (_: IllegalArgumentException) {
            ContextCompat.getColor(context, R.color.primary)
        }
    }
}
