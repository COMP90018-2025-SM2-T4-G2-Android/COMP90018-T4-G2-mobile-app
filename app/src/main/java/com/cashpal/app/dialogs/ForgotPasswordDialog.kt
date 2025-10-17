package com.cashpal.app.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.cashpal.app.R
import com.cashpal.app.databinding.DialogForgotPasswordBinding
import com.cashpal.app.di.ServiceLocator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordDialog : DialogFragment() {
    
    private lateinit var binding: DialogForgotPasswordBinding
    private lateinit var repository: com.cashpal.app.repository.CashPalRepository
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogForgotPasswordBinding.inflate(layoutInflater)
        repository = ServiceLocator.getRepository()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Enter your email address and we'll send you a link to reset your password.")
            .setView(binding.root)
            .setPositiveButton("Send") { _, _ ->
                handlePasswordReset()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
    
    private fun handlePasswordReset() {
        val email = binding.etEmail.text.toString().trim()
        
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your email address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.resetPassword(email).collect { result ->
                    withContext(Dispatchers.Main) {
                        result.fold(
                            onSuccess = { _ ->
                                Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                                dismiss()
                            },
                            onFailure = { error ->
                                Toast.makeText(requireContext(), "Failed to send reset email: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
