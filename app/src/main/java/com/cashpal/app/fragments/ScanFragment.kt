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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cashpal.app.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
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
    private var hasScanned = false // prevent multiple triggers

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraSession()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan", Toast.LENGTH_SHORT).show()
            }
        }

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

        startScanButton.setOnClickListener {
            if (hasCameraPermission()) {
                startCameraSession()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        qrOption.setOnClickListener {
            Toast.makeText(requireContext(), "QR Code option clicked", Toast.LENGTH_SHORT).show()
        }

        vendorOption.setOnClickListener {
            Toast.makeText(requireContext(), "Vendor Code option clicked", Toast.LENGTH_SHORT).show()
        }

        // RecyclerView setup
// Generate dummy list with 20 items for testing scroll
        val sampleScans = List(20) { i ->
            RecentScan(
                vendor = "Vendor #$i",
                location = "Melbourne",
                time = "10:${i}0 AM",
                amount = "$${(i + 1) * 5}.00"
            )
        }

        recentScans.layoutManager = LinearLayoutManager(requireContext())
        recentScans.adapter = RecentScanAdapter(sampleScans) { scan ->
            Toast.makeText(requireContext(), "Scan Again: ${scan.vendor}", Toast.LENGTH_SHORT).show()
        }



        // Animate scanning line
        startScanningLineAnimation()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraSession() {
        hasScanned = false
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

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
                        if (!hasScanned) {
                            hasScanned = true
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "QR Scanned: $qrText", Toast.LENGTH_LONG).show()
                                // TODO: navigate or trigger payment with qrText
                            }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
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

    override fun onDestroyView() {
        super.onDestroyView()
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
