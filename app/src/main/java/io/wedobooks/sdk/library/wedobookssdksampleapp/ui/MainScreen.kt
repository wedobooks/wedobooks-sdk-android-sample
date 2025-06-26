package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.BookType
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.MainScreenViewModel
import io.wedobooks.sdk.models.CheckoutBook
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    goToReader: (CheckoutBook) -> Unit,
    goToLogin: () -> Unit,
    toggleDarkMode: () -> Unit,
) {
    val vm: MainScreenViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val isEbookLoading by vm.isEbookCheckoutLoading
    val isAudioBookLoading by vm.isAudioCheckoutLoading

    Box(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                CustomButton(
                    title = "Audio Book",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        coroutineScope.launch {
                            vm.getCheckout(BookType.AudioBook)?.let {
                                goToReader(it)
                            }
                        }
                    }
                )
                if (isAudioBookLoading) {
                    Spinner(modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            Row {
                CustomButton(
                    title = "EBook",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        coroutineScope.launch {
                            vm.getCheckout(BookType.EBook)?.let {
                                goToReader(it)
                            }
                        }
                    }
                )
                if (isEbookLoading) {
                    Spinner(modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            CustomButton(
                title = "Stop Audio",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    vm.stopAudio()
                }
            )
            CustomButton(
                title = "Log out",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    goToLogin()
                    vm.logout()
                }
            )
            CustomButton(
                title = "Reset downloads",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    vm.removeStorage()
                }
            )
            CustomButton(
                title = "Toggle Dark Mode",
                color = MaterialTheme.colorScheme.primary,
                onClick = toggleDarkMode
            )
        }
    }
}

@Composable
fun CustomButton(
    title: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(
            role = Role.Button,
            enabled = enabled,
            onClick = onClick
        ),
        border = BorderStroke(
            width = 2.dp, color = color
        ),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .widthIn(200.dp)
                .requiredHeight(48.dp),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                text = title,
                color = if (enabled) {
                    color
                } else color.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun Spinner(modifier: Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(44.dp),
        color = Color.Blue,
        strokeWidth = 4.dp
    )
}