package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    goToMainScreen: () -> Unit,
) {
    val vm: LoginViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val isLoggedIn by vm.isLoggedIn.collectAsState(false)
    val isLoading by vm.isLoading

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            goToMainScreen()
        }
    }

    Box {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp).align(Alignment.Center),
                color = Color.Blue,
                strokeWidth = 4.dp
            )
        } else {
            Column(
                modifier = Modifier
                    .align(
                        Alignment.Center
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomButton(
                    title = "log in",
                    onClick = {
                        coroutineScope.launch {
                            vm.login("tF6EmXY0VEaKi4gMCILpZitZjYu2")
                        }
                    },
                    color = Color.Black
                )
            }
        }
    }
}