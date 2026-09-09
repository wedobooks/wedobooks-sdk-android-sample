package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBook
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.Spinner

/**
 * Saved books as one-tap actions, shown wherever an ISBN would otherwise be
 * typed.
 *
 * Each entry is a button that performs the action immediately rather than
 * filling the text field - the field is for ISBNs you have not saved yet.
 * Entries are labelled with the title, which a book gains the first time it is
 * loaned; before that there is only the ISBN to show.
 */
@Composable
fun SavedIsbnPicker(
    books: List<TestBook>,
    actionLabel: String,
    busyIsbns: Set<String>,
    onAction: (String) -> Unit,
    onForget: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (books.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "$actionLabel A SAVED BOOK",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        books.forEach { book ->
            SavedBookButton(
                book = book,
                isBusy = busyIsbns.contains(book.isbn),
                onAction = { onAction(book.isbn) },
                onForget = onForget?.let { forget -> { forget(book.isbn) } },
            )
        }
    }
}

/**
 * Styled to match `CustomButton`, but with the dismiss affordance *inside* the
 * button rather than beside it, so the row reads as one full-width control
 * instead of a button that looks clipped.
 *
 * The inner clickable consumes the tap, so hitting the cross forgets the book
 * without also triggering the action.
 */
@Composable
private fun SavedBookButton(
    book: TestBook,
    isBusy: Boolean,
    onAction: () -> Unit,
    onForget: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, enabled = !isBusy, onClick = onAction),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(48.dp),
        ) {
            if (isBusy) {
                Spinner(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        // Keep the label clear of the cross on either side so a
                        // long title stays visually centred.
                        .padding(horizontal = 56.dp),
                    text = book.displayName,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (onForget != null && !isBusy) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .requiredHeight(48.dp)
                        .clickable(role = Role.Button, onClick = onForget)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✕",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
