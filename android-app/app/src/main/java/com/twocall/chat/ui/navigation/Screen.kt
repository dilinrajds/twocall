package com.twocall.chat.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object PairingWelcome : Screen("pairing_welcome")
    object CreatePair : Screen("create_pair")
    object EnterPairCode : Screen("enter_pair_code")
    object PairingSuccess : Screen("pairing_success")
    object Conversation : Screen("conversation")
    object AudioCall : Screen("audio_call")
    object VideoCall : Screen("video_call")
    object Settings : Screen("settings")
}
