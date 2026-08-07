package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.Constants
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.MainScreenViewModel
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.SdkMode
import io.wedobooks.sdk.models.WdbDownloadStatus
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import kotlinx.coroutines.launch

private data class TestMaterial(
    val isbn: String,
    val type: MaterialType,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    setCheckout: (Checkout) -> Unit,
    goToReader: () -> Unit,
    goToHeadlessAudio: () -> Unit,
    goToWdbAudioPlayer: () -> Unit,
    goToLogin: () -> Unit,
    goToDownloadedBooks: () -> Unit,
    goToDevices: () -> Unit,
    goToSampleEbook: () -> Unit,
    goToSampleAudiobook: () -> Unit,
    goToHeadlessSampleAudio: () -> Unit,
    goToWdbSampleAudio: () -> Unit,
    toggleDarkMode: () -> Unit,
) {
    val vm: MainScreenViewModel = viewModel()
    val testMaterials = remember {
        listOf(
            TestMaterial(
                isbn = Constants.E_BOOK,
                type = MaterialType.Ebook,
                label = "Ebook",
            ),
            TestMaterial(
                isbn = Constants.AUDIO_BOOK,
                type = MaterialType.Audiobook,
                label = "Audiobook",
            ),
        )
    }

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
                    testMaterials = testMaterials,
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

                "Reservations" -> ReservationsScreen()

                "Settings" -> SettingsTab(
                    vm = vm,
                    goToLogin = goToLogin,
                    goToDownloadedBooks = goToDownloadedBooks,
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
    testMaterials: List<TestMaterial>,
    setCheckout: (Checkout) -> Unit,
    goToReader: () -> Unit,
    goToHeadlessAudio: () -> Unit,
    goToWdbAudioPlayer: () -> Unit,
    goToSampleEbook: () -> Unit,
    goToSampleAudiobook: () -> Unit,
    goToHeadlessSampleAudio: () -> Unit,
    goToWdbSampleAudio: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val allCheckouts by remember { WeDoBooksSdk.bookOperations.allCheckoutsFlow() }
        .collectAsState(initial = emptyList())
    val bookDownloads by WeDoBooksSdk.storageOperations.bookDownloadsFlow.collectAsState(
        initial = emptyMap()
    )

    val isEbookLoading by vm.isEbookCheckoutLoading
    val isAudioBookLoading by vm.isAudioCheckoutLoading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        testMaterials.forEach { material ->
            item(key = "material-${material.isbn}") {
                val checkout = allCheckouts.firstOrNull { it.materialId == material.isbn }
                val isLoading = when (material.type) {
                    MaterialType.Audiobook -> isAudioBookLoading
                    MaterialType.Ebook -> isEbookLoading
                }
                val downloadStatus = bookDownloads[material.isbn]

                // Optimistic flag for the download button. Flipped to true the
                // moment the user clicks Download / Remove so the UI changes
                // immediately, instead of waiting the ~hundreds of ms it takes
                // for `bookDownloadsFlow` to emit the new state.
                //
                // We snapshot the pre-click state and clear the flag as soon
                // as the real state diverges, handing control back to the
                // SDK-driven status.
                var isTogglePending by remember(material.isbn) { mutableStateOf(false) }
                var stateAtClick: WdbDownloadState? by remember(material.isbn) { mutableStateOf(null) }
                LaunchedEffect(downloadStatus?.state) {
                    if (isTogglePending && downloadStatus?.state != stateAtClick) {
                        isTogglePending = false
                    }
                }

                MaterialCard(
                    material = material,
                    checkout = checkout,
                    isLoading = isLoading,
                    downloadStatus = downloadStatus,
                    isTogglePending = isTogglePending,
                    onRequestCheckout = {
                        coroutineScope.launch {
                            vm.getCheckout(
                                isbn = material.isbn,
                                materialType = material.type
                            )
                        }
                    },
                    onOpen = {
                        checkout?.let {
                            setCheckout(it)
                            goToReader()
                        }
                    },
                    onHeadlessAudio = {
                        checkout?.let {
                            setCheckout(it)
                            goToHeadlessAudio()
                        }
                    },
                    onWdbAudioPlayer = {
                        checkout?.let {
                            setCheckout(it)
                            goToWdbAudioPlayer()
                        }
                    },
                    onOpenSample = when (material.type) {
                        MaterialType.Ebook -> goToSampleEbook
                        MaterialType.Audiobook -> goToSampleAudiobook
                    },
                    onHeadlessSample = if (material.type == MaterialType.Audiobook) {
                        goToHeadlessSampleAudio
                    } else null,
                    onWdbSample = if (material.type == MaterialType.Audiobook) {
                        goToWdbSampleAudio
                    } else null,
                    onToggleDownload = {
                        checkout?.let {
                            stateAtClick = downloadStatus?.state
                            isTogglePending = true
                            vm.toggleDownload(it, downloadStatus)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: TestMaterial,
    checkout: Checkout?,
    isLoading: Boolean,
    downloadStatus: WdbDownloadStatus?,
    isTogglePending: Boolean,
    onRequestCheckout: () -> Unit,
    onOpen: () -> Unit,
    onHeadlessAudio: () -> Unit,
    onWdbAudioPlayer: () -> Unit,
    onOpenSample: () -> Unit,
    /** Optional headless-sample button (only audiobooks expose this in the SDK). */
    onHeadlessSample: (() -> Unit)?,
    /** Optional WdbAudioPlayer-sample button with custom UI (audiobooks only). */
    onWdbSample: (() -> Unit)?,
    onToggleDownload: () -> Unit,
) {
    val downloadUiState = downloadStatus.toDownloadButtonState(
        hasSelectedCheckout = checkout != null,
        isTogglePending = isTogglePending,
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // -- Header --------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = material.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "ISBN ${material.isbn}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            // -- Status -------------------------------------------------------
            if (checkout != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = checkout.title.ifBlank { "Untitled" },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val authors = checkout.author.joinToString(", ")
                    if (authors.isNotBlank()) {
                        Text(
                            text = authors,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                Text(
                    text = "Not checked out yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // -- Checkout actions ---------------------------------------------
            CardSectionHeader(title = "Checkout")
            if (checkout == null) {
                CustomButton(
                    title = "Request checkout",
                    isLoading = isLoading,
                    onClick = onRequestCheckout,
                )
            } else {
                val openLabel = when (material.type) {
                    MaterialType.Audiobook -> "Play (SDK player)"
                    MaterialType.Ebook -> "Read (SDK reader)"
                }
                CustomButton(title = openLabel, onClick = onOpen)
                if (material.type == MaterialType.Audiobook) {
                    CustomButton(title = "Play (headless · custom UI)", onClick = onHeadlessAudio)
                    CustomButton(title = "Play (Media3 builder)", onClick = onWdbAudioPlayer)
                }
                CustomButton(
                    title = downloadUiState.title,
                    enabled = downloadUiState.enabled,
                    onClick = onToggleDownload,
                )
            }

            // -- Sample actions (no checkout needed) ---------------------------
            CardSectionHeader(title = "Sample")
            val sampleLabel = when (material.type) {
                MaterialType.Audiobook -> "Play sample (SDK player)"
                MaterialType.Ebook -> "Read sample (SDK reader)"
            }
            CustomButton(title = sampleLabel, onClick = onOpenSample)
            if (onHeadlessSample != null) {
                CustomButton(
                    title = "Play sample (headless · custom UI)",
                    onClick = onHeadlessSample,
                )
            }
            if (onWdbSample != null) {
                CustomButton(
                    title = "Play sample (Media3 builder · custom UI)",
                    onClick = onWdbSample,
                )
            }
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
    // StatsScreen now pages through one stats pane per checkout (after the
    // year pane), so all active checkouts are shown.
    StatsScreen(checkouts = allCheckouts)
}

// ---------------------------------------------------------------------------
// Settings tab
// ---------------------------------------------------------------------------

@Composable
private fun SettingsTab(
    vm: MainScreenViewModel,
    goToLogin: () -> Unit,
    goToDownloadedBooks: () -> Unit,
    goToDevices: () -> Unit,
    toggleDarkMode: () -> Unit,
) {
    fun garbageCollection() {
        System.gc()
        System.runFinalization()
    }

    var showHistorySheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Storage — downloaded books + bulk clear.
        section("Storage") {
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
            title = "Downloading ${(progress * 100).toInt().coerceIn(0, 100)}%",
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
            title = "Download error — retry",
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
