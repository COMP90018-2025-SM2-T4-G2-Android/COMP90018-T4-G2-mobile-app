package com.cashpal.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.R
import com.cashpal.app.auth.BiometricAuthManager
import com.cashpal.app.auth.SignInActivity
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.BiometricPreferences
import kotlinx.coroutines.launch

class MoreFragment : Fragment() {
    
    private lateinit var biometricSwitch: SwitchCompat
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var biometricPreferences: BiometricPreferences
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize biometric components
        biometricManager = BiometricAuthManager(requireActivity(), requireContext())
        biometricPreferences = BiometricPreferences(requireContext())
        
        initViews()
        setupClickListeners()
        setupSwitches()
    }
    
    private fun initViews() {
        biometricSwitch = requireView().findViewById(R.id.switch_biometric)
        notificationsSwitch = requireView().findViewById(R.id.switch_notifications)
        darkModeSwitch = requireView().findViewById(R.id.switch_dark_mode)
    }
    
    private fun setupClickListeners() {
        // AI Assistant
        requireView().findViewById<View>(R.id.card_ai_assistant)?.setOnClickListener {
            showToast("AI Assistant - Coming Soon!")
        }
        
        // Account Management
        requireView().findViewById<View>(R.id.item_profile)?.setOnClickListener {
            showProfileDialog()
        }
        
        requireView().findViewById<View>(R.id.item_verification)?.setOnClickListener {
            showToast("Verification")
        }
        
        requireView().findViewById<View>(R.id.item_payment_methods)?.setOnClickListener {
            showPaymentMethodsDialog()
        }
        
        requireView().findViewById<View>(R.id.item_security)?.setOnClickListener {
            showToast("Security")
        }
        
        // Features
        requireView().findViewById<View>(R.id.item_nfc_settings)?.setOnClickListener {
            showToast("NFC Settings")
        }
        
        requireView().findViewById<View>(R.id.item_qr_settings)?.setOnClickListener {
            showToast("QR Settings")
        }
        
        // Preferences
        requireView().findViewById<View>(R.id.item_language)?.setOnClickListener {
            showToast("Language Settings")
        }
        
        // Support
        requireView().findViewById<View>(R.id.item_help_center)?.setOnClickListener {
            showToast("Help Center")
        }
        
        requireView().findViewById<View>(R.id.item_contact_support)?.setOnClickListener {
            showToast("Contact Support")
        }
        
        // App Info - App Version is non-clickable
        
        // Sign Out
        requireView().findViewById<View>(R.id.btn_sign_out)?.setOnClickListener {
            signOut()
        }
    }
    
    private fun setupSwitches() {
        // Check biometric availability
        val isBiometricAvailable = biometricManager.isBiometricAvailable()
        biometricSwitch.isEnabled = isBiometricAvailable
        
        if (!isBiometricAvailable) {
            biometricSwitch.isChecked = false
            biometricSwitch.alpha = 0.5f
        } else {
            // Set current state from preferences
            biometricSwitch.isChecked = biometricPreferences.isBiometricEnabled()
        }
        
        // Set other default states
        notificationsSwitch.isChecked = true
        darkModeSwitch.isChecked = false
        
        // Handle biometric switch state changes
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBiometricAvailable) {
                biometricPreferences.setBiometricEnabled(isChecked)
                showToast("Biometric Authentication: ${if (isChecked) "Enabled" else "Disabled"}")
                
                if (!isChecked) {
                    // Clear biometric data when disabled
                    biometricPreferences.clearBiometricData()
                }
            } else {
                showToast("Biometric authentication not available on this device")
                biometricSwitch.isChecked = false
            }
        }
        
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            showToast("Notifications: ${if (isChecked) "Enabled" else "Disabled"}")
        }
        
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            showToast("Dark Mode: ${if (isChecked) "Enabled" else "Disabled"}")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showProfileDialog() {
        val profileDialog = ProfileDialogFragment.newInstance()
        profileDialog.show(childFragmentManager, ProfileDialogFragment.TAG)
    }
    
    private fun showPaymentMethodsDialog() {
        val paymentMethodsDialog = PaymentMethodsDialogFragment.newInstance()
        paymentMethodsDialog.show(childFragmentManager, PaymentMethodsDialogFragment.TAG)
    }
    
    private fun signOut() {
        lifecycleScope.launch {
            try {
                val repository = ServiceLocator.getRepository()
                repository.signOut()
                
                // Navigate to AuthActivity and clear the back stack
                        val intent = Intent(requireContext(), SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                
                // Finish the current activity (MainActivity)
                requireActivity().finish()
                
            } catch (e: Exception) {
                showToast("Failed to sign out: ${e.message}")
            }
        }
    }
}
