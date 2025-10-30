package com.cashpal.app.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.cashpal.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FraudAlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val message = args.getString(ARG_MESSAGE).orEmpty()
        val title = args.getString(ARG_TITLE) ?: getString(R.string.fraud_alert_title)
        val location = args.getString(ARG_LOCATION)
        val device = args.getString(ARG_DEVICE)
        val timestamp = args.getString(ARG_TIMESTAMP)

        val detailBuilder = StringBuilder().apply {
            append(message)
            append("\n\n")
            location?.let {
                append(getString(R.string.fraud_alert_location, it))
                append("\n")
            }
            device?.let {
                append(getString(R.string.fraud_alert_device, it))
                append("\n")
            }
            timestamp?.let {
                append(getString(R.string.fraud_alert_timestamp, it))
                append("\n")
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(detailBuilder.toString().trim())
            .setPositiveButton(R.string.fraud_alert_ok, null)
            .create()
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_DEVICE = "arg_device"
        private const val ARG_TIMESTAMP = "arg_timestamp"

        fun newInstance(
            title: String?,
            message: String,
            location: String?,
            device: String?,
            timestamp: String?
        ): FraudAlertDialogFragment {
            val fragment = FraudAlertDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
                putString(ARG_LOCATION, location)
                putString(ARG_DEVICE, device)
                putString(ARG_TIMESTAMP, timestamp)
            }
            return fragment
        }
    }
}
