package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.wedobooks.sdk.WeDoBooksSDK
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.LoginScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.MainScreen
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.theme.WeDoBooksSDKSampleAppTheme
import io.wedobooks.sdk.models.CheckoutBook
import io.wedobooks.sdk.models.WeDoBooksConfiguration
import io.wedobooks.sdk.models.WeDoBooksThemeConfiguration
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WeDoBooksSDK.setup(
            context = this.applicationContext,
            config = WeDoBooksConfiguration(
                applicationId = BuildConfig.APPLICATION_ID,
                firebaseApiKey = Constants.SDK_API_KEY,
                firebaseProjectId = Constants.SDK_PROJECT_ID,
                firebaseAppId = Constants.SDK_APP_ID,
                readerApiKey = BuildConfig.READER_API_KEY,
                readerApiSecret = BuildConfig.READER_API_SECRET
            ),
            themeConfig = WeDoBooksThemeConfiguration
                .builder()
                .build()
        )
        setContent {
            val mainNavController = rememberNavController()
            var checkout by remember {
                mutableStateOf<CheckoutBook?>(null)
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
                                goToReader = {
                                    checkout = it
                                    mainNavController.navigate(Routes.reader)
                                },
                                goToLogin = {
                                    mainNavController.navigate(Routes.login)
                                },
                                toggleDarkMode = {
                                    isDarkMode = !isDarkMode
                                }
                            )
                        }
                        composable(route = Routes.reader) {
                            WeDoBooksSDK.bookOperations.getReader(
                                checkout = checkout,
                                coverUrl = null,
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                onFinishClick = {},
                                isFinishButtonEnabled = false,
                                onAudioMinimizeClick = null,
                                viewModelStoreOwner = null,
                                isDarkMode = isDarkMode
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
        WeDoBooksSDK.bookOperations.actionModeStarted(mode)

    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        WeDoBooksSDK.bookOperations.actionModeFinished(mode)
    }
}

@Composable
fun EasyAccess(
    modifier: Modifier = Modifier,
    navController: NavController,
    onEasyAccessClick: (CheckoutBook) -> Unit,
) {
    val ctx = LocalContext.current.applicationContext
    val easyAccessState by remember { WeDoBooksSDK.easyAccess.lastOpenedBookFlow(ctx) }.collectAsState(
        null
    )
    val easyAccessAudioActive by WeDoBooksSDK.easyAccess.isPlayerActive.collectAsState(null to false)
    val easyAccessIsPlayerPlaying by WeDoBooksSDK.easyAccess.isPlayerPlaying.collectAsState(false)
    val currentRoute = navController.currentBackStackEntryAsState()

    currentRoute.value?.destination?.route?.let {
        if (Routes.main == it) {
            easyAccessState?.checkout?.let { checkout ->
                val (audioCheckout, audioIsActive) = easyAccessAudioActive
                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    if (checkout.id == audioCheckout?.id && audioIsActive) {
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
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 24.dp),
                                text = checkout.title,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Icon(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            WeDoBooksSDK.easyAccess.togglePlay()
                                        }
                                    )
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 16.dp),
                                painter = painterResource(
                                    if (easyAccessIsPlayerPlaying) {
                                        io.wedobooks.sdk.R.drawable.ic_pause
                                    } else io.wedobooks.sdk.R.drawable.ic_play
                                ),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
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
}