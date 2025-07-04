package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.StatsScreenViewModel
import io.wedobooks.sdk.models.StatEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

private const val SELECT_DATE_TEXT = "Select Date"

data class Stat(
    override val audioMinutes: Int = 0,
    override val ebookMinutes: Int = 0,
    override val audioSeconds: Int = 0,
    override val ebookSeconds: Int = 0,
    override val wordsRead: Int = 0
) : StatEntry {
    override val minutesRead = audioMinutes + ebookMinutes
    override val secondsRead = audioSeconds + ebookSeconds
}

@Composable
fun StatsScreen(
    checkoutId: String?,
    goBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val vm: StatsScreenViewModel = viewModel(
        factory = StatsScreenViewModel.factory(checkoutId)
    )
    var selectedDate by remember {
        mutableStateOf<Date?>(null)
    }
    val formatter by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
    }
    val selectedDateText by remember(selectedDate) {
        derivedStateOf {
            selectedDate?.let {
                formatter.format(it)
            } ?: SELECT_DATE_TEXT
        }
    }

    val statsForCurrentYear by vm.statsForCurrentYear.collectAsState(emptyMap())
    val statsForCheckout by vm.statsForCheckout.collectAsState(emptyMap())

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            if (checkoutId != null) {
                2
            } else 1
        }
    )

    fun showDatePickerDialog(
        minDate: Date? = null,
        onSelect: (Date) -> Unit,
    ) {
        val startDateCalendar = Calendar.getInstance()
        startDateCalendar.time = Date()

        DatePickerDialog(
            ctx,
            { _, year, monthOfYear, dayOfMonth ->
                startDateCalendar.set(
                    /* year = */ year,
                    /* month = */ monthOfYear,
                    /* date = */ dayOfMonth,
                    /* hourOfDay = */ 0,
                    /* minute = */ 0,
                    /* second = */ 0,
                )
                onSelect(
                    startDateCalendar.time
                )
            },
            startDateCalendar.get(Calendar.YEAR),
            startDateCalendar.get(Calendar.MONTH),
            startDateCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = (Date()).time
            minDate?.time?.let {
                datePicker.minDate = it
            }
        }.show()
    }

    Column(modifier = Modifier
        .systemBarsPadding()
        .fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .clickable {
                        showDatePickerDialog {
                            selectedDate = it
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Column(
                    modifier = Modifier.size(width = 200.dp, height = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier,
                        text = selectedDateText,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        HorizontalPager(
            state = pagerState
        ) {
            when (it) {
                0 -> {
                    StatsView(
                        title = if (selectedDate != null) "stats for $selectedDateText" else "stats for 2025",
                        selectedDate = selectedDate,
                        selectedDateText = selectedDateText,
                        stats = statsForCurrentYear
                    )
                }
                1 -> {
                    StatsView(
                        title = "stats for chosen checkout",
                        selectedDate = selectedDate,
                        selectedDateText = selectedDateText,
                        stats = statsForCheckout
                    )
                }
                else -> Unit
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) {
                val isSelected = pagerState.currentPage == it
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .size(8.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }
        Surface(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    goBack()
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Column(
                modifier = Modifier.size(width = 200.dp, height = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier,
                    text = "back"
                )
            }
        }
    }
}

@Composable
private fun StatsView(
    title: String,
    selectedDate: Date?,
    selectedDateText: String,
    stats: Map<String, StatEntry>
) {
    val selectedStat by remember(stats, selectedDate) {
        derivedStateOf {
            var newStat = Stat()
            if (SELECT_DATE_TEXT == selectedDateText) {
                stats.forEach { (_, value) ->
                    newStat = newStat.copy(
                        audioMinutes = newStat.audioMinutes + value.audioMinutes,
                        ebookMinutes = newStat.ebookMinutes + value.ebookMinutes,
                        audioSeconds = newStat.audioSeconds + value.audioSeconds,
                        ebookSeconds = newStat.ebookSeconds + value.ebookSeconds,
                        wordsRead = newStat.wordsRead + value.wordsRead,
                    )
                }
                newStat
            } else {
                stats[selectedDateText]?.let {
                    newStat = newStat.copy(
                        audioMinutes = it.audioMinutes,
                        ebookMinutes = it.ebookMinutes,
                        audioSeconds = it.audioSeconds,
                        ebookSeconds = it.ebookSeconds,
                        wordsRead = it.wordsRead,
                    )
                }
                newStat
            }
        }
    }
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
            text = title,
            fontSize = 24.sp
        )
        SpacedToEndText(
            lhsText = "Audio Minutes",
            rhsText = selectedStat.audioMinutes.toString()
        )
        SpacedToEndText(
            lhsText = "Ebook Minutes",
            rhsText = selectedStat.ebookMinutes.toString()
        )
        SpacedToEndText(
            lhsText = "Audio Seconds",
            rhsText = selectedStat.audioSeconds.toString()
        )
        SpacedToEndText(
            lhsText = "Ebook Seconds",
            rhsText = selectedStat.ebookSeconds.toString()
        )
        SpacedToEndText(
            lhsText = "Words Read",
            rhsText = selectedStat.wordsRead.toString()
        )
        SpacedToEndText(
            lhsText = "Minutes Read",
            rhsText = selectedStat.minutesRead.toString()
        )
        SpacedToEndText(
            lhsText = "Seconds Read",
            rhsText = selectedStat.secondsRead.toString()
        )
    }
}

@Composable
private fun SpacedToEndText(
    lhsText: String,
    rhsText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = lhsText
        )
        Text(
            text = rhsText
        )
    }
}