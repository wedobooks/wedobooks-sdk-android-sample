package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.os.Bundle
import android.view.ActionMode
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.WDBAudioPlayerScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.DownloadedBooksScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.HeadlessAudioScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.LoginScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.MainScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.StatsScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.theme.WeDoBooksSDKSampleAppTheme
import io.wedobooks.sdk.models.Checkout

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
            var isDarkMode by remember {
                mutableStateOf(isSystemDarkMode)
            }

            WeDoBooksSDKSampleAppTheme(
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
                                goToWDBAudioPlayer = {
                                    mainNavController.navigate(Routes.wdbAudioPlayer)
                                },
                                goToLogin = {
                                    mainNavController.navigate(Routes.login)
                                },
                                goToStats = {
                                    mainNavController.navigate(Routes.stats)
                                },
                                goToDownloadedBooks = {
                                    mainNavController.navigate(Routes.downloadedBooks)
                                },
                                toggleDarkMode = {
                                    isDarkMode = !isDarkMode
                                }
                            )
                        }
                        composable(route = Routes.reader) {
                            // you can set your own coverUrl else set to null if you want to use coverUrl provided by WeDoBooks
                            WeDoBooksSdk.bookOperations.BookScreen(
                                checkout = checkout,
                                cover = null,
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                onFinishClick = {}, // there is a button when you get to the end of the ebook
                                isFinishButtonEnabled = false,
                                onAudioMinimizeClick = null, // different behavior for minimize else defaults to onCloseClick without stopping audio
                                viewModelStoreOwner = null, // if you want to save state outside this composable
                                initialAudioBookProgressMs = null, // used when useInternalProgressService is set to false in WdbConfiguration
                                isDarkMode = isDarkMode,
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
                            WDBAudioPlayerScreen(
                                checkout = checkout,
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.stats) {
                            StatsScreen(
                                checkout = checkout,
                                goBack = {
                                    mainNavController.popBackStack()
                                }
                            )
                        }
                        composable(route = Routes.downloadedBooks) {
                            DownloadedBooksScreen(
                                goBack = {
                                    mainNavController.popBackStack()
                                }
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
    val ctx = LocalContext.current.applicationContext
    // very important to use lastOpenedBookFlow like this or in a ViewModel you don't want it to infinitely recompose
    val easyAccessState by remember { WeDoBooksSdk.easyAccess.lastOpenedBookFlow(ctx) }.collectAsState(
        null
    )
    val currentRoute = navController.currentBackStackEntryAsState()

    currentRoute.value?.destination?.route?.let {
        if (Routes.main == it) {
            easyAccessState?.checkout?.let { checkout ->
                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                onClick = {
                                    onEasyAccessClick(checkout)
                                },
                                role = Role.Button
                            )
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
                            }
                        )

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = checkout.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
