package com.cashpal.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.cashpal.app.R

class PinVerifyDialogFragment(
    private val onPinVerified: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_pin_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etPin = view.findViewById<EditText>(R.id.et_pin)
        val btnVerify = view.findViewById<Button>(R.id.btn_verify)

        btnVerify.setOnClickListener {
            val pin = etPin.text.toString()
            if (BiometricPasswordStore.verifyPin(requireContext(), pin)) {
                Toast.makeText(requireContext(), "PIN verified", Toast.LENGTH_SHORT).show()
                dismiss()
                onPinVerified()
            } else {
                Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
