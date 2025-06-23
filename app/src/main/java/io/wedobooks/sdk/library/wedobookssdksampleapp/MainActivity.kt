package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
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
import io.wedobooks.sdk.models.ICheckoutBook
import io.wedobooks.sdk.models.WeDoBooksConfiguration
import io.wedobooks.sdk.models.WeDoBooksThemeColors
import io.wedobooks.sdk.models.WeDoBooksThemeConfiguration

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
                readerApiKey = BuildConfig.COLIBRIO_API_KEY,
                readerApiSecret = BuildConfig.COLIBRIO_API_SECRET
            ),
            themeConfig = WeDoBooksThemeConfiguration
                .builder()
                .build()
        )
        setContent {
            val mainNavController = rememberNavController()
            var checkout by remember {
                mutableStateOf<ICheckoutBook?>(null)
            }
            val profile by WeDoBooksSDK.user.profile.collectAsState(null)
            val isSystemDarkMode = isSystemInDarkTheme()
            var isDarkMode by remember {
                mutableStateOf(isSystemDarkMode)
            }
            MaterialTheme(
                colorScheme = if (isDarkMode) {
                    WeDoBooksThemeColors.DefaultDark.getColorsScheme()
                } else WeDoBooksThemeColors.DefaultLight.getColorsScheme()
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
                            WeDoBooksSDK.books.getReader(
                                checkout = checkout,
                                coverUrl = "https://images.qa.pubhub.dk/01/f5c95358-cad1-4edf-b14a-2d7a713195f0.jpg?457441",
                                onCloseClick = {
                                    mainNavController.popBackStack()
                                },
                                onFinishClick = {},
                                isFinishButtonEnabled = false,
                                onAudioMinimizeClick = null,
                                viewModelStoreOwner = null,
                                forceDarkMode = isDarkMode
                            )
                        }
                    }

                    profile?.let {
                        EasyAccess(
                            modifier = Modifier
                                .systemBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(60.dp)
                                .align(Alignment.BottomCenter),
                            navController = mainNavController,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        WeDoBooksSDK.books.actionModeStarted(mode)

    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        WeDoBooksSDK.books.actionModeFinished(mode)
    }
}

@Composable
fun EasyAccess(
    modifier: Modifier = Modifier,
    navController: NavController,
    isDarkMode: Boolean,
) {
    val ctx = LocalContext.current.applicationContext
    val easyAccessState by WeDoBooksSDK.easyAccess.lastOpenedBookFlow(ctx).collectAsState(null)
    val currentRoute = navController.currentBackStackEntryAsState()

    currentRoute.value?.destination?.route?.let {
        if (Routes.main == it) {
            easyAccessState?.checkout?.let { checkout ->
                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = if (isDarkMode) 8.dp else 0.dp
                ) {
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
                            text = checkout.title + "(${checkout.type.type})",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WeDoBooksSDKSampleAppTheme {
        Greeting("Android")
    }
}