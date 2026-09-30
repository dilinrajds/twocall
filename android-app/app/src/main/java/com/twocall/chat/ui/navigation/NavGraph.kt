package com.twocall.chat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.twocall.chat.ChatApplication
import com.twocall.chat.ui.screens.*
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.ui.viewmodel.ChatViewModel
import com.twocall.chat.ui.viewmodel.PairingViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    pairingViewModel: PairingViewModel,
    chatViewModel: ChatViewModel,
    callViewModel: CallViewModel,
    app: ChatApplication
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashLoadingScreen(
                isPaired = pairingViewModel.isAlreadyPaired(),
                onNavigateToWelcome = {
                    navController.navigate(Screen.PairingWelcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToConversation = {
                    navController.navigate(Screen.Conversation.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PairingWelcome.route) {
            PairingWelcomeScreen(
                onCreatePairClick = { navController.navigate(Screen.CreatePair.route) },
                onJoinPartnerClick = { navController.navigate(Screen.EnterPairCode.route) }
            )
        }

        composable(Screen.CreatePair.route) {
            CreatePairScreen(
                viewModel = pairingViewModel,
                onBackClick = { navController.popBackStack() },
                onPairedSuccess = {
                    navController.navigate(Screen.PairingSuccess.route) {
                        popUpTo(Screen.PairingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EnterPairCode.route) {
            EnterPairCodeScreen(
                viewModel = pairingViewModel,
                onBackClick = { navController.popBackStack() },
                onPairedSuccess = {
                    navController.navigate(Screen.PairingSuccess.route) {
                        popUpTo(Screen.PairingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PairingSuccess.route) {
            PairingSuccessScreen(
                onOpenConversation = {
                    navController.navigate(Screen.Conversation.route) {
                        popUpTo(Screen.PairingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Conversation.route) {
            ConversationScreen(
                viewModel = chatViewModel,
                onAudioCallClick = {
                    callViewModel.startAudioCall()
                    navController.navigate(Screen.AudioCall.route)
                },
                onVideoCallClick = {
                    navController.navigate(Screen.VideoCall.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.AudioCall.route) {
            AudioCallScreen(
                viewModel = callViewModel,
                onCallEnded = { navController.popBackStack() }
            )
        }

        composable(Screen.VideoCall.route) {
            VideoCallScreen(
                viewModel = callViewModel,
                onCallEnded = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = chatViewModel,
                app = app,
                onBackClick = { navController.popBackStack() },
                onDisconnected = {
                    navController.navigate(Screen.PairingWelcome.route) {
                        popUpTo(Screen.Conversation.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
