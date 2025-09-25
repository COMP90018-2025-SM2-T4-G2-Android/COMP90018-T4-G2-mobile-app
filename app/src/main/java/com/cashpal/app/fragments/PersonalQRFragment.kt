package com.cashpal.app.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import com.cashpal.app.R
import com.cashpal.app.models.QRPayload
import com.cashpal.app.utils.QRCodeUtils
import com.google.gson.Gson

class PersonalQRFragment : Fragment() {

    private lateinit var qrImage: ImageView
    private lateinit var txtName: TextView
    private lateinit var txtPhone: TextView
    private lateinit var edtAmount: EditText

    // Example current user (later replace with logged-in user data)
    private val currentUser = QRPayload(
        id = "user_001",
        name = "Your Name",
        phone = "+1 (555) 123-4567",
        type = "personal",
        amount = null
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_personal_qr, container, false)

        // Bind views
        qrImage = view.findViewById(R.id.img_qr_code)
        txtName = view.findViewById(R.id.txt_user_name)
        txtPhone = view.findViewById(R.id.txt_user_phone)
        edtAmount = view.findViewById(R.id.edt_amount)

        // Set static user info
        txtName.text = currentUser.name
        txtPhone.text = currentUser.phone

        // Generate QR without amount first
        updateQRCode(payload = currentUser)

        // Update QR dynamically when amount changes
        edtAmount.addTextChangedListener { editable ->
            val amount: Double? = editable?.toString()?.trim()?.toDoubleOrNull()
            updateQRCode(payload = currentUser.copy(amount = amount))
        }

        // TODO: Hook up Copy, Share, Save buttons here

        return view
    }

    private fun updateQRCode(payload: QRPayload) {
        // Convert payload to JSON
        val json = Gson().toJson(payload)

        // Generate QR bitmap
        val qrBitmap: Bitmap? = QRCodeUtils.generateQRCode(json)

        // Display QR
        qrBitmap?.let {
            qrImage.setImageBitmap(it)
        }
    }
}
