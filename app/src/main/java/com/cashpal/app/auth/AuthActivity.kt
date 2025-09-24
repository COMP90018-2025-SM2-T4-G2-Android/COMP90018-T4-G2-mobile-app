package com.cashpal.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.MainActivity
import com.cashpal.app.R
import com.cashpal.app.databinding.ActivityAuthBinding
import com.cashpal.app.di.ServiceLocator
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val repository = ServiceLocator.getRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupClickListeners()
        checkAuthStatus()
    }
    
    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                signIn(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val displayName = binding.etDisplayName.text.toString().trim()
            
            if (email.isNotEmpty() && password.isNotEmpty() && displayName.isNotEmpty()) {
                signUp(email, password, displayName)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkAuthStatus() {
        if (repository.isUserSignedIn()) {
            navigateToMain()
        }
    }
    
    private fun signIn(email: String, password: String) {
        lifecycleScope.launch {
            repository.signInWithEmail(email, password).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Toast.makeText(this@AuthActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AuthActivity, "Sign in failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    private fun signUp(email: String, password: String, displayName: String) {
        lifecycleScope.launch {
            repository.signUpWithEmail(email, password, displayName).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Toast.makeText(this@AuthActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AuthActivity, "Sign up failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
