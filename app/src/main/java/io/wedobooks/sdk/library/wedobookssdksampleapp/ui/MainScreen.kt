package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.Constants
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.SavedIsbnPicker
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.SettingSwitchRow
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.MainScreenViewModel
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.SdkMode
import io.wedobooks.sdk.models.WdbDownloadStatus
import io.wedobooks.sdk.models.WdbException
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.models.enums.WdbReaderBrightnessMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    setCheckout: (Checkout) -> Unit,
    goToReader: () -> Unit,
    goToHeadlessAudio: () -> Unit,
    goToWdbAudioPlayer: () -> Unit,
    goToLogin: () -> Unit,
    goToDownloadedBooks: () -> Unit,
    goToStorageUsage: () -> Unit,
    goToDevices: () -> Unit,
    goToSampleEbook: (String) -> Unit,
    goToSampleAudiobook: (String) -> Unit,
    goToHeadlessSampleAudio: (String) -> Unit,
    goToWdbSampleAudio: (String) -> Unit,
    toggleDarkMode: () -> Unit,
) {
    val vm: MainScreenViewModel = viewModel()
    val currentUserId by vm.authService.currentUser.collectAsState()
    val tabs = remember {
        listOfNotNull(
            "Checkouts",
            "Stats",
            // Reservations are library-mode only; hide the tab entirely in streaming mode.
            "Reservations".takeIf { Constants.SDK_MODE == SdkMode.Library },
            // Settings always stays last, regardless of which tabs precede it.
            "Settings",
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "WeDoBooks SDK Sample",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppEnvironment.current.label,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = currentUserId ?: "Not signed in",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (tabs[selectedTab]) {
                "Checkouts" -> CheckoutsTab(
                    vm = vm,
                    setCheckout = setCheckout,
                    goToReader = goToReader,
                    goToHeadlessAudio = goToHeadlessAudio,
                    goToWdbAudioPlayer = goToWdbAudioPlayer,
                    goToSampleEbook = goToSampleEbook,
                    goToSampleAudiobook = goToSampleAudiobook,
                    goToHeadlessSampleAudio = goToHeadlessSampleAudio,
                    goToWdbSampleAudio = goToWdbSampleAudio,
                )

                "Stats" -> StatsTab()

                "Reservations" -> ReservationsScreen(
                    savedBooks = vm.books,
                    reservingIsbns = vm.reservationsInFlight.value,
                    onReserveSaved = { isbn -> vm.reserveBook(isbn) },
                    onForgetIsbn = { vm.forgetBook(it) },
                )

                "Settings" -> SettingsTab(
                    vm = vm,
                    goToLogin = goToLogin,
                    goToDownloadedBooks = goToDownloadedBooks,
                    goToStorageUsage = goToStorageUsage,
                    goToDevices = goToDevices,
                    toggleDarkMode = toggleDarkMode,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Checkouts tab
// ---------------------------------------------------------------------------

@Composable
private fun CheckoutsTab(
    vm: MainScreenViewModel,
    setCheckout: (Checkout) -> Unit,
    goToReader: () -> Unit,
    goToHeadlessAudio: () -> Unit,
    goToWdbAudioPlayer: () -> Unit,
    goToSampleEbook: (String) -> Unit,
    goToSampleAudiobook: (String) -> Unit,
    goToHeadlessSampleAudio: (String) -> Unit,
    goToWdbSampleAudio: (String) -> Unit,
) {
    val allCheckouts by remember { WeDoBooksSdk.bookOperations.allCheckoutsFlow() }
        .collectAsState(initial = emptyList())
    val bookDownloads by WeDoBooksSdk.storageOperations.bookDownloadsFlow.collectAsState(
        initial = emptyMap()
    )
    var showLoanSheet by remember { mutableStateOf(false) }
    var selectedMaterialId by remember { mutableStateOf<String?>(null) }

    val checkouts = allCheckouts
        .sortedByDescending { it.active }
        .distinctBy { it.materialId }

    LaunchedEffect(checkouts) { vm.rememberCheckouts(checkouts) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "loan-a-book") {
            CustomButton(
                title = "Loan a book by ISBN",
                onClick = { showLoanSheet = true },
            )
        }

        item(key = "checkouts-header") {
            SectionHeader(title = "Active checkouts (${checkouts.size})")
        }
        if (checkouts.isEmpty()) {
            item(key = "checkouts-empty") {
                Text(
                    text = "Nothing checked out in this environment.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        checkouts.forEach { checkout ->
            item(key = "checkout-${checkout.materialId}") {
                CheckoutCard(
                    checkout = checkout,
                    onClick = { selectedMaterialId = checkout.materialId },
                )
            }
        }
    }

    if (showLoanSheet) {
        LoanBookSheet(vm = vm, onDismiss = { showLoanSheet = false })
    }

    // Looked up by id so the sheet follows checkout updates and closes if the checkout goes away.
    checkouts.firstOrNull { it.materialId == selectedMaterialId }?.let { checkout ->
        val dismiss = { selectedMaterialId = null }
        CheckoutActionsSheet(
            checkout = checkout,
            downloadStatus = bookDownloads[checkout.materialId],
            onDismiss = dismiss,
            onOpen = {
                dismiss()
                setCheckout(checkout)
                goToReader()
            },
            onHeadlessAudio = {
                dismiss()
                setCheckout(checkout)
                goToHeadlessAudio()
            },
            onWdbAudioPlayer = {
                dismiss()
                setCheckout(checkout)
                goToWdbAudioPlayer()
            },
            onToggleDownload = { status -> vm.toggleDownload(checkout, status) },
            onReadSample = {
                dismiss()
                goToSampleEbook(checkout.materialId)
            },
            onPlaySample = {
                dismiss()
                goToSampleAudiobook(checkout.materialId)
            },
            onHeadlessSample = {
                dismiss()
                goToHeadlessSampleAudio(checkout.materialId)
            },
            onWdbSample = {
                dismiss()
                goToWdbSampleAudio(checkout.materialId)
            },
        )
    }
}

@Composable
private fun CheckoutCard(
    checkout: Checkout,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(checkout.type.iconRes()),
                contentDescription = checkout.type.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = checkout.title.ifBlank { checkout.materialId },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The actions for one checkout; `checkout.type` decides which open actions apply. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutActionsSheet(
    checkout: Checkout,
    downloadStatus: WdbDownloadStatus?,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onHeadlessAudio: () -> Unit,
    onWdbAudioPlayer: () -> Unit,
    onToggleDownload: (WdbDownloadStatus?) -> Unit,
    onReadSample: () -> Unit,
    onPlaySample: () -> Unit,
    onHeadlessSample: () -> Unit,
    onWdbSample: () -> Unit,
) {
    // Optimistic flag for the download button: set on click so the label changes
    // immediately, cleared once `bookDownloadsFlow` emits a state different from
    // the one at click time.
    var isTogglePending by remember { mutableStateOf(false) }
    var stateAtClick: WdbDownloadState? by remember { mutableStateOf(null) }
    LaunchedEffect(downloadStatus?.state) {
        if (isTogglePending && downloadStatus?.state != stateAtClick) {
            isTogglePending = false
        }
    }

    val downloadUiState = downloadStatus.toDownloadButtonState(
        hasSelectedCheckout = true,
        isTogglePending = isTogglePending,
    )
    val isAudio = checkout.type.isAudio

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = checkout.title.ifBlank { checkout.materialId },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val authors = checkout.author.joinToString(", ")
                if (authors.isNotBlank()) {
                    Text(
                        text = authors,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = "ISBN ${checkout.materialId} \u00B7 ${checkout.type.name}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            CardSectionHeader(title = "Checkout")
            CustomButton(
                title = if (isAudio) "Play (SDK player)" else "Read (SDK reader)",
                onClick = onOpen,
            )
            if (isAudio) {
                CustomButton(title = "Play (headless \u00B7 custom UI)", onClick = onHeadlessAudio)
                CustomButton(title = "Play (Media3 builder)", onClick = onWdbAudioPlayer)
            }
            CustomButton(
                title = downloadUiState.title,
                enabled = downloadUiState.enabled,
                onClick = {
                    stateAtClick = downloadStatus?.state
                    isTogglePending = true
                    onToggleDownload(downloadStatus)
                },
            )

            // Samples need no checkout.
            CardSectionHeader(title = "Sample")
            CustomButton(title = "Read sample (SDK reader)", onClick = onReadSample)
            CustomButton(title = "Play sample (SDK player)", onClick = onPlaySample)
            CustomButton(title = "Play sample (headless \u00B7 custom UI)", onClick = onHeadlessSample)
            CustomButton(title = "Play sample (Media3 builder \u00B7 custom UI)", onClick = onWdbSample)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanBookSheet(
    vm: MainScreenViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val inFlight by vm.checkoutsInFlight
    var isbn by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    fun loan(target: String) {
        coroutineScope.launch { result = vm.loanBook(target).describe(target) }
    }

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
                text = "Loan a book",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Checks the ISBN out straight away. It is only added to " +
                    "your list if the loan succeeds, and it is remembered by title.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
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
                title = "Loan",
                isLoading = inFlight.contains(isbn.trim()),
                enabled = isbn.isNotBlank(),
                onClick = { loan(isbn.trim()) },
            )
            SavedIsbnPicker(
                books = vm.books,
                actionLabel = "LOAN",
                busyIsbns = inFlight,
                onAction = ::loan,
                onForget = { vm.forgetBook(it) },
            )
        }
    }
}

@Composable
private fun CardSectionHeader(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

// ---------------------------------------------------------------------------
// Stats tab
// ---------------------------------------------------------------------------

@Composable
private fun StatsTab() {
    val allCheckouts by remember { WeDoBooksSdk.bookOperations.allCheckoutsFlow() }
        .collectAsState(initial = emptyList())
    StatsScreen(
        checkouts = allCheckouts
            .sortedByDescending { it.active }
            .distinctBy { it.materialId },
    )
}

// ---------------------------------------------------------------------------
// Settings tab
// ---------------------------------------------------------------------------

@Composable
private fun SettingsTab(
    vm: MainScreenViewModel,
    goToLogin: () -> Unit,
    goToDownloadedBooks: () -> Unit,
    goToStorageUsage: () -> Unit,
    goToDevices: () -> Unit,
    toggleDarkMode: () -> Unit,
) {
    fun garbageCollection() {
        System.gc()
        System.runFinalization()
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    val bookScreenSettings = vm.bookScreenSettings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Storage — usage breakdown, downloaded books + bulk clear.
        section("Storage") {
            item {
                CustomButton(
                    title = "Storage usage",
                    onClick = goToStorageUsage,
                )
            }
            item {
                CustomButton(
                    title = "Downloaded books",
                    onClick = goToDownloadedBooks,
                )
            }
            item {
                CustomButton(
                    title = "Clear all downloads",
                    onClick = { vm.removeStorage() },
                )
            }
            item {
                CustomButton(
                    title = "Refresh downloads",
                    onClick = { vm.refreshDownloads() },
                )
            }
        }

        // Devices — list and manage SDK device sessions.
        section("Devices") {
            item {
                CustomButton(
                    title = "Manage devices",
                    onClick = goToDevices,
                )
            }
        }

        // Playback — runtime audio control independent of the active screen.
        section("Playback") {
            item {
                CustomButton(
                    title = "Stop audio playback",
                    onClick = { vm.stopAudio() },
                )
            }
        }

        // Book screen — set{Player,Reader}Configuration, applied to the next book opened.
        section("Book screen") {
            item {
                SettingSwitchRow(
                    title = "Player minimize button",
                    checked = bookScreenSettings.showPlayerMinimizeButton,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(showPlayerMinimizeButton = it))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Player finish button",
                    checked = bookScreenSettings.showPlayerFinishButton,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(showPlayerFinishButton = it))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Reader pull-down top",
                    checked = bookScreenSettings.readerPullDownTop,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(readerPullDownTop = it))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Reader header animation",
                    checked = bookScreenSettings.readerShowHeaderAnimation,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(readerShowHeaderAnimation = it))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Reader finish button",
                    checked = bookScreenSettings.showReaderFinishButton,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(showReaderFinishButton = it))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Reader brightness changes device setting",
                    checked = bookScreenSettings.readerBrightnessMode == WdbReaderBrightnessMode.System,
                    onCheckedChange = {
                        val mode =
                            if (it) WdbReaderBrightnessMode.System else WdbReaderBrightnessMode.Window
                        vm.updateBookScreenSettings(bookScreenSettings.copy(readerBrightnessMode = mode))
                    },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Remember reader brightness",
                    checked = bookScreenSettings.rememberReaderBrightness,
                    onCheckedChange = {
                        vm.updateBookScreenSettings(bookScreenSettings.copy(rememberReaderBrightness = it))
                    },
                )
            }
        }

        // Appearance — visual settings.
        section("Appearance") {
            item {
                CustomButton(
                    title = "Toggle dark mode",
                    onClick = toggleDarkMode,
                )
            }
        }

        // Debug — internal diagnostics; safe to ignore in normal use.
        section("Debug") {
            item {
                CustomButton(
                    title = "Force garbage collection",
                    onClick = ::garbageCollection,
                )
            }
        }

        // History — add a material to reading history by ISBN (opens a sheet).
        section("History") {
            item {
                CustomButton(
                    title = "Add to history",
                    onClick = { showHistorySheet = true },
                )
            }
        }

        // Account — destructive action lives at the bottom by convention.
        section("Account") {
            item {
                CustomButton(
                    title = "Sign out",
                    color = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                    onClick = {
                        goToLogin()
                        vm.logout()
                    },
                )
            }
        }
    }

    if (showHistorySheet) {
        AddToHistorySheet(
            vm = vm,
            onDismiss = { showHistorySheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToHistorySheet(
    vm: MainScreenViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isbn by remember { mutableStateOf("") }
    val isAdding by vm.isAddingToHistory

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add to history",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Adds a material to your reading history as a completed entry.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text("ISBN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CustomButton(
                title = "Add to history",
                isLoading = isAdding,
                enabled = isbn.isNotBlank(),
                onClick = {
                    val materialId = isbn.trim()
                    coroutineScope.launch {
                        val error = vm.addToHistory(materialId)
                        if (error == null) {
                            isbn = ""
                            Toast.makeText(context, "Added $materialId to history", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }
    }
}

private fun LazyListScope.section(
    title: String,
    content: LazyListScope.() -> Unit,
) {
    item { SectionHeader(title = title) }
    content()
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

// ---------------------------------------------------------------------------
// Shared UI: download button state + CustomButton + Spinner
// ---------------------------------------------------------------------------

private data class DownloadButtonState(
    val title: String,
    val enabled: Boolean,
)

private fun WdbDownloadStatus?.toDownloadButtonState(
    hasSelectedCheckout: Boolean,
    isTogglePending: Boolean = false,
): DownloadButtonState {
    if (!hasSelectedCheckout) {
        return DownloadButtonState(
            title = "Download",
            enabled = false,
        )
    }
    if (isTogglePending) {
        // Optimistic feedback while we wait for `bookDownloadsFlow` to emit
        // the new state — pick a label that matches the direction the user
        // just chose so they get immediate, accurate feedback.
        val title = when (this?.state) {
            WdbDownloadState.Paused,
            WdbDownloadState.Cancelled,
            WdbDownloadState.Finished,
            WdbDownloadState.Error,
            -> "Removing…"

            else -> "Starting download…"
        }
        return DownloadButtonState(title = title, enabled = false)
    }
    return when (this?.state) {
        WdbDownloadState.Downloading,
        WdbDownloadState.Waiting,
        -> DownloadButtonState(
            title = buildString {
                append("Downloading ${(progress * 100).toInt().coerceIn(0, 100)}%")
                formatDownloadBytes(downloadedBytes, totalBytes)?.let { append(" ($it)") }
            },
            enabled = false,
        )

        WdbDownloadState.Paused,
        WdbDownloadState.Cancelled,
        WdbDownloadState.Finished,
        -> DownloadButtonState(
            title = "Remove download",
            enabled = true,
        )

        WdbDownloadState.Error -> DownloadButtonState(
            title = if (this?.error is WdbException.InsufficientStorage) {
                "Not enough storage — retry"
            } else {
                "Download error — retry"
            },
            enabled = true,
        )

        else -> DownloadButtonState(
            title = "Download",
            enabled = true,
        )
    }
}

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    title: String,
    selectedTitle: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    selectedColor: Color = MaterialTheme.colorScheme.secondary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSecondary,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val currentTextColor = if (isSelected) selectedTextColor else textColor
    val currentColor = if (isSelected) selectedColor else color
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                enabled = enabled && !isLoading,
                onClick = onClick,
            ),
        color = if (enabled || isLoading) {
            currentColor
        } else currentColor.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(48.dp),
        ) {
            if (isLoading) {
                Spinner(
                    modifier = Modifier.align(Alignment.Center),
                    color = currentTextColor,
                )
            } else {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp),
                    text = if (isSelected) selectedTitle.orEmpty().ifEmpty { title } else title,
                    color = if (enabled) {
                        currentTextColor
                    } else currentTextColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
fun Spinner(modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(
        modifier = modifier.size(28.dp),
        color = color,
        strokeWidth = 3.dp,
    )
}
