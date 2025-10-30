package com.cashpal.app.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log
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
import com.cashpal.app.dialogs.ForgotPasswordDialog
import com.cashpal.app.utils.BiometricPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private val repository = ServiceLocator.getRepository()
    private val securityRepository = ServiceLocator.getSecurityRepository()
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var biometricPreferences: BiometricPreferences
    
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
        
        // Initialize biometric components
        biometricManager = BiometricAuthManager(this, this)
        biometricPreferences = BiometricPreferences(this)
        
        setupClickListeners()
        checkAuthStatus()
        setupBiometricAvailability()
        loadLastLoginEmail()
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
        
        // Handle forgot password click
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
        
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
        
        // Handle biometric login click
        binding.cardBiometric.setOnClickListener {
            handleBiometricLogin()
        }
        
        // Handle demo mode click
        binding.btnDemoMode.setOnClickListener {
            enableDemoMode()
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
                        // Save email for biometric login
                        biometricPreferences.setLastLoginEmail(email)
                        Toast.makeText(this@SignInActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                        kickOffSecuritySync()
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
                        kickOffSecuritySync()
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
    
    private fun setupBiometricAvailability() {
        if (!biometricManager.isBiometricAvailable()) {
            binding.cardBiometric.visibility = android.view.View.GONE
        }
    }
    
    private fun loadLastLoginEmail() {
        val lastEmail = biometricPreferences.getLastLoginEmail()
        if (!lastEmail.isNullOrEmpty()) {
            binding.etEmail.setText(lastEmail)
        }
    }
    
    private fun showForgotPasswordDialog() {
        val dialog = ForgotPasswordDialog()
        dialog.show(supportFragmentManager, "ForgotPasswordDialog")
    }
    
    private fun handleBiometricLogin() {
        if (!biometricManager.isBiometricAvailable()) {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        val lastEmail = biometricPreferences.getLastLoginEmail()
        if (lastEmail.isNullOrEmpty()) {
            Toast.makeText(this, "No saved credentials found. Please sign in manually first.", Toast.LENGTH_LONG).show()
            return
        }
        
        biometricManager.showBiometricPrompt(object : BiometricAuthManager.BiometricCallback {
            override fun onSuccess() {
                // For demo purposes, we'll just show a toast
                // In a real app, you'd retrieve stored credentials and sign in
                Toast.makeText(this@SignInActivity, "Biometric authentication successful!", Toast.LENGTH_SHORT).show()
                
                // Navigate to main activity (in demo mode)
                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                intent.putExtra("demo_mode", true)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            
            override fun onError(errorCode: Int, errorMessage: String) {
                Toast.makeText(this@SignInActivity, "Biometric error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailed() {
                Toast.makeText(this@SignInActivity, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun enableDemoMode() {
        biometricPreferences.setDemoMode(true)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("demo_mode", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun kickOffSecuritySync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val registerResult = securityRepository.registerDeviceForAlerts(applicationContext)
            if (registerResult.isFailure) {
                Log.w(
                    "SignInActivity",
                    "Device registration for fraud alerts failed",
                    registerResult.exceptionOrNull()
                )
            }

            val sessionResult = securityRepository.logLoginEvent(applicationContext)
            if (sessionResult.isFailure) {
                Log.w(
                    "SignInActivity",
                    "Login session logging failed",
                    sessionResult.exceptionOrNull()
                )
            }
        }
    }
}
