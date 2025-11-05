package com.cashpal.app.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatDelegate
import com.cashpal.app.R
import com.cashpal.app.auth.BiometricAuthManager
import com.cashpal.app.auth.BiometricPasswordStore
import com.cashpal.app.auth.SignInActivity
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.BiometricPreferences
import com.cashpal.app.utils.AppPreferences
import androidx.core.os.bundleOf
import kotlinx.coroutines.launch

class MoreFragment : Fragment() {
    
    companion object {
        private const val ARG_OPEN_PROFILE = "open_profile"

        fun newInstance(openProfile: Boolean = false): MoreFragment {
            return MoreFragment().apply {
                arguments = bundleOf(ARG_OPEN_PROFILE to openProfile)
            }
        }
    }
    
    private lateinit var biometricSwitch: SwitchCompat
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var biometricPreferences: BiometricPreferences
    private lateinit var notificationsChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var darkModeChangeListener: CompoundButton.OnCheckedChangeListener
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPreferences.KEY_NOTIFICATIONS -> syncNotificationsSwitch()
                AppPreferences.KEY_DARK_MODE -> syncDarkModeSwitch()
            }
        }
    private var openProfileOnResume = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openProfileOnResume =
            savedInstanceState?.getBoolean(ARG_OPEN_PROFILE)
                ?: arguments?.getBoolean(ARG_OPEN_PROFILE, false)
                ?: false
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize biometric components
        biometricManager = BiometricAuthManager(requireContext(), requireActivity())
        biometricPreferences = BiometricPreferences(requireContext())
        
        initViews()
        setupClickListeners()
        setupSwitches()
        AppPreferences.registerListener(preferenceChangeListener)
    }

    override fun onResume() {
        super.onResume()
        if (openProfileOnResume) {
            view?.post {
                openProfileOnResume = false
                showProfileDialog()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_OPEN_PROFILE, openProfileOnResume)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppPreferences.unregisterListener(preferenceChangeListener)
        biometricSwitch.setOnCheckedChangeListener(null)
        notificationsSwitch.setOnCheckedChangeListener(null)
        darkModeSwitch.setOnCheckedChangeListener(null)
    }
    
    private fun initViews() {
        biometricSwitch = requireView().findViewById(R.id.switch_biometric)
        notificationsSwitch = requireView().findViewById(R.id.switch_notifications)
        darkModeSwitch = requireView().findViewById(R.id.switch_dark_mode)
    }
    
    private fun setupClickListeners() {
        // AI Assistant
        requireView().findViewById<View>(R.id.card_ai_assistant)?.setOnClickListener {
            val userId = ServiceLocator.getRepository().getCurrentUser()?.uid
            com.cashpal.app.AIChatActivity.start(requireContext(), userId)
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
        
        // Demo Mode Toggle
        requireView().findViewById<View>(R.id.card_demo_mode)?.setOnClickListener {
            toggleDemoMode()
        }
        
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
        notificationsSwitch.isChecked = AppPreferences.areNotificationsEnabled()
        darkModeSwitch.isChecked = AppPreferences.isDarkModeEnabled()

        notificationsChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(isChecked)
            showToast("Notifications: ${if (isChecked) "Enabled" else "Disabled"}")
        }

        darkModeChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkModeEnabled(isChecked)
            applyDarkMode(isChecked)
            showToast("Dark Mode: ${if (isChecked) "Enabled" else "Disabled"}")
        }
        
        // Handle biometric switch state changes
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBiometricAvailable) {
                biometricPreferences.setBiometricEnabled(isChecked)
                showToast("Biometric Authentication: ${if (isChecked) "Enabled" else "Disabled"}")
                
                if (!isChecked) {
                    // Clear biometric data when disabled
                    biometricPreferences.clearCredentials()
                    // Also disable BiometricPasswordStore to prevent biometric login
                    BiometricPasswordStore.disable(requireContext())
                }
            } else {
                showToast("Biometric authentication not available on this device")
                biometricSwitch.isChecked = false
            }
        }
        
        notificationsSwitch.setOnCheckedChangeListener(notificationsChangeListener)
        darkModeSwitch.setOnCheckedChangeListener(darkModeChangeListener)
    }

    private fun syncNotificationsSwitch() {
        val isEnabled = AppPreferences.areNotificationsEnabled()
        notificationsSwitch.setOnCheckedChangeListener(null)
        notificationsSwitch.isChecked = isEnabled
        notificationsSwitch.setOnCheckedChangeListener(notificationsChangeListener)
    }

    private fun syncDarkModeSwitch() {
        val isEnabled = AppPreferences.isDarkModeEnabled()
        darkModeSwitch.setOnCheckedChangeListener(null)
        darkModeSwitch.isChecked = isEnabled
        darkModeSwitch.setOnCheckedChangeListener(darkModeChangeListener)
    }

    private fun applyDarkMode(enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    fun openProfileFromHeader() {
        showProfileDialog()
    }

    private fun showProfileDialog() {
        val profileDialog = ProfileDialogFragment.newInstance()
        profileDialog.show(childFragmentManager, ProfileDialogFragment.TAG)
    }
    
    private fun showPaymentMethodsDialog() {
        val paymentMethodsDialog = PaymentMethodsDialogFragment.newInstance()
        paymentMethodsDialog.show(childFragmentManager, PaymentMethodsDialogFragment.TAG)
    }
    
    private fun toggleDemoMode() {
        val currentDemoMode = biometricPreferences.isDemoMode()
        biometricPreferences.setDemoMode(!currentDemoMode)
        
        val message = if (!currentDemoMode) {
            "Demo mode enabled! Sample data will be shown."
        } else {
            "Demo mode disabled! Real data will be shown."
        }
        
        showToast(message)
        
        // Restart the app to apply changes
        val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("demo_mode", !currentDemoMode)
            startActivity(intent)
            requireActivity().finish()
        } else {
            showToast("Failed to restart app")
        }
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
