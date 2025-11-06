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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.R
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.VendorQRPayload
import com.cashpal.app.utils.QRCodeUtils
import com.cashpal.app.utils.UserProfileCache
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VendorQRFragment : Fragment() {

    private val repository by lazy { ServiceLocator.getRepository() }

    private lateinit var qrImage: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var vendorName: TextView
    private lateinit var vendorCategory: TextView
    private lateinit var vendorLocation: TextView
    private lateinit var vendorId: TextView
    private lateinit var amountBadge: LinearLayout
    private lateinit var amountValue: TextView
    private lateinit var amountInput: EditText

    private lateinit var btnCopy: View
    private lateinit var btnShare: View
    private lateinit var btnSave: View
    private lateinit var btnBack: View

    private var lastQrBitmap: Bitmap? = null
    private var vendorInfo: VendorInfo? = null
    private var currentAmount: Double? = null
    private var qrGenerationJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_vendor_qr, container, false)
        bindViews(view)
        setupInteractions()
        loadVendorInfo()
        return view
    }

    private fun bindViews(root: View) {
        qrImage = root.findViewById(R.id.iv_vendor_qr_code)
        loadingOverlay = root.findViewById(R.id.fl_vendor_qr_loading)
        vendorName = root.findViewById(R.id.tv_vendor_name)
        vendorCategory = root.findViewById(R.id.tv_vendor_category)
        vendorLocation = root.findViewById(R.id.tv_vendor_location)
        vendorId = root.findViewById(R.id.tv_vendor_id)
        amountBadge = root.findViewById(R.id.ll_vendor_amount_display)
        amountValue = root.findViewById(R.id.tv_vendor_amount_value)
        amountInput = root.findViewById(R.id.et_vendor_amount_input)

        btnCopy = root.findViewById(R.id.btn_copy_vendor_qr)
        btnShare = root.findViewById(R.id.btn_share_vendor_qr)
        btnSave = root.findViewById(R.id.btn_save_vendor_qr)
        btnBack = root.findViewById(R.id.btn_back)

        amountBadge.visibility = View.GONE
        loadingOverlay.visibility = View.GONE
    }

    private fun setupInteractions() {
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        amountInput.addTextChangedListener { editable ->
            val amount = editable?.toString()?.trim()?.toDoubleOrNull()
            handleAmountChange(amount)
        }

        btnCopy.setOnClickListener { copyQrToClipboard() }
        btnShare.setOnClickListener { shareQrImage() }
        btnSave.setOnClickListener { saveQrImage() }
    }

    private fun loadVendorInfo() {
        val firebaseUser = repository.getCurrentUser()

        if (firebaseUser == null) {
            val fallbackInfo = VendorInfo(
                id = "merchant_guest",
                name = getString(R.string.vendor_qr_default_name),
                category = getString(R.string.vendor_qr_default_category),
                location = getString(R.string.vendor_qr_default_location),
                merchantId = "MERCHANT-000000",
                currency = "AUD",
                contact = null
            )
            vendorInfo = fallbackInfo
            applyVendorInfo(fallbackInfo)
            handleAmountChange(parseAmountInput())
            return
        }

        val fallbackName = firebaseUser.displayName
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            } ?: getString(R.string.vendor_qr_default_name)

        val fallbackContact = firebaseUser.phoneNumber ?: firebaseUser.email
        val merchantId = buildMerchantId(firebaseUser.uid)

        val initialInfo = VendorInfo(
            id = firebaseUser.uid,
            name = fallbackName,
            category = getString(R.string.vendor_qr_default_category),
            location = getString(R.string.vendor_qr_default_location),
            merchantId = merchantId,
            currency = "AUD",
            contact = fallbackContact
        )

        val cachedProfile = UserProfileCache.get()?.takeIf { it.id == firebaseUser.uid }
        val resolvedInfo = if (cachedProfile != null) {
            initialInfo.copy(
                name = cachedProfile.displayName.ifBlank { fallbackName },
                contact = cachedProfile.phoneNumber ?: fallbackContact
            )
        } else {
            initialInfo
        }

        vendorInfo = resolvedInfo
        applyVendorInfo(resolvedInfo)
        handleAmountChange(parseAmountInput())

        if (cachedProfile == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val profileResult = repository.getUserProfile(firebaseUser.uid).firstOrNull()
                profileResult?.getOrNull()?.let { user ->
                    UserProfileCache.update(user)
                    val name = user.displayName.ifBlank { fallbackName }
                    val contact = user.phoneNumber ?: fallbackContact
                    val updatedInfo = vendorInfo?.copy(
                        name = name,
                        contact = contact
                    )
                    vendorInfo = updatedInfo
                    if (updatedInfo != null) {
                        applyVendorInfo(updatedInfo)
                        updateQRCode()
                    }
                }
            }
        }
    }

    private fun applyVendorInfo(info: VendorInfo) {
        vendorName.text = info.name
        vendorCategory.text = info.category
        vendorLocation.text = info.location
        vendorId.text = getString(R.string.vendor_qr_merchant_id_format, info.merchantId)
        updateQRCode()
    }

    private fun handleAmountChange(amount: Double?) {
        currentAmount = amount?.takeIf { it > 0 }
        if (currentAmount != null) {
            amountBadge.visibility = View.VISIBLE
            amountValue.text = String.format(
                Locale.getDefault(),
                getString(R.string.vendor_qr_amount_format),
                currentAmount!!
            )
        } else {
            amountBadge.visibility = View.GONE
        }
        updateQRCode()
    }

    private fun parseAmountInput(): Double? {
        return amountInput.text?.toString()?.trim()?.toDoubleOrNull()
    }

    private fun updateQRCode() {
        val info = vendorInfo ?: return
        qrGenerationJob?.cancel()
        loadingOverlay.visibility = View.VISIBLE

        val payload = VendorQRPayload(
            id = info.id,
            name = info.name,
            merchantId = info.merchantId,
            category = info.category,
            location = info.location,
            contact = info.contact,
            amount = currentAmount,
            currency = info.currency,
            timestamp = System.currentTimeMillis().toString()
        )

        qrGenerationJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val json = Gson().toJson(payload)
            val bitmap = QRCodeUtils.generateQRCode(
                content = json,
                size = 640,
                foregroundColor = Color.parseColor("#0F9D58"),
                backgroundColor = Color.WHITE
            )

            withContext(Dispatchers.Main) {
                loadingOverlay.visibility = View.GONE
                if (!isActive) return@withContext
                bitmap?.let {
                    lastQrBitmap = it
                    qrImage.setImageBitmap(it)
                }
            }
        }
    }

    private fun requireReadyBitmap(): Bitmap? {
        val bitmap = lastQrBitmap ?: (qrImage.drawable as? BitmapDrawable)?.bitmap
        if (bitmap == null) {
            Toast.makeText(requireContext(), R.string.vendor_qr_bitmap_missing, Toast.LENGTH_SHORT).show()
        }
        return bitmap
    }

    private fun copyQrToClipboard() {
        val bitmap = requireReadyBitmap() ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val uri = writePngToCache(requireContext(), bitmap)
            withContext(Dispatchers.Main) {
                if (uri == null) {
                    Toast.makeText(requireContext(), R.string.vendor_qr_copy_failed, Toast.LENGTH_SHORT).show()
                } else {
                    val clip = ClipData.newUri(
                        requireContext().contentResolver,
                        "CashPal Vendor QR",
                        uri
                    )
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), R.string.vendor_qr_copy_success, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareQrImage() {
        val bitmap = requireReadyBitmap() ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val uri = writePngToCache(requireContext(), bitmap)
            withContext(Dispatchers.Main) {
                if (uri == null) {
                    Toast.makeText(requireContext(), R.string.vendor_qr_share_failed, Toast.LENGTH_SHORT).show()
                } else {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.vendor_qr_share_message, vendorInfo?.name ?: getString(R.string.vendor_qr_default_name)))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.vendor_qr_share_title)))
                }
            }
        }
    }

    private fun saveQrImage() {
        val bitmap = requireReadyBitmap() ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val uri = savePngToPictures(requireContext(), bitmap)
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    Toast.makeText(requireContext(), R.string.vendor_qr_save_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.vendor_qr_save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun writePngToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val dir = File(context.cacheDir, "vendor_qr").apply { mkdirs() }
            val file = File(dir, "cashpal_vendor_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun savePngToPictures(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val filename = "cashpal_vendor_qr_${System.currentTimeMillis()}.png"
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
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val dir = File(pictures, "CashPal").apply { mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

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

    private fun buildMerchantId(uid: String): String {
        val suffix = uid.takeLast(6).uppercase(Locale.getDefault())
        return "MERCHANT-$suffix"
    }

    private data class VendorInfo(
        val id: String,
        val name: String,
        val category: String,
        val location: String,
        val merchantId: String,
        val currency: String,
        val contact: String?
    )

    override fun onDestroyView() {
        super.onDestroyView()
        qrGenerationJob?.cancel()
    }
}
