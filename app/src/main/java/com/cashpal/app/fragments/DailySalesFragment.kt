package com.cashpal.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cashpal.app.R
import com.cashpal.app.data.DataRepository
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.DailySalesSummary
import com.cashpal.app.utils.SalesAnalytics
import com.cashpal.app.utils.SalesEntry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailySalesFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var subtitleText: TextView
    private lateinit var todayAmountText: TextView
    private lateinit var changeText: TextView
    private lateinit var totalText: TextView
    private lateinit var averageText: TextView
    private lateinit var chartContainer: LinearLayout
    private lateinit var breakdownList: LinearLayout
    private lateinit var chartCard: MaterialCardView
    private lateinit var breakdownCard: MaterialCardView
    private lateinit var scrollView: View
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateText: TextView

    private lateinit var repository: com.cashpal.app.repository.CashPalRepository
    private lateinit var dataRepository: DataRepository
    private var loadJob: Job? = null
    private var userCurrencyCode: String = "AUD"

    private val isDemoMode: Boolean
        get() = activity?.intent?.getBooleanExtra("demo_mode", false) == true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_daily_sales, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ServiceLocator.getRepository()
        dataRepository = DataRepository(requireContext(), repository)
        bindViews(view)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        subtitleText.text = getString(R.string.daily_sales_overview_subtitle)
        if (isDemoMode) {
            renderDemoSales()
        } else {
            loadDailySales()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
    }

    private fun bindViews(root: View) {
        toolbar = root.findViewById(R.id.dailySalesToolbar)
        subtitleText = root.findViewById(R.id.dailySalesSubtitle)
        todayAmountText = root.findViewById(R.id.dailySalesTodayAmount)
        changeText = root.findViewById(R.id.dailySalesChange)
        totalText = root.findViewById(R.id.dailySalesTotal)
        averageText = root.findViewById(R.id.dailySalesAverage)
        chartContainer = root.findViewById(R.id.dailySalesChartContainer)
        breakdownList = root.findViewById(R.id.dailySalesBreakdownList)
        chartCard = root.findViewById(R.id.dailySalesChartCard)
        breakdownCard = root.findViewById(R.id.dailySalesBreakdownCard)
        scrollView = root.findViewById(R.id.dailySalesScroll)
        loadingIndicator = root.findViewById(R.id.dailySalesLoading)
        emptyStateText = root.findViewById(R.id.dailySalesEmptyState)
    }

    private fun loadDailySales() {
        showLoading()
        val currentUserId = dataRepository.getCurrentUserId()
        if (currentUserId.isNullOrBlank() || !dataRepository.isUserSignedIn()) {
            showEmptyState(getString(R.string.daily_sales_sign_in_required))
            return
        }

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            fetchUserCurrency(currentUserId)
            repository.getUserTransactions(currentUserId, 1000).collectLatest { result ->
                result.fold(
                    onSuccess = { transactions ->
                        val summary = buildDailySalesSummary(transactions, currentUserId)
                        if (summary == null) {
                            showEmptyState(getString(R.string.daily_sales_empty_detail))
                        } else {
                            showSummary(summary)
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("DailySalesFragment", "Failed to load sales data", error)
                        showEmptyState(getString(R.string.daily_sales_load_error))
                    }
                )
            }
        }
    }

    private suspend fun fetchUserCurrency(userId: String) {
        try {
            val userResult = repository.getUserProfile(userId).first()
            userResult.getOrNull()?.currency?.takeIf { it.isNotBlank() }?.let {
                userCurrencyCode = it
            }
        } catch (e: Exception) {
            android.util.Log.w("DailySalesFragment", "Unable to resolve user currency, using default", e)
        }
    }

    private fun renderDemoSales() {
        showLoading()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        userCurrencyCode = try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "AUD"
        }
        val sampleAmounts = listOf(180.0, 220.0, 160.0, 240.0, 210.0, 260.0, 300.0)
        val entries = sampleAmounts.mapIndexed { index, amount ->
            val date = today.minusDays((sampleAmounts.size - 1 - index).toLong())
            SalesEntry(date.atStartOfDay(zone).toInstant(), amount)
        }
        val summary = SalesAnalytics.calculateDailySales(entries, zone, today)
        if (summary == null) {
            showEmptyState(getString(R.string.daily_sales_empty_detail))
        } else {
            showSummary(summary)
        }
    }

    private fun buildDailySalesSummary(
        transactions: List<com.cashpal.app.models.Transaction>,
        currentUserId: String
    ): DailySalesSummary? {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val entries = transactions.asSequence()
            .filter { it.status == com.cashpal.app.models.TransactionStatus.COMPLETED }
            .filter { it.toUserId == currentUserId }
            .mapNotNull { transaction ->
                if (transaction.amount <= 0) return@mapNotNull null
                val timestamp = transaction.completedAt ?: transaction.createdAt
                SalesEntry(timestamp.toDate().toInstant(), transaction.amount)
            }
            .toList()

        if (entries.isEmpty()) return null

        return SalesAnalytics.calculateDailySales(
            entries = entries,
            zoneId = zoneId,
            today = today
        )
    }

    private fun showLoading() {
        loadingIndicator.isVisible = true
        scrollView.isVisible = false
        emptyStateText.isVisible = false
    }

    private fun showSummary(summary: DailySalesSummary) {
        loadingIndicator.isVisible = false
        scrollView.isVisible = true
        emptyStateText.isVisible = false

        todayAmountText.text = formatCurrency(summary.todayTotal)
        changeText.text = formatTrendLabel(summary)
        applyTrendStyle(summary.trendDirection)

        val totalFormatted = formatCurrency(summary.totalForPeriod)
        totalText.text = getString(R.string.daily_sales_total, totalFormatted)

        val average = if (summary.buckets.isNotEmpty()) {
            summary.totalForPeriod / summary.buckets.size
        } else {
            0.0
        }
        averageText.text = getString(R.string.daily_sales_average, formatCurrency(average))

        renderChart(summary.buckets)
        renderBreakdown(summary.buckets)
    }

    private fun showEmptyState(message: String) {
        loadingIndicator.isVisible = false
        scrollView.isVisible = false
        emptyStateText.isVisible = true
        emptyStateText.text = message
    }

    private fun formatTrendLabel(summary: DailySalesSummary): String {
        val changePercent = summary.changePercent ?: return getString(R.string.daily_sales_change_no_data)
        val sign = if (summary.absoluteChange >= 0) "+" else "-"
        val absoluteText = formatCurrency(abs(summary.absoluteChange))
        val percentText = String.format(Locale.getDefault(), "%.1f%%", abs(changePercent))
        val combined = "$sign$absoluteText ($sign$percentText)"
        return getString(R.string.daily_sales_change_template, combined)
    }

    private fun applyTrendStyle(direction: DailySalesSummary.TrendDirection?) {
        val (backgroundRes, textRes) = when (direction) {
            DailySalesSummary.TrendDirection.UP -> R.color.received_background to R.color.received_color
            DailySalesSummary.TrendDirection.DOWN -> R.color.sent_background to R.color.sent_color
            else -> R.color.pay_soft_surface to R.color.muted_foreground
        }
        val backgroundColor = ContextCompat.getColor(requireContext(), backgroundRes)
        val textColor = ContextCompat.getColor(requireContext(), textRes)
        changeText.backgroundTintList = android.content.res.ColorStateList.valueOf(backgroundColor)
        changeText.setTextColor(textColor)
    }

    private fun renderChart(buckets: List<com.cashpal.app.utils.DailySalesBucket>) {
        chartContainer.removeAllViews()
        if (buckets.isEmpty()) {
            chartCard.isVisible = false
            breakdownCard.isVisible = false
            return
        }

        chartCard.isVisible = true
        breakdownCard.isVisible = true

        val maxTotal = buckets.maxOfOrNull { it.total } ?: 0.0
        val maxHeight = resources.displayMetrics.density.times(96).roundToInt()
        val minBarHeight = resources.displayMetrics.density.times(6).roundToInt()

        buckets.forEach { bucket ->
            val columnLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    val margin = resources.displayMetrics.density.times(6).roundToInt()
                    setMargins(margin, 0, margin, 0)
                }
            }

            val amountText = TextView(requireContext()).apply {
                text = if (bucket.total > 0) formatCurrency(bucket.total) else ""
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_foreground))
                visibility = if (bucket.total > 0) View.VISIBLE else View.INVISIBLE
            }

            val ratio = if (maxTotal > 0) bucket.total / maxTotal else 0.0
            val barHeight = when {
                bucket.total <= 0 -> minBarHeight
                else -> max((ratio * maxHeight).roundToInt(), minBarHeight)
            }

            val barView = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.displayMetrics.density.times(18).roundToInt(),
                    barHeight
                ).apply {
                    topMargin = resources.displayMetrics.density.times(8).roundToInt()
                    bottomMargin = resources.displayMetrics.density.times(8).roundToInt()
                }
                setBackgroundResource(R.drawable.daily_sales_bar_background)
                alpha = if (bucket.total > 0) 1f else 0.25f
            }

            val labelText = TextView(requireContext()).apply {
                text = bucket.label()
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_foreground))
                setPadding(0, resources.displayMetrics.density.times(4).roundToInt(), 0, 0)
            }

            columnLayout.addView(amountText)
            columnLayout.addView(barView)
            columnLayout.addView(labelText)
            chartContainer.addView(columnLayout)
        }
    }

    private fun renderBreakdown(buckets: List<com.cashpal.app.utils.DailySalesBucket>) {
        breakdownList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
        val today = LocalDate.now()

        buckets.asReversed().forEach { bucket ->
            val row = inflater.inflate(R.layout.item_daily_sales_row, breakdownList, false)
            val dayLabel = row.findViewById<TextView>(R.id.dailySalesDayLabel)
            val amountLabel = row.findViewById<TextView>(R.id.dailySalesAmountLabel)

            dayLabel.text = bucket.date.format(formatter)
            amountLabel.text = formatCurrency(bucket.total)

            if (bucket.date == today) {
                dayLabel.setTypeface(dayLabel.typeface, android.graphics.Typeface.BOLD)
                amountLabel.setTypeface(amountLabel.typeface, android.graphics.Typeface.BOLD)
            }

            breakdownList.addView(row)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        try {
            formatter.currency = Currency.getInstance(userCurrencyCode)
        } catch (e: Exception) {
            formatter.currency = Currency.getInstance("AUD")
        }
        return formatter.format(amount)
    }
}
