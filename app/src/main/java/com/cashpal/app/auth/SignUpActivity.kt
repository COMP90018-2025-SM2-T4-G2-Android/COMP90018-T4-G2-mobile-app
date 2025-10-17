package com.cashpal.app.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.MainActivity
import com.cashpal.app.R
import com.cashpal.app.databinding.ActivitySignUpBinding
import com.cashpal.app.di.ServiceLocator
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val repository = ServiceLocator.getRepository()
    
    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                handleGoogleSignInResult(data)
            }
        } else {
            Toast.makeText(this, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
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
        
        binding.tvSignInLink.setOnClickListener {
            navigateToSignIn()
        }
        
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    private fun signUp(email: String, password: String, displayName: String) {
        lifecycleScope.launch {
            repository.signUpWithEmail(email, password, displayName).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Toast.makeText(this@SignUpActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@SignUpActivity, "Sign up failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    private fun signInWithGoogle() {
        try {
            repository.signInWithGoogle(this@SignUpActivity) { intent ->
                googleSignInLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this@SignUpActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleGoogleSignInResult(data: Intent) {
        lifecycleScope.launch {
            repository.handleGoogleSignInResult(data).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Toast.makeText(this@SignUpActivity, "Google Sign-In successful", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@SignUpActivity, "Google Sign-In failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    private fun navigateToSignIn() {
        finish() // Go back to SignInActivity
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
