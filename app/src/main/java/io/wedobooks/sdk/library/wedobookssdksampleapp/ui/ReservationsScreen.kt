package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.Constants
import io.wedobooks.sdk.models.Reservation
import io.wedobooks.sdk.models.ReservationOffer
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

@Composable
fun ReservationsScreen() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val reservations by remember { WeDoBooksSdk.reservationOperations.reservationsFlow }
        .collectAsState(initial = emptyList())
    val offers by remember { WeDoBooksSdk.reservationOperations.reservationOffersFlow }
        .collectAsState(initial = emptyList())

    var isbn by remember { mutableStateOf(Constants.RESERVATION_BOOK.orEmpty()) }
    var isReserving by remember { mutableStateOf(false) }
    var reserveResult by remember { mutableStateOf<String?>(null) }
    var busyReservationId by remember { mutableStateOf<String?>(null) }
    var acceptingOfferId by remember { mutableStateOf<String?>(null) }
    var cancellingOfferId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "reserve") {
            ReserveCard(
                isbn = isbn,
                onIsbnChange = { isbn = it },
                isReserving = isReserving,
                result = reserveResult,
                onReserve = {
                    val materialId = isbn.trim()
                    coroutineScope.launch {
                        isReserving = true
                        reserveResult = runCatching {
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
        }

        item(key = "reservations-header") { SectionLabel("Reservations (${reservations.size})") }
        if (reservations.isEmpty()) {
            item(key = "reservations-empty") { EmptyLabel("No reservations yet") }
        } else {
            items(items = reservations, key = { "res-${it.id}" }) { reservation ->
                ReservationCard(
                    reservation = reservation,
                    isBusy = busyReservationId == reservation.id,
                    onDelete = {
                        coroutineScope.launch {
                            busyReservationId = reservation.id
                            val message = runCatching {
                                val ok = WeDoBooksSdk.reservationOperations
                                    .removeReservation(reservation.materialId)
                                if (ok) "Reservation deleted" else "Delete returned false"
                            }.getOrElse { "Delete failed: ${it.cause?.message ?: it.message}" }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            busyReservationId = null
                        }
                    },
                )
            }
        }

        item(key = "offers-header") { SectionLabel("Offers (${offers.size})") }
        if (offers.isEmpty()) {
            item(key = "offers-empty") { EmptyLabel("No active offers") }
        } else {
            items(items = offers, key = { "offer-${it.id}" }) { offer ->
                OfferCard(
                    offer = offer,
                    isAccepting = acceptingOfferId == offer.id,
                    isCancelling = cancellingOfferId == offer.id,
                    onAccept = {
                        coroutineScope.launch {
                            acceptingOfferId = offer.id
                            val message = runCatching {
                                val response = WeDoBooksSdk.reservationOperations
                                    .acceptReservationOffer(offer.id)
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
                    },
                    onCancel = {
                        coroutineScope.launch {
                            cancellingOfferId = offer.id
                            // Cancelling an offer is done by deleting its underlying reservation.
                            val message = runCatching {
                                val ok = WeDoBooksSdk.reservationOperations
                                    .removeReservation(offer.materialId)
                                if (ok) "Offer cancelled" else "Cancel returned false"
                            }.getOrElse { "Cancel failed: ${it.cause?.message ?: it.message}" }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            cancellingOfferId = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReserveCard(
    isbn: String,
    onIsbnChange: (String) -> Unit,
    isReserving: Boolean,
    result: String?,
    onReserve: () -> Unit,
) {
    Card {
        Text(
            text = "Reserve a book",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = isbn,
            onValueChange = onIsbnChange,
            label = { Text("ISBN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CustomButton(
            title = "Reserve",
            isLoading = isReserving,
            enabled = isbn.isNotBlank(),
            onClick = onReserve,
        )
        result?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    isBusy: Boolean,
    onDelete: () -> Unit,
) {
    Card {
        Text(
            text = "ISBN ${reservation.materialId}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        MetaRow("Type", reservation.type.name)
        MetaRow("Material", reservation.materialType.name)
        MetaRow("Loan date", dateFormatter.format(reservation.loanDate))
        if (reservation.wordCount > 0) MetaRow("Words", reservation.wordCount.toString())
        if (reservation.duration > 0) MetaRow("Duration", "${reservation.duration}s")
        CustomButton(
            title = "Delete reservation",
            isLoading = isBusy,
            color = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary,
            onClick = onDelete,
        )
    }
}

@Composable
private fun OfferCard(
    offer: ReservationOffer,
    isAccepting: Boolean,
    isCancelling: Boolean,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Text(
            text = "ISBN ${offer.materialId}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        MetaRow("Status", offer.status.name)
        MetaRow("Expires", dateFormatter.format(offer.expiresAt))
        CustomButton(
            title = "Accept offer",
            isLoading = isAccepting,
            enabled = !isCancelling,
            onClick = onAccept,
        )
        CustomButton(
            title = "Cancel offer",
            isLoading = isCancelling,
            enabled = !isAccepting,
            color = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary,
            onClick = onCancel,
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
