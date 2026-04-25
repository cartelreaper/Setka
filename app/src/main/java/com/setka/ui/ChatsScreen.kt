package com.setka.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setka.data.model.Chat
import com.setka.ui.components.AvatarCircle
import com.setka.ui.components.UnreadBadge
import com.setka.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    vm: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onOpenNearby: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMap: () -> Unit
) {
    val chats            by vm.allChats.collectAsState()
    val connectionStatus by vm.connectionStatus.collectAsState()
    val peers            by vm.discoveredPeers.collectAsState()
    var query            by remember { mutableStateOf("") }
    var showSearch       by remember { mutableStateOf(false) }
    var contextChat      by remember { mutableStateOf<Chat?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value         = query,
                            onValueChange = { query = it },
                            placeholder   = { Text("Поиск") },
                            singleLine    = true,
                            colors        = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor   = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text("Setka", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            if (connectionStatus.isNotBlank()) {
                                Text(connectionStatus, fontSize = 11.sp,
                                    color = when {
                                        connectionStatus.contains("WiFi") ||
                                        connectionStatus.contains("Bluetooth") ->
                                            MaterialTheme.colorScheme.secondary
                                        connectionStatus.contains("Ожидание") ||
                                        connectionStatus.contains("Поиск") ->
                                            MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    })
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; query = "" }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null)
                    }
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Default.Hub, null)
                    }
                    IconButton(onClick = onOpenContacts) {
                        Icon(Icons.Default.People, null)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onOpenNearby,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Badge(
                    containerColor = if (peers.isNotEmpty())
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    if (peers.isNotEmpty()) Text(peers.size.toString(), fontSize = 10.sp)
                }
                Icon(Icons.Default.Bluetooth, null,
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        val filtered = if (query.isBlank()) chats
        else chats.filter { it.name.contains(query, ignoreCase = true) }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                    Spacer(Modifier.height(20.dp))
                    Text("Добро пожаловать в Setka",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Чтобы начать общение:\n\n" +
                        "1. Нажмите кнопку  внизу\n" +
                        "2. Нажмите «Сканировать»\n" +
                        "3. Выберите устройство из списка\n\n" +
                        "Собеседник должен нажать «Ждать» у себя",
                        fontSize  = 15.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onOpenNearby,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Найти людей рядом")
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(filtered, key = { it.id }) { chat ->
                    ChatItem(
                        chat        = chat,
                        unreadCount = vm.getUnreadCount(chat.id).collectAsState().value,
                        onClick     = { onOpenChat(chat.id) },
                        onLongClick = { contextChat = chat }
                    )
                    Divider(thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(0.2f))
                }
            }
        }
    }

    contextChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { contextChat = null },
            title = { Text(chat.name) },
            text  = {
                Column {
                    TextButton(onClick = { vm.setPinned(chat.id, !chat.isPinned); contextChat = null }) {
                        Text(if (chat.isPinned) "Открепить" else "Закрепить")
                    }
                    TextButton(onClick = { vm.setMuted(chat.id, !chat.isMuted); contextChat = null }) {
                        Text(if (chat.isMuted) "Включить уведомления" else "Выключить уведомления")
                    }
                    TextButton(onClick = { vm.deleteChat(chat.id); contextChat = null }) {
                        Text("Удалить чат", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextChat = null }) { Text("Закрыть") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatItem(
    chat: Chat,
    unreadCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeStr = remember(chat.lastMessageAt) {
        if (chat.lastMessageAt == 0L) ""
        else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastMessageAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = chat.name, color = chat.avatarColor, size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Text(chat.name, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chat.isMuted) {
                    Icon(Icons.Default.VolumeOff, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Spacer(Modifier.width(4.dp))
                }
                Text(timeStr, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.lastMessageText, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                if (unreadCount > 0 && !chat.isMuted) {
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge(unreadCount)
                }
            }
        }
    }
}
