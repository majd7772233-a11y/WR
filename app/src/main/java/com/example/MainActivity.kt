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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Player1Primary
import com.example.ui.theme.TextSecondary
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
                        ScreenState.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                    }
                }

                val inAppNotification by viewModel.inAppNotification.collectAsState()

                AnimatedVisibility(
                    visible = inAppNotification != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                ) {
                    inAppNotification?.let { notif ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.5.dp, Player1Primary),
                            shadowElevation = 14.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                                .clickable { viewModel.dismissInAppNotification() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = notif.icon, fontSize = 24.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = notif.message,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
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
