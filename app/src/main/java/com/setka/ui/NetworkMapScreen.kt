package com.setka.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setka.data.model.UserStats
import com.setka.network.transport.DiscoveredPeer
import com.setka.viewmodel.ChatViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val peers   by vm.discoveredPeers.collectAsState()
    val stats   by vm.myStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = { Text("Карта сети", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Индикатор силы сети
            NetworkStrengthCard(peers = peers)

            // Живая карта
            LiveNetworkMap(peers = peers)

            // Статистика
            stats?.let { StatsCard(it) }

            // Достижения
            stats?.let { AchievementsCard(it) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Индикатор силы сети ──────────────────────────────────────────────

@Composable
private fun NetworkStrengthCard(peers: List<DiscoveredPeer>) {
    val strength = when {
        peers.size >= 10 -> 100
        peers.size >= 5  -> 75
        peers.size >= 3  -> 50
        peers.size >= 1  -> 25
        else             -> 0
    }
    val strengthColor = when {
        strength >= 75 -> Color(0xFF2E7D32)
        strength >= 50 -> Color(0xFFF57F17)
        strength >= 25 -> Color(0xFFE65100)
        else           -> Color(0xFFB71C1C)
    }
    val strengthLabel = when {
        strength >= 75 -> "Сильная сеть"
        strength >= 50 -> "Средняя сеть"
        strength >= 25 -> "Слабая сеть"
        else           -> "Нет сети"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Сила сети", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(strengthLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = strengthColor)
                }
                Text("$strength%", fontSize = 32.sp,
                    fontWeight = FontWeight.Bold, color = strengthColor)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = strength / 100f,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = strengthColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text("${peers.size} устройств рядом", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Живая карта сети ─────────────────────────────────────────────────

@Composable
private fun LiveNetworkMap(peers: List<DiscoveredPeer>) {
    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor   = MaterialTheme.colorScheme.surface

    // Пульсация для нашего узла
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue  = 60f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2

                // Пульсирующий круг вокруг нас
                drawCircle(
                    color  = primaryColor.copy(alpha = 0.1f),
                    radius = pulseRadius,
                    center = Offset(cx, cy)
                )

                // Рисуем соединения к пирам
                val peerPositions = mutableListOf<Offset>()
                peers.take(12).forEachIndexed { i, peer ->
                    val angle  = (i * 360f / peers.size.coerceAtLeast(1)) * (Math.PI / 180f)
                    val radius = size.minDimension * 0.35f
                    val px = cx + (radius * cos(angle)).toFloat()
                    val py = cy + (radius * sin(angle)).toFloat()
                    peerPositions.add(Offset(px, py))

                    // Линия соединения — толщина по силе сигнала
                    val signalStrength = ((peer.signalStrength + 100) / 100f).coerceIn(0.1f, 1f)
                    drawLine(
                        color       = secondaryColor.copy(alpha = 0.4f * signalStrength),
                        start       = Offset(cx, cy),
                        end         = Offset(px, py),
                        strokeWidth = 1.5f + signalStrength * 2f,
                        cap         = StrokeCap.Round
                    )
                }

                // Рисуем пиров
                peerPositions.forEachIndexed { i, pos ->
                    drawCircle(color = secondaryColor.copy(0.15f), radius = 18f, center = pos)
                    drawCircle(color = secondaryColor, radius = 10f, center = pos)
                    drawCircle(color = surfaceColor, radius = 5f, center = pos)
                }

                // Наш узел (в центре)
                drawCircle(color = primaryColor.copy(0.2f), radius = 24f, center = Offset(cx, cy))
                drawCircle(color = primaryColor, radius = 14f, center = Offset(cx, cy))
                drawCircle(color = surfaceColor, radius = 7f,  center = Offset(cx, cy))
            }

            // Подпись
            Text(
                "Вы",
                modifier = Modifier.align(Alignment.Center),
                fontSize  = 9.sp,
                color     = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            if (peers.isEmpty()) {
                Text(
                    "Нет устройств рядом",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    fontSize  = 12.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
        }
    }
}

// ── Статистика ───────────────────────────────────────────────────────

@Composable
private fun StatsCard(stats: UserStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Статистика", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Отправлено", stats.totalMessagesSent.toString(), Icons.Default.Send)
                StatItem("Переслано", stats.totalMessagesRelayed.toString(), Icons.Default.Repeat)
                StatItem("Встречено", stats.uniquePeers.toString(), Icons.Default.People)
            }
            if (stats.longestChain > 0) {
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null,
                        tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Рекорд цепочки: ${stats.longestChain} узлов",
                            fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        if (stats.longestChainPath.isNotBlank()) {
                            Text(stats.longestChainPath, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Достижения ───────────────────────────────────────────────────────

@Composable
private fun AchievementsCard(stats: UserStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Достижения", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            val achievements = listOf(
                Triple(stats.achieveFirstMessage, "🚀", "Первый шаг — первое сообщение отправлено"),
                Triple(stats.achieveFirstBridge,  "🌉", "Мост — стал ретранслятором для других"),
                Triple(stats.achieve10Nodes,      "🔗", "Длинная цепь — 10+ узлов подряд"),
                Triple(stats.achieveSavedMessage, "🛡️", "Спаситель — сообщение дошло через тебя"),
                Triple(stats.achieve100Messages,  "💬", "Болтун — 100 отправленных сообщений"),
                Triple(stats.achieveNightOwl,     "🦉", "Ночная сова — онлайн после 2 ночи")
            )

            achievements.forEach { (unlocked, emoji, description) ->
                AchievementRow(
                    emoji       = emoji,
                    description = description,
                    unlocked    = unlocked
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AchievementRow(emoji: String, description: String, unlocked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.outline.copy(0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (unlocked) emoji else "🔒",
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            description,
            fontSize = 14.sp,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
        )
    }
}
