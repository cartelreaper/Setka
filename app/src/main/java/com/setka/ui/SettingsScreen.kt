package com.setka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setka.ui.components.AvatarCircle
import com.setka.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val myUser  by vm.myUser.collectAsState()
    var nickname by remember(myUser) { mutableStateOf(myUser?.nickname ?: "") }
    var saved    by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = { Text("Настройки", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Аватар
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                AvatarCircle(
                    name  = nickname.ifBlank { "?" },
                    color = myUser?.avatarColor ?: 0,
                    size  = 80.dp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Никнейм
            OutlinedTextField(
                value         = nickname,
                onValueChange = { nickname = it; saved = false },
                label         = { Text("Никнейм") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Person, null) }
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (nickname.isNotBlank()) {
                        vm.updateNickname(nickname.trim())
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = nickname.isNotBlank() && nickname.trim() != myUser?.nickname
            ) { Text("Сохранить") }

            if (saved) {
                Spacer(Modifier.height(4.dp))
                Text("Сохранено ✓",
                    color    = MaterialTheme.colorScheme.secondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(Modifier.height(28.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            Spacer(Modifier.height(8.dp))

            // Информация
            SettingsGroup(title = "О приложении") {
                SettingsInfoRow("Версия", "1.0.0")
                SettingsInfoRow("Шифрование", "ECDH + AES-256-GCM")
                SettingsInfoRow("Транспорт", "BT + WiFi Direct")
                SettingsInfoRow("Сервер", "Peer-to-peer (без сервера)")
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            Spacer(Modifier.height(8.dp))

            // Действия
            SettingsGroup(title = "Данные") {
                SettingsActionRow(
                    icon  = Icons.Default.PrivacyTip,
                    label = "Политика конфиденциальности",
                    onClick = onOpenPrivacy
                )
                SettingsActionRow(
                    icon    = Icons.Default.DeleteForever,
                    label   = "Очистить все чаты",
                    tintRed = true,
                    onClick = { showClearDialog = true }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title   = { Text("Очистить всё?") },
            text    = { Text("Все чаты и сообщения будут удалены с этого устройства. Отменить нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    // TODO: vm.clearAllData()
                    showClearDialog = false
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(title, fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(bottom = 4.dp))
    content()
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    tintRed: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (tintRed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
    }
}
