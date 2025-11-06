package com.cashpal.app.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Represents a single sales observation in time.
 */
data class SalesEntry(
    val timestamp: Instant,
    val amount: Double
)

/**
 * Aggregated sales total for a specific calendar day.
 */
data class DailySalesBucket(
    val date: LocalDate,
    val total: Double
) {
    fun label(locale: java.util.Locale = java.util.Locale.getDefault()): String {
        return date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
    }
}

/**
 * Summary metrics for the POS dashboard daily sales card.
 */
data class DailySalesSummary(
    val todayTotal: Double,
    val yesterdayTotal: Double,
    val absoluteChange: Double,
    val changePercent: Double?,
    val totalForPeriod: Double,
    val buckets: List<DailySalesBucket>
) {
    val trendDirection: TrendDirection = when {
        absoluteChange > 0 -> TrendDirection.UP
        absoluteChange < 0 -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }

    enum class TrendDirection { UP, DOWN, FLAT }
}

object SalesAnalytics {

    /**
     * Calculate the merchant's daily sales for the trailing [days] window (inclusive).
     *
     * Only positive amounts are considered. Results are ordered chronologically.
     */
    fun calculateDailySales(
        entries: List<SalesEntry>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
        days: Int = 7
    ): DailySalesSummary? {
        if (days <= 0) return null
        if (entries.isEmpty()) return null

        val sanitized = entries.filter { it.amount > 0 }
        if (sanitized.isEmpty()) return null

        val startDate = today.minusDays((days - 1).toLong())
        val groupedTotals = sanitized
            .mapNotNull { entry ->
                val entryDate = entry.timestamp.atZone(zoneId).toLocalDate()
                if (entryDate.isBefore(startDate) || entryDate.isAfter(today)) {
                    null
                } else {
                    entryDate to entry.amount
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

        if (groupedTotals.isEmpty()) return null

        val buckets = (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailySalesBucket(date, groupedTotals[date] ?: 0.0)
        }

        val todayTotal = buckets.lastOrNull()?.total ?: 0.0
        val yesterdayTotal = if (buckets.size >= 2) buckets[buckets.size - 2].total else 0.0
        val absoluteChange = todayTotal - yesterdayTotal
        val changePercent = if (yesterdayTotal > 0.0) {
            (absoluteChange / yesterdayTotal) * 100.0
        } else {
            null
        }
        val totalForPeriod = buckets.sumOf { it.total }

        return DailySalesSummary(
            todayTotal = todayTotal,
            yesterdayTotal = yesterdayTotal,
            absoluteChange = absoluteChange,
            changePercent = changePercent,
            totalForPeriod = totalForPeriod,
            buckets = buckets
        )
    }
}
