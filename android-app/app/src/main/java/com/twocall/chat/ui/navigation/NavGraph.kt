package com.twocall.chat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.twocall.chat.ChatApplication
import com.twocall.chat.ui.screens.*
import com.twocall.chat.ui.viewmodel.CallViewModel
import com.twocall.chat.ui.viewmodel.ChatViewModel
import com.twocall.chat.ui.viewmodel.PairingViewModel
import com.twocall.chat.webrtc.CallState

@Composable
fun NavGraph(
    navController: NavHostController,
    pairingViewModel: PairingViewModel,
    chatViewModel: ChatViewModel,
    callViewModel: CallViewModel,
    app: ChatApplication
) {
    val callState by callViewModel.callState.collectAsState()
    val currentSession by callViewModel.currentSession.collectAsState()

    // Global Incoming/Outgoing Call Navigation Observer
    LaunchedEffect(callState, currentSession) {
        if (callState == CallState.INCOMING_RINGING || callState == CallState.OUTGOING_CALLING) {
            currentSession?.pairId?.let { pairId ->
                if (chatViewModel.activePairId.value != pairId) chatViewModel.selectConversation(pairId)
            }
            val isVideo = currentSession?.isVideo == true
            val targetRoute = if (isVideo) Screen.VideoCall.route else Screen.AudioCall.route
            if (navController.currentDestination?.route != targetRoute) {
                navController.navigate(targetRoute)
            }
        }
    }

    LaunchedEffect(Unit) {
        app.pairTerminatedFlow.collect {
            pairingViewModel.reset()
            if (app.keyStoreManager.isPaired()) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.PairingWelcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashLoadingScreen(
                checkPairStatus = { pairingViewModel.checkAndRefreshPairStatus() },
                onNavigateToWelcome = {
                    navController.navigate(Screen.PairingWelcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToConversation = {
                    app.connectWebSocket()
                    if (navController.currentDestination?.route == Screen.Splash.route) navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = chatViewModel,
                onOpenConversation = { pairId ->
                    navController.navigate(Screen.Conversation.route)
                },
                onAddPersonClick = {
                    pairingViewModel.reset()
                    navController.navigate(Screen.PairingWelcome.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.PairingWelcome.route) {
            PairingWelcomeScreen(
                onCreatePairClick = {
                    pairingViewModel.reset()
                    navController.navigate(Screen.CreatePair.route)
                },
                onJoinPartnerClick = {
                    pairingViewModel.reset()
                    navController.navigate(Screen.EnterPairCode.route)
                }
            )
        }

        composable(Screen.CreatePair.route) {
            CreatePairScreen(
                viewModel = pairingViewModel,
                onBackClick = { navController.popBackStack() },
                onPairedSuccess = {
                    app.connectWebSocket()
                    navController.navigate(Screen.Home.route) {
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
                    app.connectWebSocket()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PairingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PairingSuccess.route) {
            PairingSuccessScreen(
                onOpenConversation = {
                    app.connectWebSocket()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PairingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Conversation.route) {
            ConversationScreen(
                viewModel = chatViewModel,
                onBackClick = {
                    val popped = navController.popBackStack(Screen.Home.route, false)
                    if (!popped) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onAudioCallClick = {
                    callViewModel.startAudioCall()
                    navController.navigate(Screen.AudioCall.route)
                },
                onVideoCallClick = {
                    callViewModel.startVideoCall()
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
                onCallEnded = {
                    val popped = navController.popBackStack(Screen.Conversation.route, false)
                    if (!popped) {
                        navController.navigate(Screen.Conversation.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.VideoCall.route) {
            VideoCallScreen(
                viewModel = callViewModel,
                onCallEnded = {
                    val popped = navController.popBackStack(Screen.Conversation.route, false)
                    if (!popped) {
                        navController.navigate(Screen.Conversation.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = chatViewModel,
                app = app,
                onBackClick = { navController.popBackStack() },
                onDisconnected = {
                    pairingViewModel.reset()
                    if (app.keyStoreManager.isPaired()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.PairingWelcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
