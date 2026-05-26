package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.R
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        // ---- Hero block (top) ---------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(27.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(131.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(80.dp),
                        painter = painterResource(R.drawable.wdb_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WeDoBooks SDK",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Sample app",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
        }

        // ---- Sign-in action (bottom) --------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(horizontal = 8.dp),
                text = "Sign in with a demo token to explore reader, audiobook and " +
                    "sample flows powered by the SDK.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            CustomButton(
                modifier = Modifier.widthIn(max = 360.dp),
                title = "Sign in",
                isLoading = isLoading,
                onClick = {
                    coroutineScope.launch { vm.login() }
                },
            )
        }
    }
}

