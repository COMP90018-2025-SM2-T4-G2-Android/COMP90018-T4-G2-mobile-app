package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.cashpal.app.R

class MoreFragment : Fragment() {
    
    private lateinit var biometricSwitch: SwitchCompat
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var darkModeSwitch: SwitchCompat
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
            showToast("Profile")
        }
        
        requireView().findViewById<View>(R.id.item_verification)?.setOnClickListener {
            showToast("Verification")
        }
        
        requireView().findViewById<View>(R.id.item_payment_methods)?.setOnClickListener {
            showToast("Payment Methods")
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
            showToast("Sign Out")
        }
    }
    
    private fun setupSwitches() {
        // Set default states
        biometricSwitch.isChecked = true
        notificationsSwitch.isChecked = true
        darkModeSwitch.isChecked = false
        
        // Handle switch state changes
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            showToast("Biometric Authentication: ${if (isChecked) "Enabled" else "Disabled"}")
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
}
