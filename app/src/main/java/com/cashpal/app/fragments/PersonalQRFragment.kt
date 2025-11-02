package com.cashpal.app.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.cashpal.app.R
import com.cashpal.app.models.QRPayload
import com.cashpal.app.utils.QRCodeUtils
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PersonalQRFragment : Fragment() {

    private lateinit var qrImage: ImageView
    private lateinit var txtName: TextView
    private lateinit var txtPhone: TextView
    private lateinit var edtAmount: EditText

    // Action buttons (containers in your layout)
    private lateinit var btnCopy: View
    private lateinit var btnShare: View
    private lateinit var btnSave: View
    private lateinit var btnBack: View

    // Hold the most recently generated QR bitmap
    private var lastQrBitmap: Bitmap? = null


    // Example user – replace with real user when available
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
    ): View {
        val view = inflater.inflate(R.layout.fragment_personal_qr, container, false)

        // Bind views
        qrImage   = view.findViewById(R.id.iv_personal_qr_code)
        txtName   = view.findViewById(R.id.tv_user_name)
        txtPhone  = view.findViewById(R.id.tv_user_phone)
        edtAmount = view.findViewById(R.id.et_amount_input)

        btnCopy   = view.findViewById(R.id.btn_copy_qr)
        btnShare  = view.findViewById(R.id.btn_share_qr)
        btnSave   = view.findViewById(R.id.btn_save_qr)
        btnBack = view.findViewById(R.id.btn_back)

        // Static user info
        txtName.text = currentUser.name
        txtPhone.text = currentUser.phone

        // Initial QR
        updateQRCode(currentUser)

        // Update QR when amount changes
        edtAmount.addTextChangedListener { editable ->
            val amount: Double? = editable?.toString()?.trim()?.toDoubleOrNull()
            updateQRCode(currentUser.copy(amount = amount))
        }

        // Actions
        btnCopy.setOnClickListener { copyQrToClipboard() }
        btnShare.setOnClickListener { shareQrImage() }
        btnSave.setOnClickListener  { saveQrImage() }
        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        return view
    }

    /** Build and show the QR, cache it in [lastQrBitmap] */
    private fun updateQRCode(payload: QRPayload) {
        val json = Gson().toJson(payload)

        // Purple QR on white, like your UI
        val bmp: Bitmap? = QRCodeUtils.generateQRCode(
            content = json,
            size = 1024, // crisp
            foregroundColor = Color.parseColor("#6C5CE7"),
            backgroundColor = Color.WHITE
        )

        bmp?.let {
            lastQrBitmap = it
            qrImage.setImageBitmap(it)
        }
    }

    // ---- Actions ----

    private fun requireReadyBitmap(): Bitmap? {
        // Prefer cached bitmap; fallback to imageView's drawable if needed
        val bmp = lastQrBitmap ?: (qrImage.drawable as? BitmapDrawable)?.bitmap
        if (bmp == null) {
            Toast.makeText(requireContext(), "QR code is not ready yet", Toast.LENGTH_SHORT).show()
        }
        return bmp
    }

    /** COPY image to clipboard (content URI). */
    private fun copyQrToClipboard() {
        val bmp = requireReadyBitmap() ?: return
        val uri = writePngToCache(requireContext(), bmp) ?: run {
            Toast.makeText(requireContext(), "Failed to copy QR", Toast.LENGTH_SHORT).show(); return
        }

        val clip = ClipData.newUri(
            requireContext().contentResolver,
            "CashPal QR",
            uri
        )
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "QR image copied", Toast.LENGTH_SHORT).show()
    }

    /** SHARE image using FileProvider URI. */
    private fun shareQrImage() {
        val bmp = requireReadyBitmap() ?: return
        val uri = writePngToCache(requireContext(), bmp) ?: run {
            Toast.makeText(requireContext(), "Failed to prepare QR", Toast.LENGTH_SHORT).show(); return
        }

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, "Share QR code"))
    }

    /** SAVE image to Pictures/CashPal (scoped storage aware). */
    private fun saveQrImage() {
        val bmp = requireReadyBitmap() ?: return
        val saved = savePngToPictures(requireContext(), bmp)
        if (saved != null) {
            Toast.makeText(requireContext(), "Saved to Gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Storage helpers ----

    /** Cache the PNG and return a FileProvider URI. */
    private fun writePngToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "cashpal_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Save PNG into Pictures/CashPal and return the content URI. */
    private fun savePngToPictures(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val filename = "cashpal_qr_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/CashPal"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } else {
                // <= Android 9: write to public Pictures/CashPal
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val dir = File(pictures, "CashPal").apply { mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Insert into MediaStore so it appears in Gallery
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        } catch (e: Exception) {
            null
        }
    }
}
