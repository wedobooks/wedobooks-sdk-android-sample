package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.R
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBook
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.SavedIsbnPicker
import io.wedobooks.sdk.models.Reservation
import io.wedobooks.sdk.models.ReservationOffer
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

@Composable
fun ReservationsScreen(
    savedBooks: List<TestBook> = emptyList(),
    reservingIsbns: Set<String> = emptySet(),
    onReserveSaved: suspend (String) -> String = { "" },
    onForgetIsbn: (String) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val reservations by remember { WeDoBooksSdk.reservationOperations.reservationsFlow }
        .collectAsState(initial = emptyList())
    val offers by remember { WeDoBooksSdk.reservationOperations.reservationOffersFlow }
        .collectAsState(initial = emptyList())

    var showReserveSheet by remember { mutableStateOf(false) }
    var busyMaterialId by remember { mutableStateOf<String?>(null) }
    var acceptingOfferId by remember { mutableStateOf<String?>(null) }

    val entries = remember(reservations, offers) { mergeReservations(reservations, offers) }

    fun cancel(materialId: String) {
        coroutineScope.launch {
            busyMaterialId = materialId
            val message = runCatching {
                val ok = WeDoBooksSdk.reservationOperations.removeReservation(materialId)
                if (ok) "Reservation cancelled" else "Cancel returned false"
            }.getOrElse { "Cancel failed: ${it.cause?.message ?: it.message}" }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            busyMaterialId = null
        }
    }

    fun accept(offer: ReservationOffer) {
        coroutineScope.launch {
            acceptingOfferId = offer.id
            val message = runCatching {
                val response = WeDoBooksSdk.reservationOperations.acceptReservationOffer(offer.id)
                if (response.success) {
                    "Offer accepted — loan ${response.loanId}"
                } else {
                    buildString {
                        append("Offer not accepted")
                        response.canLoan?.let { append(" · "); append(it.name) }
                        response.quotaReject?.let {
                            append(" (quota "); append(it.quota); append(")")
                        }
                    }
                }
            }.getOrElse { "Accept failed: ${it.cause?.message ?: it.message}" }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            acceptingOfferId = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "reserve") {
            CustomButton(
                title = "Reserve a book by ISBN",
                onClick = { showReserveSheet = true },
            )
        }

        item(key = "reservations-header") {
            SectionLabel("Reservations (${entries.size}) · offers (${offers.size})")
        }
        if (entries.isEmpty()) {
            item(key = "reservations-empty") { EmptyLabel("No reservations yet") }
        } else {
            items(items = entries, key = { it.key }) { entry ->
                ReservationCard(
                    entry = entry,
                    isCancelling = busyMaterialId == entry.materialId,
                    isAccepting = entry.offer?.id != null && acceptingOfferId == entry.offer.id,
                    onCancel = { cancel(entry.materialId) },
                    onAccept = { entry.offer?.let(::accept) },
                )
            }
        }
    }

    if (showReserveSheet) {
        ReserveBookSheet(
            savedBooks = savedBooks,
            reservingIsbns = reservingIsbns,
            onReserveSaved = onReserveSaved,
            onForgetIsbn = onForgetIsbn,
            onDismiss = { showReserveSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReserveBookSheet(
    savedBooks: List<TestBook>,
    reservingIsbns: Set<String>,
    onReserveSaved: suspend (String) -> String,
    onForgetIsbn: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var isbn by remember { mutableStateOf("") }
    var isReserving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reserve a book",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text("ISBN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            result?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CustomButton(
                title = "Reserve",
                isLoading = isReserving,
                enabled = isbn.isNotBlank(),
                onClick = {
                    val materialId = isbn.trim()
                    coroutineScope.launch {
                        isReserving = true
                        result = runCatching {
                            val response =
                                WeDoBooksSdk.reservationOperations.reserveBook(materialId)
                            buildString {
                                append(response.canLoan.name)
                                response.message?.takeIf { it.isNotBlank() }
                                    ?.let { append(" · "); append(it) }
                            }
                        }.getOrElse { "Error: ${it.cause?.message ?: it.message}" }
                        isReserving = false
                    }
                },
            )
            SavedIsbnPicker(
                books = savedBooks,
                actionLabel = "RESERVE",
                busyIsbns = reservingIsbns,
                onAction = { saved ->
                    coroutineScope.launch { result = onReserveSaved(saved) }
                },
                onForget = onForgetIsbn,
            )
        }
    }
}

/**
 * A reservation and the offer that fulfils it, if one is open. An offer whose
 * reservation is no longer listed still gets a card, with [reservation] null.
 */
private class ReservationEntry(
    val reservation: Reservation?,
    val offer: ReservationOffer?,
) {
    val materialId: String get() = reservation?.materialId ?: offer!!.materialId
    val key: String get() = reservation?.let { "res-${it.id}" } ?: "offer-${offer!!.id}"
}

private fun mergeReservations(
    reservations: List<Reservation>,
    offers: List<ReservationOffer>,
): List<ReservationEntry> {
    val offersByReservation = offers.associateBy { it.reservationId }
    val matched = mutableSetOf<String>()
    val entries = reservations.map { reservation ->
        val offer = offersByReservation[reservation.id]
            ?: offers.firstOrNull { it.materialId == reservation.materialId }
        offer?.let { matched += it.id }
        ReservationEntry(reservation, offer)
    }
    val orphans = offers.filterNot { it.id in matched }.map { ReservationEntry(null, it) }
    return entries + orphans
}

@Composable
private fun ReservationCard(
    entry: ReservationEntry,
    isCancelling: Boolean,
    isAccepting: Boolean,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "ISBN ${entry.materialId}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            // Cancelling an offer deletes its underlying reservation, so one control covers both.
            // 48dp touch target, pulled right so the 24dp glyph ends flush with the value column.
            if (isCancelling) {
                Spinner(modifier = Modifier.size(24.dp))
            } else {
                IconButton(
                    modifier = Modifier
                        .size(48.dp)
                        .offset(x = 12.dp),
                    enabled = !isAccepting,
                    onClick = onCancel,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.wdb_ic_close),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Cancel reservation",
                    )
                }
            }
        }
        entry.reservation?.let { reservation ->
            reservation.queuePosition?.let { MetaRow("Queue position", it.toString()) }
            MetaRow("Type", reservation.type.name)
            MetaRow("Material", reservation.materialType.name)
            MetaRow("Loan date", dateFormatter.format(reservation.loanDate))
            if (reservation.wordCount > 0) MetaRow("Words", reservation.wordCount.toString())
            if (reservation.duration > 0) MetaRow("Duration", "${reservation.duration}s")
        }
        entry.offer?.let { offer ->
            MetaRow("Offer", offer.status.name)
            MetaRow("Offer expires", dateFormatter.format(offer.expiresAt))
        }
        CustomButton(
            title = if (entry.offer != null) "Accept offer" else "No offer yet",
            isLoading = isAccepting,
            enabled = entry.offer != null && !isCancelling,
            onClick = onAccept,
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodyMedium,
    )
}
