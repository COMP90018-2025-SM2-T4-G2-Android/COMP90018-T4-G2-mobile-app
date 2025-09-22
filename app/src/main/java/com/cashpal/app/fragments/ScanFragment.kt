package com.cashpal.app.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cashpal.app.R
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var handledResult = false

    private var camera: Camera? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var torchEnabled = false

    private lateinit var btnFlash: ImageButton
    private lateinit var btnSwitchCamera: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        previewView = view.findViewById(R.id.previewView)
        btnFlash = view.findViewById(R.id.btnFlash)
        btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera)

        btnFlash.setOnClickListener { toggleFlash() }
        btnSwitchCamera.setOnClickListener { switchCamera() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    private fun startCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(context, cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(context: Context, cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // ✅ Only scan QR codes
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // ✅ no "format ="
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // ✅ important
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                analysis
            )
        } catch (exc: Exception) {
            Log.e("ScanFragment", "Camera binding failed", exc)
        }
    }

    private fun toggleFlash() {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                torchEnabled = !torchEnabled
                it.cameraControl.enableTorch(torchEnabled)
                Toast.makeText(requireContext(), if (torchEnabled) "Flash ON" else "Flash OFF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No flash available", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(requireContext(), "Camera not ready yet", Toast.LENGTH_SHORT).show()
    }

    private fun switchCamera() {
        cameraSelector =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA

        startCamera(requireContext())
        Toast.makeText(requireContext(), "Switched Camera", Toast.LENGTH_SHORT).show()
    }

    private fun processImageProxy(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val qrValue = barcodes[0].rawValue ?: ""
                        Log.d("ScanFragment", "QR Code Detected: $qrValue")
                        Toast.makeText(requireContext(), "QR Code: $qrValue", Toast.LENGTH_LONG).show()
                        // reset after scan so you can scan again
                        handledResult = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanFragment", "Scan failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close() // ✅ Always close!
                }
        } else {
            imageProxy.close()
        }
    }
}
