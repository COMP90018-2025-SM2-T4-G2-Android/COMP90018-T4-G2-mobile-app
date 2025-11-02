package com.cashpal.app.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.R
import com.cashpal.app.di.ServiceLocator
import kotlinx.coroutines.launch

class ProfileDialogFragment : DialogFragment() {
    
    private lateinit var tvInitials: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvAccountType: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnChangePhoto: View
    private lateinit var btnEditProfile: Button
    private lateinit var btnClose: ImageView
    
    private val repository = ServiceLocator.getRepository()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        loadUserData()
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    
    private fun initViews(view: View) {
        tvInitials = view.findViewById(R.id.tv_initials)
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvAccountType = view.findViewById(R.id.tv_account_type)
        tvEmail = view.findViewById(R.id.tv_email)
        tvPhone = view.findViewById(R.id.tv_phone)
        tvAddress = view.findViewById(R.id.tv_address)
        btnChangePhoto = view.findViewById(R.id.btn_change_photo)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnClose = view.findViewById(R.id.btn_close)
    }
    
    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }
        
        btnChangePhoto.setOnClickListener {
            showToast("Change photo - Coming soon!")
            // TODO: Implement photo picker
            // You can use ActivityResultContracts.PickVisualMedia() here
        }
        
        btnEditProfile.setOnClickListener {
            openEditProfileDialog()
        }
    }
    
    private fun loadUserData() {
        val currentUser = repository.getCurrentUser()
        if (currentUser == null) {
            showToast("User not found")
            dismiss()
            return
        }
        
        lifecycleScope.launch {
            try {
                repository.getUserProfile(currentUser.uid).collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                populateUserData(user)
                            } else {
                                showToast("User profile not found")
                                dismiss()
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("ProfileDialog", "Failed to load user profile", error)
                            showToast("Failed to load profile: ${error.message}")
                            // Show placeholder data as fallback
                            showPlaceholderData()
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileDialog", "Error loading user data", e)
                showToast("Error loading profile data")
                showPlaceholderData()
            }
        }
    }
    
    private fun populateUserData(user: com.cashpal.app.models.User) {
        tvUserName.text = user.displayName.ifBlank { "User" }
        tvInitials.text = getInitials(user.displayName.ifBlank { user.email })
        tvAccountType.text = if (user.isVerified) "Verified Account" else "Account Holder"
        tvEmail.text = user.email.ifBlank { "No email" }
        tvPhone.text = user.phoneNumber ?: "No phone number"
        tvAddress.text = "Address not available" // Address field not in User model
        
        // Load profile photo if available
        if (!user.avatarUrl.isNullOrBlank()) {
            // TODO: Load image using Glide or similar library
            // For now, just show initials
        }
    }
    
    private fun showPlaceholderData() {
        tvUserName.text = "User"
        tvInitials.text = "U"
        tvAccountType.text = "Account Holder"
        tvEmail.text = "No email"
        tvPhone.text = "No phone number"
        tvAddress.text = "Address not available"
    }
    
    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts.last().first()}".uppercase()
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun openEditProfileDialog() {
        val currentUser = repository.getCurrentUser()
        if (currentUser == null) {
            showToast("User not found")
            return
        }
        
        lifecycleScope.launch {
            try {
                repository.getUserProfile(currentUser.uid).collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                val editProfileDialog = EditProfileDialogFragment.newInstance(
                                    name = user.displayName,
                                    email = user.email,
                                    phone = user.phoneNumber ?: "",
                                    address = "" // Address not in User model
                                )
                                
                                // Dismiss current dialog first
                                dismiss()
                                
                                // Show edit profile dialog
                                editProfileDialog.show(parentFragmentManager, EditProfileDialogFragment.TAG)
                            }
                        },
                        onFailure = { error ->
                            showToast("Failed to load profile: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileDialog", "Error loading user for edit", e)
                showToast("Error loading profile")
            }
        }
    }
    
    companion object {
        const val TAG = "ProfileDialogFragment"
        
        fun newInstance(): ProfileDialogFragment {
            return ProfileDialogFragment()
        }
    }
}

