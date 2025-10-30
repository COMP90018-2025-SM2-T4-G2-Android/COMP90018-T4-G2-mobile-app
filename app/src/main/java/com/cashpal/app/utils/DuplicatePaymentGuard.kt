package com.cashpal.app.utils

import android.content.Context
import com.cashpal.app.data.Contact
import com.cashpal.app.data.PaymentRequest
import kotlin.math.roundToLong

/**
 * Simple duplicate-transfer detector.
 * Flags as duplicate if SAME contact + SAME amount occurs within [WINDOW_MS].
 */
object DuplicatePaymentGuard {
    private const val PREFS = "dup_payment_guard_prefs"
    private const val KEY_CONTACT_ID = "last_contact_id"
    private const val KEY_AMOUNT_CENTS = "last_amount_cents"
    private const val KEY_TIME_MS = "last_time_ms"

    // Adjust as you like (e.g., 5 * 60_000 for 5 min, 30 * 60_000 for 30 min)
    private const val WINDOW_MS = 15 * 60_000L

    /** Returns true if this payment matches the last one within the time window. */
    fun isDuplicate(ctx: Context, pr: PaymentRequest): Boolean {
        val (lastId, lastCents, lastTime) = load(ctx)
        val now = System.currentTimeMillis()
        val amountCents = (pr.amount * 100.0).roundToLong()
        val sameContact = lastId == contactKey(pr.contact)
        val sameAmount = lastCents == amountCents
        val withinWindow = lastTime != 0L && (now - lastTime) <= WINDOW_MS
        return sameContact && sameAmount && withinWindow
    }

    /** Save this payment as the most recent one (call after a successful send). */
    fun save(ctx: Context, pr: PaymentRequest) {
        val amountCents = (pr.amount * 100.0).roundToLong()
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONTACT_ID, contactKey(pr.contact))
            .putLong(KEY_AMOUNT_CENTS, amountCents)
            .putLong(KEY_TIME_MS, System.currentTimeMillis())
            .apply()
    }

    /** Clear the last record (optional helper). */
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun load(ctx: Context): Triple<String?, Long, Long> {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = sp.getString(KEY_CONTACT_ID, null)
        val cents = sp.getLong(KEY_AMOUNT_CENTS, Long.MIN_VALUE)
        val t = sp.getLong(KEY_TIME_MS, 0L)
        return Triple(id, cents, t)
    }

    // Choose a stable key for contacts (prefer id/email/phone)
    private fun contactKey(c: Contact): String =
        c.id ?: c.email ?: c.phone ?: c.name
}
