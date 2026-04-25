package com.setka.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.setka.data.model.Message
import com.setka.data.model.MessageStatus
import com.setka.data.model.MessageType
import com.setka.data.model.TransportType
import com.setka.ui.components.MediaHelper
import com.setka.ui.components.PlaybackState
import com.setka.ui.components.VoicePlayer
import com.setka.ui.components.VoiceRecorder
import com.setka.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    chatId: String,
    onBack: () -> Unit
) {
    val context   = LocalContext.current
    val messages  by vm.getMessages(chatId).collectAsState()
    val myUser    by vm.myUser.collectAsState()
    val typing    by vm.typingStates.collectAsState()
    val isTyping   = typing[chatId] == true
    val scope      = rememberCoroutineScope()

    var text         by remember { mutableStateOf("") }
    var isRecording  by remember { mutableStateOf(false) }
    var recordSecs   by remember { mutableStateOf(0) }
    val listState     = rememberLazyListState()
    val voiceRecorder = remember { VoiceRecorder(context) }
    val playbackState by VoicePlayer.state.collectAsState()

    var fileSizeWarning by remember { mutableStateOf("") }

    // Пикер изображений — лимит 10 МБ
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val size = MediaHelper.getFileSize(context, it)
            if (size > 10 * 1024 * 1024) {
                fileSizeWarning = "Фото слишком большое (макс. 10 МБ). Размер: ${MediaHelper.formatFileSize(size)}"
            } else {
                fileSizeWarning = ""
                vm.sendImageMessage(chatId, it, context)
            }
        }
    }

    // Пикер файлов — лимит 25 МБ
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val size = MediaHelper.getFileSize(context, it)
            if (size > 25 * 1024 * 1024) {
                fileSizeWarning = "Файл слишком большой (макс. 25 МБ). Размер: ${MediaHelper.formatFileSize(size)}"
            } else {
                fileSizeWarning = ""
                vm.sendFileMessage(chatId, it, context)
            }
        }
    }

    // Таймер записи
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSecs = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordSecs++
                if (recordSecs >= 120) { // макс 2 минуты
                    val result = voiceRecorder.stop()
                    result?.let { (file, dur) -> vm.sendVoiceMsg(chatId, file, dur) }
                    isRecording = false
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(chatId) { vm.markAllRead(chatId) }

    val chats    by vm.allChats.collectAsState()
    val chatName  = chats.find { it.id == chatId }?.name ?: "Чат"

    var showAttachMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        VoicePlayer.stop()
                        onBack()
                    }) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = {
                    Column {
                        Text(chatName, fontWeight = FontWeight.Medium)
                        if (isTyping) Text("печатает...", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column {
                // Предупреждение о размере файла
                if (fileSizeWarning.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(fileSizeWarning, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = { fileSizeWarning = "" },
                                modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Меню прикреплений
                if (showAttachMenu) {
                    Surface(shadowElevation = 4.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AttachChip(icon = Icons.Default.Image, label = "Фото") {
                                showAttachMenu = false
                                imagePicker.launch("image/*")
                            }
                            AttachChip(icon = Icons.Default.AttachFile, label = "Файл") {
                                showAttachMenu = false
                                filePicker.launch("*/*")
                            }
                        }
                    }
                }

                Surface(shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Кнопка прикрепления
                        IconButton(onClick = { showAttachMenu = !showAttachMenu }) {
                            Icon(
                                if (showAttachMenu) Icons.Default.Close else Icons.Default.Add,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isRecording) {
                            // Режим записи
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FiberManualRecord, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Запись %d:%02d".format(recordSecs / 60, recordSecs % 60),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    vm.sendTyping(chatId, it.isNotEmpty())
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Сообщение") },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        // Кнопка отправки или записи голосового
                        if (text.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    vm.sendTextMessage(chatId, text)
                                    text = ""
                                    showAttachMenu = false
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Send, null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp))
                            }
                        } else {
                            // Зажать = запись, отпустить = отправить
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isRecording) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                val file = voiceRecorder.start()
                                                if (file != null) {
                                                    isRecording = true
                                                    tryAwaitRelease()
                                                    val result = voiceRecorder.stop()
                                                    isRecording = false
                                                    result?.let { (f, dur) ->
                                                        if (dur > 500) vm.sendVoiceMsg(chatId, f, dur)
                                                        else f.delete()
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    null,
                                    tint = if (isRecording) MaterialTheme.colorScheme.onError
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Начните переписку",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message       = msg,
                        isOwn         = msg.senderId == myUser?.id,
                        playbackState = playbackState,
                        onPlayVoice   = { msgId, path ->
                            VoicePlayer.play(msgId, path, scope)
                        },
                        onLongClick   = { vm.deleteMessage(msg.id) },
                        onRetry       = { vm.retryMessage(msg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    playbackState: PlaybackState,
    onPlayVoice: (String, String) -> Unit,
    onLongClick: () -> Unit,
    onRetry: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFmt.format(Date(message.sentAt))

    if (message.isDeleted) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            Text("Сообщение удалено", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        }
        return
    }

    val bubbleBg = if (isOwn) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOwn) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurfaceVariant
    val subColor = textColor.copy(alpha = 0.65f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isOwn) 16.dp else 4.dp,
                    bottomEnd   = if (isOwn) 4.dp  else 16.dp
                ))
                .background(bubbleBg)
                .combinedClickable(onLongClick = onLongClick, onClick = {})
        ) {
            Column {
                // Фото — без внутренних отступов
                if (message.type == MessageType.IMAGE && message.mediaPath != null) {
                    AsyncImage(
                        model = File(message.mediaPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isOwn) 16.dp else 4.dp,
                                bottomEnd   = if (isOwn) 4.dp  else 16.dp
                            ))
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (!isOwn) {
                        Text(message.senderNickname, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(2.dp))
                    }

                    when (message.type) {
                        MessageType.TEXT -> Text(message.text, color = textColor)

                        MessageType.IMAGE -> if (message.text.isNotBlank()) {
                            Text(message.text, color = textColor)
                        }

                        MessageType.VOICE -> {
                            val isPlaying = playbackState.isPlaying &&
                                            playbackState.currentMessageId == message.id
                            val progress  = if (playbackState.currentMessageId == message.id)
                                            playbackState.progress else 0f
                            val duration  = if (message.voiceDurationMs > 0)
                                            MediaHelper.formatDuration(message.voiceDurationMs)
                                            else "0:00"

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        message.mediaPath?.let { onPlayVoice(message.id, it) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        null, tint = textColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.fillMaxWidth().height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = textColor,
                                        trackColor = textColor.copy(0.25f)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(duration, fontSize = 11.sp, color = subColor)
                                }
                            }
                        }

                        MessageType.FILE -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, null,
                                modifier = Modifier.size(28.dp), tint = textColor)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(message.mediaName ?: "Файл", color = textColor,
                                    fontSize = 14.sp, maxLines = 1)
                                Text(MediaHelper.formatFileSize(message.mediaSizeBytes),
                                    color = subColor, fontSize = 11.sp)
                            }
                        }

                        else -> {}
                    }

                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Кнопка повтора для FAILED
                        if (isOwn && message.status == MessageStatus.FAILED) {
                            TextButton(
                                onClick = onRetry,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(20.dp)
                            ) {
                                Text("Повторить", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        val transportIcon = when (message.transport) {
                            TransportType.BLUETOOTH   -> Icons.Default.Bluetooth
                            TransportType.WIFI_DIRECT -> Icons.Default.Wifi
                            TransportType.INTERNET    -> Icons.Default.Cloud
                            else -> null
                        }
                        transportIcon?.let {
                            Icon(it, null, modifier = Modifier.size(10.dp), tint = subColor)
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(timeStr, fontSize = 11.sp, color = subColor)
                        if (isOwn) {
                            Spacer(Modifier.width(3.dp))
                            StatusIcon(message.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.SENDING   -> Icons.Default.Schedule to Color.Gray
        MessageStatus.SENT      -> Icons.Default.Done to Color.Gray
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to Color.Gray
        MessageStatus.READ      -> Icons.Default.DoneAll to Color(0xFF4CAF50)
        MessageStatus.FAILED    -> Icons.Default.ErrorOutline to Color.Red
    }
    Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
}
