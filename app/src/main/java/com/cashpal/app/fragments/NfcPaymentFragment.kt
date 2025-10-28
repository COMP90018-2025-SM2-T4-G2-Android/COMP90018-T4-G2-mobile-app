package com.cashpal.app.fragments

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.cashpal.app.R
import com.google.android.material.appbar.MaterialToolbar

class NfcPaymentFragment : Fragment() {

    private lateinit var pulseOuter: View
    private lateinit var pulseInner: View
    private lateinit var statusText: TextView
    private lateinit var infoText: TextView
    private lateinit var toolbar: MaterialToolbar

    private var nfcAdapter: NfcAdapter? = null
    private val pulseAnimators = mutableListOf<Animator>()
    private var resetStatusRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nfc_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        pulseOuter = view.findViewById(R.id.pulse_outer)
        pulseInner = view.findViewById(R.id.pulse_inner)
        statusText = view.findViewById(R.id.tv_status)
        infoText = view.findViewById(R.id.tv_info)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        if (nfcAdapter == null) {
            statusText.text = getString(R.string.nfc_payment_no_support)
            infoText.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        startPulseAnimation()
        enableReaderMode()
    }

    override fun onPause() {
        super.onPause()
        stopPulseAnimation()
        disableReaderMode()
        resetStatusRunnable?.let { infoText.removeCallbacks(it) }
    }

    private fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            statusText.text = getString(R.string.nfc_payment_disabled)
            infoText.isVisible = false
            return
        }

        statusText.text = getString(R.string.nfc_payment_status_ready)
        infoText.isVisible = true
        infoText.text = getString(R.string.nfc_payment_info)

        adapter.enableReaderMode(
            requireActivity(),
            { tag -> handleDetectedTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    private fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(requireActivity())
    }

    private fun handleDetectedTag(@Suppress("UNUSED_PARAMETER") tag: Tag) {
        view?.post {
            statusText.text = getString(R.string.nfc_payment_received)
            infoText.isVisible = true
            infoText.text = getString(R.string.nfc_payment_processing)
            resetStatusRunnable?.let { infoText.removeCallbacks(it) }
            resetStatusRunnable = Runnable {
                infoText.text = getString(R.string.nfc_payment_info)
                statusText.text = getString(R.string.nfc_payment_status_ready)
            }
            infoText.postDelayed(resetStatusRunnable!!, 2000)
        }
    }

    private fun startPulseAnimation() {
        if (pulseAnimators.isNotEmpty()) return
        pulseAnimators += createPulseAnimator(pulseOuter, 0L)
        pulseAnimators += createPulseAnimator(pulseInner, 600L)
        pulseAnimators.forEach { it.start() }
    }

    private fun stopPulseAnimation() {
        pulseAnimators.forEach { it.cancel() }
        pulseAnimators.clear()
        pulseOuter.alpha = 0f
        pulseInner.alpha = 0f
    }

    private fun createPulseAnimator(target: View, startDelay: Long): AnimatorSet {
        target.alpha = 0f
        target.scaleX = 1f
        target.scaleY = 1f

        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.6f)
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.6f)
        val alpha = ObjectAnimator.ofFloat(target, View.ALPHA, 0.6f, 0f)

        listOf(scaleX, scaleY, alpha).forEach { animator ->
            animator.duration = 1600
            animator.startDelay = startDelay
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.RESTART
        }

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
        }
    }
}
