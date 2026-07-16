package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.PagerDots
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.StatsScreenViewModel
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.StatData
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SELECT_DATE_TEXT = "All time"

/**
 * @param checkouts All active checkouts whose stats should be paged through.
 *                  Pass an empty list to show only the year pane.
 */
@Composable
fun StatsScreen(
    checkouts: List<Checkout>,
) {
    val ctx = LocalContext.current
    // viewModel() caches per-store; key the factory on the checkout ids so
    // the VM rebuilds when the active checkouts change.
    val checkoutsKey = remember(checkouts) { checkouts.joinToString(",") { it.id } }
    val vm: StatsScreenViewModel = viewModel(
        key = "stats-$checkoutsKey",
        factory = StatsScreenViewModel.factory(checkouts),
    )
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    val formatter by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()))
    }
    val selectedDateText by remember(selectedDate) {
        derivedStateOf {
            selectedDate?.let { formatter.format(it) } ?: SELECT_DATE_TEXT
        }
    }
    val datePickerLabel by remember(selectedDate) {
        derivedStateOf {
            selectedDate?.let { formatter.format(it) } ?: "Filter by date"
        }
    }

    val statsForCurrentYear by vm.statsForCurrentYear.collectAsState(emptyMap())

    // One page for the year + one page per checkout.
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 1 + vm.orderedCheckouts.size },
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
                    year, monthOfYear, dayOfMonth, 0, 0, 0,
                )
                onSelect(startDateCalendar.time)
            },
            startDateCalendar.get(Calendar.YEAR),
            startDateCalendar.get(Calendar.MONTH),
            startDateCalendar.get(Calendar.DAY_OF_MONTH),
        ).apply {
            datePicker.maxDate = Date().time
            minDate?.time?.let { datePicker.minDate = it }
        }.show()
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .clickable {
                        showDatePickerDialog { selectedDate = it }
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Column(
                    modifier = Modifier.size(width = 200.dp, height = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = datePickerLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        HorizontalPager(state = pagerState) { page ->
            val year = java.time.LocalDate.now().year
            if (page == 0) {
                StatsView(
                    title = "Year — $year",
                    subtitle = if (selectedDate != null) {
                        "on $selectedDateText"
                    } else "all time",
                    selectedDate = selectedDate,
                    selectedDateText = selectedDateText,
                    stats = statsForCurrentYear,
                )
            } else {
                val checkout = vm.orderedCheckouts.getOrNull(page - 1)
                val flow: Flow<Map<String, StatData>>? = checkout?.let {
                    vm.statsByCheckoutId[it.id]
                }
                val stats by (flow ?: emptyMapFlow()).collectAsState(emptyMap())
                StatsView(
                    title = checkout?.title?.takeIf { it.isNotBlank() } ?: "Untitled checkout",
                    subtitle = if (selectedDate != null) {
                        "on $selectedDateText"
                    } else "all time",
                    selectedDate = selectedDate,
                    selectedDateText = selectedDateText,
                    stats = stats,
                )
            }
        }

        PagerDots(pagerState = pagerState)

        HistoryPager()
    }
}

private fun emptyMapFlow(): Flow<Map<String, StatData>> = kotlinx.coroutines.flow.flowOf(emptyMap())

@Composable
internal fun StatsView(
    title: String,
    subtitle: String,
    selectedDate: Date?,
    selectedDateText: String,
    stats: Map<String, StatData>,
) {
    val selectedStat by remember(stats, selectedDate) {
        derivedStateOf {
            var newStat = StatData.EMPTY
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                StatRow(
                    label = "Audio listened",
                    primary = formatDuration(selectedStat.audioSeconds),
                    secondary = "${selectedStat.audioSeconds} s",
                )
                StatRow(
                    label = "Ebook read",
                    primary = formatDuration(selectedStat.ebookSeconds),
                    secondary = "${selectedStat.ebookSeconds} s",
                )
                StatRow(
                    label = "Total time",
                    primary = formatDuration(selectedStat.secondsRead),
                    secondary = "${selectedStat.secondsRead} s",
                )
                StatRow(
                    label = "Words read",
                    primary = formatThousands(selectedStat.wordsRead),
                    secondary = null,
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    primary: String,
    secondary: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = primary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}
