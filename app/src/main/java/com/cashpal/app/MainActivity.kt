package com.cashpal.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.ScanContract   // ✅ Import ZXing
import com.journeyapps.barcodescanner.ScanOptions   // ✅ Import ZXing

class MainActivity : AppCompatActivity() {
    private var currentBalanceCents: Int = 10000

    // ✅ QR scanner launcher
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handlePayment(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val balanceText: TextView = findViewById(R.id.balanceValue)
        val depositButton: Button = findViewById(R.id.depositButton)
        val withdrawButton: Button = findViewById(R.id.withdrawButton)
        val scanButton: Button = findViewById(R.id.btnScan)   // ✅ new button

        fun updateBalanceText() {
            val dollars = currentBalanceCents / 100
            val cents = currentBalanceCents % 100
            balanceText.text = "$${dollars}.${cents.toString().padStart(2, '0')}"
        }

        updateBalanceText()

        depositButton.setOnClickListener {
            currentBalanceCents += 1000
            updateBalanceText()
            Toast.makeText(this, "Deposited $10", Toast.LENGTH_SHORT).show()
        }

        withdrawButton.setOnClickListener {
            if (currentBalanceCents >= 1000) {
                currentBalanceCents -= 1000
                updateBalanceText()
                Toast.makeText(this, "Withdrew $10", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Scan QR button
        scanButton.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan merchant QR code")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            barcodeLauncher.launch(options)
        }
    }

    // ✅ Handle scanned QR payment
    private fun handlePayment(qrData: String) {
        // Example QR string: "merchantId=12345&amount=500" (amount in cents)
        val params = qrData.split("&").associate {
            val (k, v) = it.split("=")
            k to v
        }

        val amount = params["amount"]?.toIntOrNull() ?: 0
        if (currentBalanceCents >= amount) {
            currentBalanceCents -= amount
            Toast.makeText(this, "Paid ${(amount / 100.0)} to merchant!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_LONG).show()
        }
    }
}
