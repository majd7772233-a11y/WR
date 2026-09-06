package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.DeepSlateBackground
import com.example.ui.theme.WallRushTheme
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.screens.*
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WallRushViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallRushApp(viewModel = viewModel)
        }
    }
}

@Composable
fun WallRushApp(viewModel: WallRushViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    val layoutDirection = if (settings.language == AppLanguage.ARABIC) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        WallRushTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepSlateBackground)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        ScreenState.HOME -> HomeScreen(viewModel = viewModel)
                        ScreenState.MATCH -> MatchScreen(viewModel = viewModel)
                        ScreenState.PUBLIC_ROOMS -> PublicRoomsScreen(viewModel = viewModel)
                        ScreenState.PLAY_FRIEND -> PlayFriendScreen(viewModel = viewModel)
                        ScreenState.TUTORIAL -> TutorialScreen(viewModel = viewModel)
                        ScreenState.REPLAY -> ReplayScreen(viewModel = viewModel)
                        ScreenState.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel)
                        ScreenState.PROFILE -> ProfileScreen(viewModel = viewModel)
                        ScreenState.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        ScreenState.ABOUT -> AboutScreen(viewModel = viewModel)
                    }
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.setShowSettingsDialog(false) }
                    )
                }
            }
        }
    }
}
