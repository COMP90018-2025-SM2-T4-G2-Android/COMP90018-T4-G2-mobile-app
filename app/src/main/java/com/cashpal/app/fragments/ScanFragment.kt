package com.cashpal.app.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.mlkit.vision.barcode.common.Barcode
import com.cashpal.app.adapters.RecentScanAdapter
import com.cashpal.app.models.RecentScan

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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        val sampleScans = List(20) { i ->
            val scanDate = (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            RecentScan(
                vendor = "Vendor #$i",
                location = "Melbourne",
                date = dateFormat.format(scanDate.time),
                amount = "$${(i + 1) * 5}.00"
            )
        }

        recentScans.layoutManager = LinearLayoutManager(requireContext())
        recentScans.adapter = RecentScanAdapter(sampleScans) { scan ->
            Toast.makeText(requireContext(), "Scan Again: ${scan.vendor}", Toast.LENGTH_SHORT).show()
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
