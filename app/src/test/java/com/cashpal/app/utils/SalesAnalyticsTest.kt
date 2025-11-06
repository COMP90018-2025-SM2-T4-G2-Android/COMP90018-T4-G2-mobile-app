package com.cashpal.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SalesAnalyticsTest {

    private val zoneId = ZoneId.of("UTC")
    private val referenceDate = LocalDate.of(2024, 6, 10)

    @Test
    fun `calculateDailySales returns null when no entries`() {
        val result = SalesAnalytics.calculateDailySales(
            entries = emptyList(),
            zoneId = zoneId,
            today = referenceDate
        )

        assertNull(result)
    }

    @Test
    fun `calculateDailySales aggregates totals for trailing week`() {
        val entries = listOf(
            SalesEntry(dayInstant(0), 120.0),
            SalesEntry(dayInstant(0), 30.0),
            SalesEntry(dayInstant(1), 80.0),
            SalesEntry(dayInstant(2), 50.0),
            SalesEntry(dayInstant(6), 10.0)
        )

        val summary = SalesAnalytics.calculateDailySales(
            entries = entries,
            zoneId = zoneId,
            today = referenceDate
        )

        requireNotNull(summary)

        assertEquals(150.0, summary.todayTotal, 0.001)
        assertEquals(80.0, summary.yesterdayTotal, 0.001)
        assertEquals(70.0, summary.absoluteChange, 0.001)
        assertEquals(87.5, summary.changePercent!!, 0.001)
        assertEquals(290.0, summary.totalForPeriod, 0.001)
        assertEquals(7, summary.buckets.size)
        assertTrue(summary.buckets.last().total == 150.0)
    }

    @Test
    fun `calculateDailySales ignores entries outside range and negative amounts`() {
        val entries = listOf(
            SalesEntry(dayInstant(0), 100.0),
            SalesEntry(dayInstant(3), -45.0),
            SalesEntry(dayInstant(8), 200.0) // 8 days ago -> excluded
        )

        val summary = SalesAnalytics.calculateDailySales(
            entries = entries,
            zoneId = zoneId,
            today = referenceDate
        )

        requireNotNull(summary)

        assertEquals(100.0, summary.todayTotal, 0.001)
        assertEquals(0.0, summary.yesterdayTotal, 0.001)
        assertEquals(null, summary.changePercent)
        assertEquals(100.0, summary.totalForPeriod, 0.001)
    }

    private fun dayInstant(daysAgo: Long): Instant {
        return referenceDate
            .minusDays(daysAgo)
            .atStartOfDay(zoneId)
            .toInstant()
    }
}
