package com.cashpal.app.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.MainActivity
import com.cashpal.app.R
import com.cashpal.app.databinding.ActivitySignInBinding
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.dialogs.ForgotPasswordDialog
import com.cashpal.app.utils.BiometricPreferences
import com.cashpal.app.utils.LocationHelper
import com.cashpal.app.utils.LocationRisk
import com.cashpal.app.utils.NotificationService
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val repository = ServiceLocator.getRepository()

    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var biometricPreferences: BiometricPreferences

    // Post-login location flow state
    private var postLoginThenGo: (() -> Unit)? = null

    // Location permission for fraud checks
    private val requestFineLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val thenGo = postLoginThenGo
            postLoginThenGo = null
            if (granted) runPostLoginSecurityWithLocation(thenGo ?: { navigateToMain() })
            else thenGo?.invoke()
        }

    // Notifications permission (Android 13+)
    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    // Google sign-in launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { handleGoogleSignInResult(it) }
        } else {
            Toast.makeText(this, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationsIfNeeded()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom); insets
        }

        biometricManager = BiometricAuthManager(this, this)
        biometricPreferences = BiometricPreferences(this)

        setupClickListeners()
        checkAuthStatus()
        setupBiometricAvailability()
        loadLastLoginEmail()

        // If we were launched for “reset pin” verification from an older flow, ignore it now.
        if (intent.hasExtra("reset_pin_mode")) {
            intent.removeExtra("reset_pin_mode")
            intent.removeExtra("new_pin_candidate")
        }
    }

    // ----------------------------------------------------
    // UI bindings
    // ----------------------------------------------------
    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signIn(email, password)
        }

        binding.tvSignUpLink.setOnClickListener { navigateToSignUp() }
        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }

        binding.btnGoogleSignIn.setOnClickListener { signInWithGoogle() }

        // Quick login with biometrics; sheet will have a "Use Password" button
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
        BiometricPasswordStore.getEmail(this)?.let { binding.etEmail.setText(it) }
    }

    // ----------------------------------------------------
    // Sign-in flows
    // ----------------------------------------------------
    private fun signIn(email: String, password: String, fromBiometric: Boolean = false) {
        lifecycleScope.launch {
            repository.signInWithEmail(email, password).collect { result ->
                result.fold(
                    onSuccess = {
                        if (fromBiometric || BiometricPasswordStore.isEnabled(this@SignInActivity)) {
                            handlePostLoginSecurity { navigateToMain() }
                        } else {
                            // After first manual login, offer to enable quick biometric login
                            maybeEnableBiometrics(email, password)
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@SignInActivity, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun signInWithGoogle() {
        try {
            repository.signInWithGoogle(this) { intent -> googleSignInLauncher.launch(intent) }
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
                        handlePostLoginSecurity { navigateToMain() }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@SignInActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // ----------------------------------------------------
    // Biometric quick login (with "Use Password" fallback)
    // ----------------------------------------------------
    private fun maybeEnableBiometrics(email: String, password: String) {
        if (!biometricManager.isBiometricAvailable() || BiometricPasswordStore.isEnabled(this)) {
            handlePostLoginSecurity { navigateToMain() }
            return
        }

        val cipher = BiometricPasswordStore.createEncryptCipher()
        biometricManager.showBiometricPrompt(
            title = "Enable Biometric Login",
            subtitle = "Use fingerprint next time you log in",
            crypto = BiometricPrompt.CryptoObject(cipher),
            callback = object : BiometricAuthManager.BiometricCallback {
                override fun onSuccess() {
                    BiometricPasswordStore.saveEncrypted(this@SignInActivity, email, cipher, password)
                    Toast.makeText(this@SignInActivity, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
                    handlePostLoginSecurity { navigateToMain() }
                }
                override fun onError(errorCode: Int, errorMessage: String) {
                    handlePostLoginSecurity { navigateToMain() }
                }
                override fun onFailed() {
                    handlePostLoginSecurity { navigateToMain() }
                }
            },
            onUsePassword = {
                // If user prefers password here, just continue (biometrics not enabled yet)
                handlePostLoginSecurity { navigateToMain() }
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
            crypto = BiometricPrompt.CryptoObject(decCipher),
            callback = object : BiometricAuthManager.BiometricCallback {
                override fun onSuccess() {
                    try {
                        val plainPassword = String(decCipher.doFinal(enc), Charsets.UTF_8)
                        signIn(email, plainPassword, fromBiometric = true)
                    } catch (e: Exception) {
                        // Key/IV invalidated (reinstall, fingerprint change, etc.)
                        BiometricPasswordStore.disable(this@SignInActivity)
                        Toast.makeText(
                            this@SignInActivity,
                            "Biometric key reset — please sign in and re-enable biometric login.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                override fun onError(errorCode: Int, errorMessage: String) { /* ignore */ }
                override fun onFailed() { /* ignore */ }
            },
            negativeLabel = "Use Password",
            onUsePassword = {
                // Fallback: show a small password dialog and verify; on success run normal sign-in
                showPasswordReauthDialog(prefillEmail = email) { ok, pw ->
                    if (ok && email != null) signIn(email, pw)
                }
            }
        )
    }

    // ----------------------------------------------------
    // Post-login security (GPS-based fraud check)
    // ----------------------------------------------------
    private fun handlePostLoginSecurity(thenGo: () -> Unit) {
        if (!hasLocationPermission()) {
            postLoginThenGo = thenGo
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        runPostLoginSecurityWithLocation(thenGo)
    }

    private fun runPostLoginSecurityWithLocation(thenGo: () -> Unit) {
        LocationHelper.getOneShotLocation(this) { currentLoc ->
            if (currentLoc == null) { thenGo(); return@getOneShotLocation }

            val suspicious = try {
                LocationRisk.isLoginLocationSuspicious(this, currentLoc)
            } catch (_: Exception) { false }

            if (suspicious) {
                NotificationService.showFraudAlert(
                    this,
                    "Sign-in from a new location. Additional verification required."
                )

                // Password re-auth (no PIN)
                showPasswordReauthDialog(prefillEmail = binding.etEmail.text?.toString()) { ok, _ ->
                    if (ok) {
                        LocationRisk.saveLastLoginFix(this, currentLoc)
                        thenGo()
                    }
                }
            } else {
                LocationRisk.saveLastLoginFix(this, currentLoc)
                thenGo()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // ----------------------------------------------------
    // Password re-auth dialog (used for fraud & biometric fallback)
    // ----------------------------------------------------
    private fun showPasswordReauthDialog(
        prefillEmail: String? = null,
        onResult: (ok: Boolean, password: String) -> Unit
    ) {
        val emailInput = android.widget.EditText(this).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(prefillEmail ?: BiometricPasswordStore.getEmail(this@SignInActivity) ?: "")
        }
        val pwInput = android.widget.EditText(this).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(emailInput); addView(pwInput)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm your password")
            .setMessage("For security, please re-enter your password.")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Verify", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val email = emailInput.text.toString().trim()
                    val pw = pwInput.text.toString()

                    if (email.isEmpty() || pw.isEmpty()) {
                        Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        repository.signInWithEmail(email, pw).collect { result ->
                            result.fold(
                                onSuccess = {
                                    dialog.dismiss()
                                    onResult(true, pw)
                                },
                                onFailure = { err ->
                                    Toast.makeText(
                                        this@SignInActivity,
                                        "Password incorrect: ${err.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
        }
        dialog.show()
    }

    // ----------------------------------------------------
    // Misc
    // ----------------------------------------------------
    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
        ForgotPasswordDialog().show(supportFragmentManager, "ForgotPasswordDialog")
    }

    private fun enableDemoMode() {
        biometricPreferences.setDemoMode(true)
        handlePostLoginSecurity {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("demo_mode", true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
