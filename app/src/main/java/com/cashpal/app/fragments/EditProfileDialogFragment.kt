package com.cashpal.app.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.cashpal.app.R

class EditProfileDialogFragment : DialogFragment() {
    
    private lateinit var tvEditInitials: TextView
    private lateinit var ivEditProfilePhoto: ImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnUploadPhoto: View
    private lateinit var btnCameraPhoto: View
    private lateinit var btnCancel: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var btnCloseEdit: ImageView
    
    // Data passed from ProfileDialogFragment
    private var userName: String = "John Doe"
    private var userEmail: String = "john.doe@email.com"
    private var userPhone: String = "+1 (555) 123-4567"
    private var userAddress: String = "123 Main St, City, State 12345"
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        
        arguments?.let {
            userName = it.getString(ARG_NAME, userName)
            userEmail = it.getString(ARG_EMAIL, userEmail)
            userPhone = it.getString(ARG_PHONE, userPhone)
            userAddress = it.getString(ARG_ADDRESS, userAddress)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadUserData()
        setupClickListeners()
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    
    private fun initViews(view: View) {
        tvEditInitials = view.findViewById(R.id.tv_edit_initials)
        ivEditProfilePhoto = view.findViewById(R.id.iv_edit_profile_photo)
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etAddress = view.findViewById(R.id.et_address)
        btnUploadPhoto = view.findViewById(R.id.btn_upload_photo)
        btnCameraPhoto = view.findViewById(R.id.btn_camera_photo)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSaveChanges = view.findViewById(R.id.btn_save_changes)
        btnCloseEdit = view.findViewById(R.id.btn_close_edit)
    }
    
    private fun loadUserData() {
        etName.setText(userName)
        etEmail.setText(userEmail)
        etPhone.setText(userPhone)
        etAddress.setText(userAddress)
        tvEditInitials.text = getInitials(userName)
    }
    
    private fun setupClickListeners() {
        btnCloseEdit.setOnClickListener {
            dismiss()
        }
        
        btnUploadPhoto.setOnClickListener {
            showToast("Upload photo from gallery - Coming soon!")
            // TODO: Implement photo picker
            // You can use ActivityResultContracts.PickVisualMedia() here
        }
        
        btnCameraPhoto.setOnClickListener {
            showToast("Take photo with camera - Coming soon!")
            // TODO: Implement camera capture
            // You can use ActivityResultContracts.TakePicture() here
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnSaveChanges.setOnClickListener {
            if (validateInputs()) {
                saveProfileChanges()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        
        return when {
            name.isEmpty() -> {
                etName.error = "Name is required"
                etName.requestFocus()
                false
            }
            email.isEmpty() -> {
                etEmail.error = "Email is required"
                etEmail.requestFocus()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Invalid email address"
                etEmail.requestFocus()
                false
            }
            phone.isEmpty() -> {
                etPhone.error = "Phone is required"
                etPhone.requestFocus()
                false
            }
            address.isEmpty() -> {
                etAddress.error = "Address is required"
                etAddress.requestFocus()
                false
            }
            else -> true
        }
    }
    
    private fun saveProfileChanges() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        
        // TODO: Save to database or API
        // For now, just show a success message
        showToast("Profile updated successfully!")
        
        // Pass data back to parent fragment if needed
        // You can use setFragmentResult here
        
        dismiss()
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
        const val TAG = "EditProfileDialogFragment"
        private const val ARG_NAME = "name"
        private const val ARG_EMAIL = "email"
        private const val ARG_PHONE = "phone"
        private const val ARG_ADDRESS = "address"
        
        fun newInstance(
            name: String = "John Doe",
            email: String = "john.doe@email.com",
            phone: String = "+1 (555) 123-4567",
            address: String = "123 Main St, City, State 12345"
        ): EditProfileDialogFragment {
            val fragment = EditProfileDialogFragment()
            val args = Bundle()
            args.putString(ARG_NAME, name)
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PHONE, phone)
            args.putString(ARG_ADDRESS, address)
            fragment.arguments = args
            return fragment
        }
    }
}

