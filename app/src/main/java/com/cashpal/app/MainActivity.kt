package com.cashpal.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var currentBalanceCents: Int = 10000
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(com.cashpal.app.R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val balanceText: TextView = findViewById(R.id.balanceValue)
        val depositButton: Button = findViewById(R.id.depositButton)
        val withdrawButton: Button = findViewById(R.id.withdrawButton)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

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

        // Handle bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_pay -> {
                    Toast.makeText(this, "Pay selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_scan -> {
                    Toast.makeText(this, "Scan selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "History selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_more -> {
                    Toast.makeText(this, "More selected", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        
        // Set Home as default selected item
        bottomNavigationView.selectedItemId = R.id.nav_home
        
        // Debug: Check if bottom navigation is visible
        Toast.makeText(this, "Bottom navigation initialized", Toast.LENGTH_SHORT).show()
    }
}
