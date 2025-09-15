package com.cashpal.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var currentBalanceCents: Int = 10000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(com.cashpal.app.R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val balanceText: TextView = findViewById(R.id.balanceValue)
        val depositButton: Button = findViewById(R.id.depositButton)
        val withdrawButton: Button = findViewById(R.id.withdrawButton)

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
    }
}
