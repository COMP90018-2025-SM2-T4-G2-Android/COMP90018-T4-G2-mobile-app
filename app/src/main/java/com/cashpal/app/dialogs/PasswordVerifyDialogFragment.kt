package com.cashpal.app.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.auth.Reauth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PasswordVerifyDialogFragment : DialogFragment() {

    private var onVerified: ((Boolean) -> Unit)? = null

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun new(prefillEmail: String?, onVerified: (Boolean) -> Unit): PasswordVerifyDialogFragment {
            return PasswordVerifyDialogFragment().apply {
                arguments = bundleOf(ARG_EMAIL to (prefillEmail ?: ""))
                this.onVerified = onVerified
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()

        val emailInput = EditText(ctx).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(arguments?.getString(ARG_EMAIL).orEmpty())
            maxLines = 1
        }
        val pwInput = EditText(ctx).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            maxLines = 1
        }
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(emailInput)
            addView(pwInput)
        }

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Confirm your password")
            .setMessage("For security, please re-enter your password.")
            .setView(container)
            .setNegativeButton("Cancel") { d, _ ->
                safeCallback(false); d.dismiss()
            }
            .setPositiveButton("Verify", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = emailInput.text.toString().trim()
                val pw = pwInput.text.toString()

                if (email.isEmpty() || pw.isEmpty()) {
                    Toast.makeText(ctx, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Reauth with crash guards
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        Reauth.reauthenticate(email, pw).collect { result: Result<Unit> ->
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(ctx, "Verified", Toast.LENGTH_SHORT).show()
                                    safeCallback(true)
                                    if (isAdded) dismissAllowingStateLoss()
                                },
                                onFailure = { err ->
                                    Toast.makeText(ctx, "Incorrect password: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    } catch (ce: CancellationException) {
                        // dialog closed; ignore
                    } catch (t: Throwable) {
                        // Last-ditch guard: never crash; show a toast so we can see the root cause
                        Toast.makeText(ctx, "Verification error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        return dialog
    }

    private fun safeCallback(ok: Boolean) {
        try { onVerified?.invoke(ok) } catch (_: Throwable) { /* swallow */ }
    }
}
