package com.setka.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setka.data.model.Contact
import com.setka.ui.components.AvatarCircle
import com.setka.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val contacts by vm.allContacts.collectAsState()
    val myUser   by vm.myUser.collectAsState()
    val scope     = rememberCoroutineScope()
    var query    by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = { Text("Контакты", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Поиск") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape     = MaterialTheme.shapes.large,
                singleLine = true
            )

            val filtered = if (query.isBlank()) contacts
            else contacts.filter { it.nickname.contains(query, ignoreCase = true) }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f))
                        Spacer(Modifier.height(12.dp))
                        Text("Нет контактов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                        Spacer(Modifier.height(4.dp))
                        Text("Найдите кого-нибудь рядом!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    }
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.userId }) { contact ->
                        ContactItem(contact = contact, onClick = {
                            val me = myUser ?: return@ContactItem
                            scope.launch {
                                val chatId = vm.chatRepo.getOrCreateDirectChat(
                                    myUserId        = me.id,
                                    myNickname      = me.nickname,
                                    contactUserId   = contact.userId,
                                    contactNickname = contact.nickname
                                )
                                onOpenChat(chatId)
                            }
                        })
                        Divider(
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outline.copy(0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = contact.nickname, color = contact.avatarColor, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            contact.nickname,
            fontWeight = FontWeight.Normal,
            modifier   = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f))
    }
}
