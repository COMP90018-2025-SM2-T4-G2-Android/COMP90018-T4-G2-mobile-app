package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cashpal.app.R
import org.json.JSONObject

class ScanResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scan_result, container, false)

        // ----- get data from arguments -----
        val qrData = arguments?.getString("qrData")

        try {
            val json = JSONObject(qrData ?: "{}")

            val name   = json.optString("name", "Unknown")
            val type   = json.optString("type", "personal")   // "personal" | "store" | "payment"
            val phone  = json.optString("phone", "")
            val amount = if (!json.isNull("amount")) json.optDouble("amount", 0.0) else 0.0

            // ----- bind views -----
            val tvName          = view.findViewById<TextView>(R.id.tv_scanned_name)
            val tvIdentifier    = view.findViewById<TextView>(R.id.tv_scanned_identifier)
            val tvAmount        = view.findViewById<TextView>(R.id.tv_scanned_amount)
            val llAmountBadge   = view.findViewById<LinearLayout>(R.id.ll_amount_badge)

            val flAvatar        = view.findViewById<FrameLayout>(R.id.fl_person_avatar)
            val ivStore         = view.findViewById<ImageView>(R.id.iv_store_icon)
            val ivQr            = view.findViewById<ImageView>(R.id.iv_qr_icon)

            val llPersonActions = view.findViewById<LinearLayout>(R.id.ll_person_actions)
            val llPayActions    = view.findViewById<LinearLayout>(R.id.ll_payment_actions)

            // ----- populate -----
            tvName.text = name
            tvIdentifier.text = if (phone.isNotEmpty()) phone else "N/A"

            if (amount > 0.0) {
                llAmountBadge.visibility = View.VISIBLE
                tvAmount.text = "$" + String.format("%.2f", amount)
            } else {
                llAmountBadge.visibility = View.GONE
            }

            // ----- switch icon/action set by type -----
            when (type.lowercase()) {
                "store" -> {
                    ivStore.visibility = View.VISIBLE
                    llPayActions.visibility = View.VISIBLE
                }
                "payment" -> {
                    ivQr.visibility = View.VISIBLE
                    llPayActions.visibility = View.VISIBLE
                }
                else -> { // personal
                    flAvatar.visibility = View.VISIBLE
                    llPersonActions.visibility = View.VISIBLE
                }
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid QR data", Toast.LENGTH_SHORT).show()
        }

        // ----- header back -----
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ----- quick actions -----
        view.findViewById<LinearLayout>(R.id.btn_scan_again).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ScanFragment())
                .addToBackStack("scan_again")
                .commit()
        }

        view.findViewById<LinearLayout>(R.id.btn_view_history).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment())
                .addToBackStack("history")
                .commit()
        }

        // "Scan Another Code" button in the card
        view.findViewById<View>(R.id.btn_scan_another)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ScanFragment())
                .addToBackStack("scan_again")
                .commit()
        }

        return view
    }
}
