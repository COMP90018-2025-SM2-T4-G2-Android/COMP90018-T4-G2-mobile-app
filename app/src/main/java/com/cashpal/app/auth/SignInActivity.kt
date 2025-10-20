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
import androidx.biometric.BiometricPrompt
import com.cashpal.app.MainActivity
import com.cashpal.app.R
import com.cashpal.app.databinding.ActivitySignInBinding
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.dialogs.ForgotPasswordDialog
import com.cashpal.app.utils.BiometricPreferences
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val repository = ServiceLocator.getRepository()
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var biometricPreferences: BiometricPreferences

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { handleGoogleSignInResult(it) }
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

        biometricManager = BiometricAuthManager(this, this)
        biometricPreferences = BiometricPreferences(this)

        setupClickListeners()
        checkAuthStatus()
        setupBiometricAvailability()
        loadLastLoginEmail()
    }

    // -------------------------------
    // UI setup
    // -------------------------------
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

        binding.tvSignUpLink.setOnClickListener { navigateToSignUp() }
        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
        binding.btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        binding.cardBiometric.setOnClickListener { handleBiometricLogin() }
        binding.btnDemoMode.setOnClickListener { enableDemoMode() }
    }

    private fun checkAuthStatus() {
        if (repository.isUserSignedIn()) navigateToMain()
    }

    private fun setupBiometricAvailability() {
        if (!biometricManager.isBiometricAvailable()) {
            binding.cardBiometric.visibility = android.view.View.GONE
        }
    }

    private fun loadLastLoginEmail() {
        BiometricPasswordStore.getEmail(this)?.let {
            binding.etEmail.setText(it)
        }
    }

    // -------------------------------
    // Sign-In Flows
    // -------------------------------
// Add fromBiometric=false default
    private fun signIn(email: String, password: String, fromBiometric: Boolean = false) {
        lifecycleScope.launch {
            repository.signInWithEmail(email, password).collect { result ->
                result.fold(
                    onSuccess = {
                        // If user came from biometric OR it's already enabled → go straight in
                        if (fromBiometric || BiometricPasswordStore.isEnabled(this@SignInActivity)) {
                            navigateToMain()
                        } else {
                            // First time after manual login: offer to enable
                            maybeEnableBiometrics(email, password)
                        }
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@SignInActivity,
                            "Sign in failed: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }


    private fun signInWithGoogle() {
        try {
            repository.signInWithGoogle(this) { intent ->
                googleSignInLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGoogleSignInResult(data: Intent) {
        lifecycleScope.launch {
            repository.handleGoogleSignInResult(data).collect { result ->
                result.fold(
                    onSuccess = {
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

    // -------------------------------
    // Biometric Setup
    // -------------------------------

    private fun maybeEnableBiometrics(email: String, password: String) {
        // If not available or already enabled, just proceed
        if (!biometricManager.isBiometricAvailable() || BiometricPasswordStore.isEnabled(this)) {
            navigateToMain(); return
        }

        val cipher = BiometricPasswordStore.createEncryptCipher()
        biometricManager.showBiometricPrompt(
            title = "Enable Biometric Login",
            subtitle = "Use fingerprint next time you log in",
            BiometricPrompt.CryptoObject(cipher),
            callback = object : BiometricAuthManager.BiometricCallback {
                override fun onSuccess() {
                    BiometricPasswordStore.saveEncrypted(this@SignInActivity, email, cipher, password)
                    Toast.makeText(this@SignInActivity, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                override fun onError(errorCode: Int, errorMessage: String) {
                    Toast.makeText(this@SignInActivity, "Biometric setup error: $errorMessage", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                override fun onFailed() {
                    Toast.makeText(this@SignInActivity, "Biometric setup failed", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
            }
        )
    }


    private fun handleBiometricLogin() {
        if (!biometricManager.isBiometricAvailable() || !BiometricPasswordStore.isEnabled(this)) {
            Toast.makeText(this, "Biometric login not set up yet", Toast.LENGTH_SHORT).show()
            return
        }

        val email = BiometricPasswordStore.getEmail(this)
        val enc = BiometricPasswordStore.getEncryptedPassword(this)
        val decCipher = BiometricPasswordStore.createDecryptCipher(this)

        if (email == null || enc == null || decCipher == null) {
            Toast.makeText(this, "No stored credentials found", Toast.LENGTH_SHORT).show()
            return
        }

        biometricManager.showBiometricPrompt(
            title = "Login with Fingerprint",
            subtitle = "Authenticate to unlock your CashPal account",
            BiometricPrompt.CryptoObject(decCipher),
            callback = object : BiometricAuthManager.BiometricCallback {
                override fun onSuccess() {
                    try {
                        val plainPassword = String(decCipher.doFinal(enc), Charsets.UTF_8)
                        // Tell signIn this came from biometric
                        signIn(email, plainPassword, fromBiometric = true)
                    } catch (e: Exception) {
                        Toast.makeText(this@SignInActivity, "Decryption failed", Toast.LENGTH_SHORT).show()
                    }
                }


                override fun onError(errorCode: Int, errorMessage: String) {
                    Toast.makeText(this@SignInActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }

                override fun onFailed() {
                    Toast.makeText(this@SignInActivity, "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // -------------------------------
    // Misc helpers
    // -------------------------------
    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showForgotPasswordDialog() {
        val dialog = ForgotPasswordDialog()
        dialog.show(supportFragmentManager, "ForgotPasswordDialog")
    }

    private fun enableDemoMode() {
        biometricPreferences.setDemoMode(true)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("demo_mode", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
