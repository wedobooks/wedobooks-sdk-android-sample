package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.wedobooks.sdk.R
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.DownloadedBooksScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.DevicesScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.HeadlessAudioSampleScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.HeadlessAudioScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.LoginScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.MainScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.StatsScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.WdbAudioPlayerSampleScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.WdbAudioPlayerScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.theme.WeDoBooksSdkTheme
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val TAG = "SampleMainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainNavController = rememberNavController()
            var checkout by remember {
                mutableStateOf<Checkout?>(null)
            }
            val isSystemDarkMode = isSystemInDarkTheme()
            // Treat the presence of a signed-in user id as the authoritative
            // "is authenticated" signal for the sample app. Anything below that
            // touches SDK surfaces requiring permissions/auth must be guarded
            // by this flag so it doesn't fire pre-login or after sign-out.
            val currentUserId by AuthService.instance.currentUser.collectAsState()
            val isAuthenticated = currentUserId != null
            // Only attach the reader's bookLoadedFlow listener once we have a
            // signed-in user — pre-auth the reader has nothing meaningful to
            // report, and we don't want to risk subscribing before SDK
            // initialisation is complete.
            val isBookLoaded by remember(isAuthenticated) {
                if (isAuthenticated) {
                    WeDoBooksSdk.reader.bookLoadedFlow
                } else {
                    flowOf(false)
                }
            }.collectAsState(false)
            var isDarkMode by remember {
                mutableStateOf(isSystemDarkMode)
            }

            LaunchedEffect(isBookLoaded, isAuthenticated) {
                /*
                   If used for initial progress, please set internalProgressConfig.reader = false
                   else the internal progress and your progress might be in a race condition,
                   with the later overwriting the first.

                   Also please only use it like this if you intend on disabling screen rotation else it will use goTo again on rotation.
                   Propper usage would be through a ViewModel that only calls it once
                */
                if (isAuthenticated && isBookLoaded) {
                    runCatching {
                        WeDoBooksSdk.reader.percentageToCfi(0.5, 9).let {
                            WeDoBooksSdk.reader.goTo(it)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                WeDoBooksSdk.events.sessionInterruptionEvents().collect { event ->
                    Log.d(TAG, "sessionInterrupted= $event")
                }
            }

            WeDoBooksSdkTheme(
                darkTheme = isDarkMode
            ) {
                Box {
                    NavHost(
                        modifier = Modifier.fillMaxSize(),
                        navController = mainNavController,
                        startDestination = Routes.login
                    ) {
                        composable(route = Routes.login) {
                            LoginScreen(
                                goToMainScreen = {
                                    mainNavController.navigate(Routes.main)
                                }
                            )
                        }
                        composable(route = Routes.main) {
                            MainScreen(
                                setCheckout = {
                                    checkout = it
                                },
                                goToReader = {
                                    mainNavController.navigate(Routes.reader)
                                },
                                goToHeadlessAudio = {
                                    mainNavController.navigate(Routes.headlessAudio)
                                },
                                goToWdbAudioPlayer = {
                                    mainNavController.navigate(Routes.wdbAudioPlayer)
                                },
                                goToLogin = {
                                    mainNavController.navigate(Routes.login)
                                },
                                goToDownloadedBooks ={
                                    mainNavController.navigate(Routes.downloadedBooks)
                                },
                                goToDevices = {
                                    mainNavController.navigate(Routes.devices)
                                },
                                goToSampleEbook = {
                                    mainNavController.navigate(Routes.sampleEbook)
                                },
                                goToSampleAudiobook = {
                                    mainNavController.navigate(Routes.sampleAudiobook)
                                },
                                goToHeadlessSampleAudio = {
                                    mainNavController.navigate(Routes.headlessSampleAudio)
                                },
                                goToWdbSampleAudio = {
                                    mainNavController.navigate(Routes.wdbSampleAudio)
                                },
                                toggleDarkMode = {
                                    isDarkMode = !isDarkMode
                                }
                            )
                        }
                        composable(route = Routes.reader) {
                            val sessionProgress by remember(checkout) {
                                if (checkout?.type == MaterialType.Ebook) {
                                    WeDoBooksSdk.reader.sessionProgressFlow
                                } else flowOf(null)
                            }.collectAsState(null)
                            LaunchedEffect(sessionProgress) {
                                Log.d("Progress", "$sessionProgress")
                            }
                            val context = LocalContext.current
                            WeDoBooksSdk.bookOperations.BookScreen(
                                checkout = checkout,
                                cover = null,
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                onFinishClick = { finishedCheckout ->
                                    Log.d(
                                        TAG,
                                        "Finish button clicked for checkoutId=${finishedCheckout.id}"
                                    )
                                },
                                isFinishButtonEnabled = true,
                                isMinimizeButtonEnabled = true,
                                onAudioMinimizeClick = null, // different behavior for minimize else defaults to onCloseClick without stopping audio
                                initialAudioBookProgressMs = 32000, // only used if internalProgressConfig.player = false
                                initialReaderCfi = null, // only used if internalProgressConfig.reader = false
                                viewModelStoreOwner = null, // if you want to save state outside this composable
                                isDarkMode = isDarkMode,
                                onError = { error ->
                                    Log.e(TAG, "BookScreen failed to load", error)
                                    Toast.makeText(
                                        context,
                                        error.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            )
                        }
                        composable(route = Routes.headlessAudio) {
                            HeadlessAudioScreen(
                                checkout = checkout,
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.wdbAudioPlayer) {
                            WdbAudioPlayerScreen(
                                checkout = checkout,
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.stats) {
                            StatsScreen(
                                checkouts = listOfNotNull(checkout),
                            )
                        }
                        composable(route = Routes.downloadedBooks) {
                            DownloadedBooksScreen(
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.devices) {
                            DevicesScreen(
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.sampleEbook) {
                            WeDoBooksSdk.bookOperations.SampleBookScreen(
                                isbn = Constants.E_BOOK,
                                materialType = MaterialType.Ebook,
                                cover = null,
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                isDarkMode = isDarkMode,
                                metadata = null,
                            )
                        }
                        composable(route = Routes.sampleAudiobook) {
                            WeDoBooksSdk.bookOperations.SampleBookScreen(
                                isbn = Constants.AUDIO_BOOK,
                                materialType = MaterialType.Audiobook,
                                cover = null,
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                isDarkMode = isDarkMode,
                                metadata = null,
                            )
                        }
                        composable(route = Routes.headlessSampleAudio) {
                            HeadlessAudioSampleScreen(
                                isbn = Constants.AUDIO_BOOK,
                                goBack = { mainNavController.popBackStack() },
                            )
                        }
                        composable(route = Routes.wdbSampleAudio) {
                            WdbAudioPlayerSampleScreen(
                                isbn = Constants.AUDIO_BOOK,
                                goBack = { mainNavController.popBackStack() },
                            )
                        }
                    }

                    EasyAccess(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                            .height(60.dp)
                            .align(Alignment.BottomCenter),
                        navController = mainNavController,
                        onEasyAccessClick = {
                            checkout = it
                            mainNavController.navigate(Routes.reader)
                        }
                    )
                }
            }
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        WeDoBooksSdk.bookOperations.actionModeStarted(mode)

    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        WeDoBooksSdk.bookOperations.actionModeFinished(mode)
    }
}

@Composable
fun EasyAccess(
    modifier: Modifier = Modifier,
    navController: NavController,
    onEasyAccessClick: (Checkout) -> Unit,
) {
    // EasyAccess is mounted as an overlay outside the NavHost, so it gets
    // composed at the same time as the login screen. Gate the SDK subscription
    // on auth state so we don't open a `lastOpenedBookFlow` listener before
    // the user signs in (or after sign-out).
    val currentUserId by AuthService.instance.currentUser.collectAsState()
    if (currentUserId == null) return

    val ctx = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // very important to use lastOpenedBookFlow like this or in a ViewModel you don't want it to infinitely recompose
    val easyAccessState by remember { WeDoBooksSdk.easyAccess.lastOpenedBookFlow(ctx) }.collectAsState(
        null
    )
    val currentRoute = navController.currentBackStackEntryAsState()

    currentRoute.value?.destination?.route?.let {
        if (Routes.main == it) {
            easyAccessState?.checkout?.let { checkout ->
                // Local dismissed flag — flipped synchronously the moment the
                // user completes a swipe so the entire 60-dp surface
                // collapses immediately, instead of leaving an empty slot
                // until the async `removeEasyAccess` clears the SDK state.
                // Keyed on `checkout.id` so the next book's easy-access card
                // doesn't start out hidden.
                var locallyDismissed by remember(checkout.id) { mutableStateOf(false) }
                if (locallyDismissed) return@let

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        val swiped = target == SwipeToDismissBoxValue.StartToEnd ||
                            target == SwipeToDismissBoxValue.EndToStart
                        if (swiped) {
                            locallyDismissed = true
                            scope.launch {
                                WeDoBooksSdk.easyAccess.removeEasyAccess(ctx)
                            }
                        }
                        swiped
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = modifier,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = { /* empty — let the underlying nav host show through */ },
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 8.dp,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    onClick = {
                                        onEasyAccessClick(checkout)
                                    },
                                    role = Role.Button,
                                ),
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .height(4.dp)
                                    .fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.outline,
                                color = MaterialTheme.colorScheme.primary,
                                progress = {
                                    easyAccessState?.progress?.toFloat() ?: 0f
                                },
                            )

                            // Reserve symmetric horizontal space so a long title
                            // stays centered without sliding under the close icon.
                            Text(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 48.dp),
                                text = checkout.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(40.dp),
                                onClick = {
                                    locallyDismissed = true
                                    scope.launch {
                                        WeDoBooksSdk.easyAccess.removeEasyAccess(ctx)
                                    }
                                },
                            ) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    painter = painterResource(R.drawable.wdb_ic_close),
                                    contentDescription = "Dismiss easy access",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
