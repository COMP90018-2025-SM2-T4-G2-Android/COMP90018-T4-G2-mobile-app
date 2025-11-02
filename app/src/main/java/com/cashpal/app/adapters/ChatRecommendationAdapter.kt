package com.cashpal.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.google.android.material.chip.Chip

class ChatRecommendationAdapter(
    private val recommendations: List<String>,
    private val onRecommendationClick: (String) -> Unit
) : RecyclerView.Adapter<ChatRecommendationAdapter.RecommendationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_recommendation, parent, false)
        return RecommendationViewHolder(view as Chip)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recommendations[position])
    }

    override fun getItemCount(): Int = recommendations.size

    inner class RecommendationViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(recommendation: String) {
            chip.text = recommendation
            chip.setOnClickListener {
                onRecommendationClick(recommendation)
            }
        }
    }
}

