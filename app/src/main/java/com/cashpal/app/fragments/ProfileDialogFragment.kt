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
import com.cashpal.app.R

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
            showToast("Edit profile - Coming soon!")
            // TODO: Navigate to edit profile screen
            dismiss()
        }
    }
    
    private fun loadUserData() {
        // TODO: Load actual user data from your data source
        // For now, using placeholder data
        tvUserName.text = "John Doe"
        tvInitials.text = getInitials("John Doe")
        tvAccountType.text = "Account Holder"
        tvEmail.text = "john.doe@email.com"
        tvPhone.text = "+1 (555) 123-4567"
        tvAddress.text = "123 Main St, City, State 12345"
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
    
    companion object {
        const val TAG = "ProfileDialogFragment"
        
        fun newInstance(): ProfileDialogFragment {
            return ProfileDialogFragment()
        }
    }
}

