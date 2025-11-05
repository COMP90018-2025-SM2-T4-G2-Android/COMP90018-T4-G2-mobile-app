package com.cashpal.app.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.text.InputType
import android.widget.EditText
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.mlkit.vision.barcode.common.Barcode
import com.cashpal.app.adapters.RecentScanAdapter
import com.cashpal.app.data.DataRepository
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.models.RecentScan
import com.cashpal.app.models.TransactionType
import com.cashpal.app.repository.CashPalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

class ScanFragment : Fragment() {

    private lateinit var startScanButton: Button
    private lateinit var qrOption: LinearLayout
    private lateinit var vendorOption: LinearLayout
    private lateinit var recentScans: RecyclerView
    private lateinit var previewView: PreviewView
    private lateinit var scanningLine: View

    private lateinit var cameraExecutor: ExecutorService
    private var hasScanned = false  // prevent multiple triggers
    private var isScanningActive = false
    private var pendingCameraStart = false
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var repository: CashPalRepository
    private lateinit var dataRepository: DataRepository
    private lateinit var recentScanAdapter: RecentScanAdapter
    private var recentScansJob: Job? = null
    private var quickTransferJob: Job? = null
    
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        startScanButton = view.findViewById(R.id.btn_start_scanning)
        qrOption = view.findViewById(R.id.btn_qr_code)
        vendorOption = view.findViewById(R.id.btn_vendor_code)
        recentScans = view.findViewById(R.id.rv_recent_scans)
        previewView = view.findViewById(R.id.previewView)
        scanningLine = view.findViewById(R.id.scanningLine)

        cameraExecutor = Executors.newSingleThreadExecutor()

        runCatching {
            ServiceLocator.getRepository()
        }.onSuccess { repo ->
            repository = repo
            dataRepository = DataRepository(requireContext(), repo)
        }.onFailure { error ->
            Log.e("ScanFragment", "Failed to initialize repository", error)
        }

        // Start camera on button click or automatically if permissions granted
        startScanButton.setOnClickListener {
            if (hasCameraPermission()) {
                isScanningActive = true
                startCamera()
            } else {
                pendingCameraStart = true
                requestCameraPermission()
            }
        }

        qrOption.setOnClickListener {
            openPersonalQR()
        }

        vendorOption.setOnClickListener {
            openVendorQR()
        }

        // RecyclerView setup
        recentScans.layoutManager = LinearLayoutManager(requireContext())
        recentScanAdapter = RecentScanAdapter(emptyList()) { scan ->
            handleScanAgain(scan)
        }
        recentScans.adapter = recentScanAdapter

        recentScanAdapter.updateScans(createPlaceholderScans())

        if (this::repository.isInitialized && this::dataRepository.isInitialized) {
            loadRecentScans()
        }

        // Animate scanning line
        startScanningLineAnimation()
        
        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCameraStart) {
                    pendingCameraStart = false
                    isScanningActive = true
                    startCamera()
                }
            } else {
                // Permission denied
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadRecentScans() {
        val currentUserId = if (this::dataRepository.isInitialized) {
            dataRepository.getCurrentUserId()
        } else {
            null
        }

        if (currentUserId.isNullOrBlank() || !this::dataRepository.isInitialized || !dataRepository.isUserSignedIn()) {
            recentScanAdapter.updateScans(createPlaceholderScans())
            return
        }

        recentScansJob?.cancel()
        recentScansJob = viewLifecycleOwner.lifecycleScope.launch {
            repository.getUserTransactions(currentUserId, 20).collect { result ->
                result.fold(
                    onSuccess = { transactions ->
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val qrTransactions = transactions.filter { isQrTransaction(it) }
                        if (qrTransactions.isEmpty()) {
                            recentScanAdapter.updateScans(createPlaceholderScans())
                        } else {
                            val scans = qrTransactions.map { transaction ->
                                mapTransactionToRecentScan(transaction, currentUserId, dateFormat)
                            }
                            recentScanAdapter.updateScans(scans)
                        }
                    },
                    onFailure = { error ->
                        Log.e("ScanFragment", "Failed to load recent scans", error)
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                "Failed to load recent scans",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        recentScanAdapter.updateScans(createPlaceholderScans())
                    }
                )
            }
        }
    }

    private fun handleScanAgain(scan: RecentScan) {
        if (!this::repository.isInitialized || !this::dataRepository.isInitialized) {
            triggerRescan()
            return
        }

        val currentUserId = dataRepository.getCurrentUserId()
        val targetUserId = scan.targetUserId

        if (currentUserId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.pay_user_not_signed_in, Toast.LENGTH_SHORT).show()
            return
        }

        if (targetUserId.isNullOrBlank() || targetUserId == "system") {
            Toast.makeText(requireContext(), R.string.scan_quick_pay_failed, Toast.LENGTH_SHORT).show()
            triggerRescan()
            return
        }

        if (currentUserId == targetUserId) {
            Toast.makeText(requireContext(), R.string.pay_self_transfer_error, Toast.LENGTH_SHORT).show()
            return
        }

        promptQuickTransfer(scan, currentUserId, targetUserId)
    }

    private fun promptQuickTransfer(scan: RecentScan, currentUserId: String, targetUserId: String) {
        if (!isAdded) {
            return
        }

        val context = requireContext()
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            val currencyCode = scan.currency.takeIf { it.length == 3 }
            if (!currencyCode.isNullOrBlank()) {
                try {
                    currency = Currency.getInstance(currencyCode.uppercase(Locale.getDefault()))
                } catch (_: IllegalArgumentException) {
                    // Ignore invalid currency codes and fallback to locale default
                }
            }
        }
        val amountInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.scan_quick_pay_amount_hint, scan.currency)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.scan_quick_pay_title, scan.vendor))
            .setMessage(getString(R.string.scan_quick_pay_message, scan.vendor))
            .setView(amountInput)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.scan_quick_pay_send, null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            sendButton.setOnClickListener {
                val amountText = amountInput.text?.toString()?.trim()
                val amount = amountText?.toDoubleOrNull()

                if (amount == null || amount <= 0) {
                    amountInput.error = getString(R.string.send_money_amount_error)
                    return@setOnClickListener
                }

                sendButton.isEnabled = false
                cancelButton?.isEnabled = false

                quickTransferJob?.cancel()
                quickTransferJob = viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val profileResult = repository.getUserProfile(currentUserId).first()
                        val userProfile = profileResult.getOrNull()

                        if (userProfile == null) {
                            val message = profileResult.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                                ?: getString(R.string.send_money_failed_generic)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            sendButton.isEnabled = true
                            cancelButton?.isEnabled = true
                            return@launch
                        }

                        val balance = userProfile.balance
                        if (amount > balance) {
                            amountInput.error = getString(
                                R.string.scan_quick_pay_insufficient_balance,
                                currencyFormatter.format(balance)
                            )
                            sendButton.isEnabled = true
                            cancelButton?.isEnabled = true
                            return@launch
                        }

                        val metadata = scan.metadata.toMutableMap()
                        metadata["qrSource"] = "scan_again"
                        metadata["rescanPayment"] = true
                        metadata["rescanAmount"] = amount
                        metadata["rescanCurrency"] = scan.currency
                        metadata["rescanVendor"] = scan.vendor
                        scan.transactionId?.let { metadata["originalTransactionId"] = it }

                        val description = getString(R.string.pay_transaction_description, scan.vendor)

                        repository.processTransaction(
                            fromUserId = currentUserId,
                            toUserId = targetUserId,
                            amount = amount,
                            description = description,
                            transactionType = TransactionType.PAYMENT,
                            qrCodeData = scan.qrCodeData,
                            metadata = metadata
                        ).collect { result ->
                            result.fold(
                                onSuccess = {
                                    dialog.dismiss()
                                    Toast.makeText(
                                        context,
                                        getString(R.string.send_money_success, scan.vendor),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadRecentScans()
                                },
                                onFailure = { error ->
                                    sendButton.isEnabled = true
                                    cancelButton?.isEnabled = true
                                    Toast.makeText(
                                        context,
                                        error.localizedMessage?.takeIf { it.isNotBlank() }
                                            ?: getString(R.string.scan_quick_pay_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            return@collect
                        }
                    } catch (e: Exception) {
                        sendButton.isEnabled = true
                        cancelButton?.isEnabled = true
                        Toast.makeText(
                            context,
                            e.localizedMessage?.takeIf { it.isNotBlank() }
                                ?: getString(R.string.scan_quick_pay_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        quickTransferJob = null
                    }
                }
            }
        }

        dialog.setOnDismissListener {
            quickTransferJob?.cancel()
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun isQrTransaction(transaction: com.cashpal.app.models.Transaction): Boolean {
        if (!transaction.qrCodeData.isNullOrBlank()) {
            return true
        }

        val metadata = transaction.metadata
        val source = metadata["qrSource"]?.toString()?.lowercase(Locale.getDefault())
        if (source == "scan" || source == "qr" || source == "scan_again") {
            return true
        }

        val qrType = metadata["qrType"]?.toString()?.lowercase(Locale.getDefault())
        return qrType == "store" ||
            qrType == "payment" ||
            qrType == "personal" ||
            qrType == "vendor"
    }

    private fun mapTransactionToRecentScan(
        transaction: com.cashpal.app.models.Transaction,
        currentUserId: String,
        dateFormat: SimpleDateFormat
    ): RecentScan {
        val isReceived = transaction.toUserId == currentUserId
        val vendorName = transaction.description.takeIf { it.isNotBlank() }
            ?: if (isReceived) "Received Payment" else "Sent Payment"

        val location = transaction.location?.let { loc ->
            listOfNotNull(
                loc.address?.takeIf { it.isNotBlank() },
                loc.city?.takeIf { it.isNotBlank() },
                loc.country?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
        }.takeUnless { it.isNullOrBlank() } ?: "Unknown location"

        val currencyCode = transaction.currency.takeIf { it.isNotBlank() } ?: "AUD"
        val amountValue = String.format(Locale.getDefault(), "%.2f", transaction.amount)
        val targetUserId = if (isReceived) transaction.fromUserId else transaction.toUserId

        return RecentScan(
            vendor = vendorName,
            location = location,
            time = dateFormat.format(transaction.createdAt.toDate()),
            amount = "$$amountValue $currencyCode",
            targetUserId = targetUserId.takeIf { it.isNotBlank() },
            transactionId = transaction.id.takeIf { it.isNotBlank() },
            isReceived = isReceived,
            currency = currencyCode,
            qrCodeData = transaction.qrCodeData,
            metadata = transaction.metadata
        )
    }

    private fun createPlaceholderScans(): List<RecentScan> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        return (0 until 5).map { index ->
            val formattedDate = dateFormat.format(calendar.time)
            val amountValue = (index + 1) * 5.0
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            RecentScan(
                vendor = "Vendor #${index + 1}",
                location = "Melbourne",
                time = formattedDate,
                amount = "$${String.format(Locale.getDefault(), "%.2f", amountValue)} AUD"
            )
        }
    }

    private fun triggerRescan() {
        if (!isAdded) return

        if (isScanningActive) {
            hasScanned = false
            return
        }

        if (hasCameraPermission()) {
            hasScanned = false
            isScanningActive = true
            if (::startScanButton.isInitialized) {
                startScanButton.visibility = View.GONE
            }
            startCamera()
        } else {
            pendingCameraStart = true
            requestCameraPermission()
        }
    }

    private fun startCamera() {
        // Check if fragment is attached and view exists
        if (!isAdded || view == null) {
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            if (!isAdded || view == null) {
                return@addListener
            }

            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                pendingCameraStart = false

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QRCodeAnalyzerMLKit { qrText ->
                            if (!hasScanned && isAdded) {
                                hasScanned = true
                                isScanningActive = false
                                requireActivity().runOnUiThread {
                                    if (isAdded) {
                                        stopCamera()
                                        val bundle = Bundle().apply {
                                            putString("qrData", qrText)
                                        }
                                        val resultFragment = ScanResultFragment().apply {
                                            arguments = bundle
                                        }

                                        parentFragmentManager.beginTransaction()
                                            .replace(R.id.fragmentContainer, resultFragment)
                                            .addToBackStack("scan_result")
                                            .commit()
                                    }
                                }
                            }

                        })
                    }

                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                startScanButton.visibility = View.GONE
                isScanningActive = true
            } catch (exc: Exception) {
                isScanningActive = false
                startScanButton.visibility = View.VISIBLE
                exc.printStackTrace()
                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to start camera: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isScanningActive = false
        if (::startScanButton.isInitialized) {
            startScanButton.visibility = View.VISIBLE
        }
    }

    private fun openPersonalQR() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PersonalQRFragment())
            .addToBackStack("personal_qr")
            .commit()
    }

    private fun openVendorQR() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, VendorQRFragment())
            .addToBackStack("vendor_qr")
            .commit()
    }

    private fun startScanningLineAnimation() {
        val animation = TranslateAnimation(
            0f, 0f,
            -200f, 200f // adjust based on frame height
        )
        animation.duration = 2000
        animation.repeatMode = Animation.REVERSE
        animation.repeatCount = Animation.INFINITE
        scanningLine.startAnimation(animation)
    }

    override fun onResume() {
        super.onResume()
        if (!this::repository.isInitialized) {
            runCatching {
                ServiceLocator.getRepository()
            }.onSuccess { repo ->
                repository = repo
                dataRepository = DataRepository(requireContext(), repo)
            }.onFailure { error ->
                Log.e("ScanFragment", "Failed to initialize repository on resume", error)
            }
        }

        if (this::repository.isInitialized && this::dataRepository.isInitialized) {
            loadRecentScans()
        }

        // Reset scan flag when fragment resumes so user can scan again
        hasScanned = false
        if (isScanningActive && hasCameraPermission() && ::previewView.isInitialized) {
            startCamera()
        } else {
            startScanButton.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        recentScansJob?.cancel()
        quickTransferJob?.cancel()
        cameraExecutor.shutdown()
    }
}

/**
 * Analyzer that uses ML Kit BarcodeScanner to detect QR codes
 */
class QRCodeAnalyzerMLKit(
    private val onQRCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.format == Barcode.FORMAT_QR_CODE) {
                            barcode.rawValue?.let { onQRCodeScanned(it) }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }



}
