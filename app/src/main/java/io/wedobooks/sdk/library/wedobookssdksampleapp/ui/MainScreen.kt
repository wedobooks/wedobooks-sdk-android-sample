package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.MainScreenViewModel
import io.wedobooks.sdk.models.CheckoutBook
import io.wedobooks.sdk.models.enums.BookType
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    setCheckout: (CheckoutBook) -> Unit,
    goToReader: () -> Unit,
    goToLogin: () -> Unit,
    goToStats: () -> Unit,
    toggleDarkMode: () -> Unit,
) {
    val vm: MainScreenViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val isEbookLoading by vm.isEbookCheckoutLoading
    val isAudioBookLoading by vm.isAudioCheckoutLoading

    var selectedCheckout by remember {
        mutableStateOf<CheckoutBook?>(null)
    }

    LaunchedEffect(selectedCheckout) {
        selectedCheckout?.let {
            setCheckout(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(
                title = "Audio Book",
                selectedTitle = "reset",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    if (selectedCheckout?.type == BookType.Audiobook) {
                        selectedCheckout = null
                    } else {
                        coroutineScope.launch {
                            vm.getCheckout(BookType.Audiobook)?.let {
                                selectedCheckout = it
                            }
                        }
                    }

                },
                isLoading = isAudioBookLoading,
                isSelected = selectedCheckout?.type == BookType.Audiobook
            )

            CustomButton(
                title = "EBook",
                selectedTitle = "reset",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    if (selectedCheckout?.type == BookType.Ebook) {
                        selectedCheckout = null
                    } else {
                        coroutineScope.launch {
                            vm.getCheckout(BookType.Ebook)?.let {
                                selectedCheckout = it
                            }
                        }
                    }
                },
                isLoading = isEbookLoading,
                isSelected = selectedCheckout?.type == BookType.Ebook
            )

            CustomButton(
                title = "To Reader",
                onClick = goToReader,
                enabled = selectedCheckout != null
            )

            CustomButton(
                title = "To Stats",
                onClick = goToStats
            )

            CustomButton(
                title = "Stop Audio",
                onClick = {
                    vm.stopAudio()
                }
            )
            CustomButton(
                title = "Log out",
                color = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary,
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
    selectedTitle: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    selectedColor: Color = MaterialTheme.colorScheme.secondary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSecondary,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val currentTextColor = if (isSelected) selectedTextColor else textColor
    val currentColor = if (isSelected) selectedColor else color
    Surface(
        modifier = Modifier.clickable(
            role = Role.Button,
            enabled = enabled && !isLoading,
            onClick = onClick
        ),
        color = if (enabled || isLoading) {
            currentColor
        } else currentColor.copy(alpha = 0.3f),
    ) {
        Box(
            modifier = Modifier
                .widthIn(200.dp)
                .requiredHeight(48.dp),
        ) {
            if (isLoading) {
                Spinner(
                    modifier = Modifier
                        .align(Alignment.Center),
                    color = currentTextColor
                )
            } else {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    text = if (isSelected) selectedTitle.orEmpty().ifEmpty { title } else title,
                    color = if (enabled) {
                        currentTextColor
                    } else currentTextColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun Spinner(modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(
        modifier = modifier.size(44.dp),
        color = color,
        strokeWidth = 4.dp
    )
}