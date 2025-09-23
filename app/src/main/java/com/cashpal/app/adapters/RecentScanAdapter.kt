package com.cashpal.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.cashpal.app.models.RecentScan

class RecentScanAdapter(
    private val scans: List<RecentScan>,
    private val onScanAgainClick: (RecentScan) -> Unit
) : RecyclerView.Adapter<RecentScanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_scan_icon)
        val vendor: TextView = view.findViewById(R.id.tv_vendor)
        val location: TextView = view.findViewById(R.id.tv_location)
        val time: TextView = view.findViewById(R.id.tv_time)
        val amount: TextView = view.findViewById(R.id.tv_amount)
        val scanAgainButton: Button = view.findViewById(R.id.btn_scan_again)
        val container: View = view.findViewById(R.id.container_scan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_scan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scan = scans[position]

        holder.vendor.text = scan.vendor
        holder.location.text = scan.location
        holder.time.text = scan.time
        holder.amount.text = scan.amount

        holder.scanAgainButton.setOnClickListener {
            onScanAgainClick(scan)
        }

        holder.container.setOnClickListener {
            // Handle item click
        }
    }

    override fun getItemCount() = scans.size
}