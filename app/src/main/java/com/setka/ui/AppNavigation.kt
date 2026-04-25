package com.setka.ui

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.setka.viewmodel.ChatViewModel

@Composable
fun AppNavigation(vm: ChatViewModel, startChatId: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startChatId) {
        if (!startChatId.isNullOrBlank()) navController.navigate("chat/$startChatId")
    }

    NavHost(navController = navController, startDestination = "chats") {
        composable("chats") {
            ChatsScreen(
                vm             = vm,
                onOpenChat     = { navController.navigate("chat/$it") },
                onOpenNearby   = { navController.navigate("nearby") },
                onOpenContacts = { navController.navigate("contacts") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenMap      = { navController.navigate("map") }
            )
        }
        composable(
            "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            val chatId = back.arguments?.getString("chatId") ?: return@composable
            ChatScreen(vm = vm, chatId = chatId, onBack = { navController.popBackStack() })
        }
        composable("contacts") {
            ContactsScreen(
                vm         = vm,
                onBack     = { navController.popBackStack() },
                onOpenChat = { chatId ->
                    navController.navigate("chat/$chatId") {
                        popUpTo("contacts") { inclusive = true }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                vm            = vm,
                onBack        = { navController.popBackStack() },
                onOpenPrivacy = { navController.navigate("privacy") }
            )
        }
        composable("nearby") {
            NearbyScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable("privacy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable("map") {
            NetworkMapScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
