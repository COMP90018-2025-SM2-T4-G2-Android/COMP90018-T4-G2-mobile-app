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
import com.cashpal.app.databinding.ActivitySignInBinding
import com.cashpal.app.di.ServiceLocator
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
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
        binding = ActivitySignInBinding.inflate(layoutInflater)
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
        
        binding.tvSignUpLink.setOnClickListener {
            navigateToSignUp()
        }
        
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
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
                        Toast.makeText(this@SignInActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@SignInActivity, "Sign in failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    
    private fun signInWithGoogle() {
        try {
            repository.signInWithGoogle(this@SignInActivity) { intent ->
                googleSignInLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this@SignInActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleGoogleSignInResult(data: Intent) {
        lifecycleScope.launch {
            repository.handleGoogleSignInResult(data).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Toast.makeText(this@SignInActivity, "Google Sign-In successful", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@SignInActivity, "Google Sign-In failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    
    private fun navigateToSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
