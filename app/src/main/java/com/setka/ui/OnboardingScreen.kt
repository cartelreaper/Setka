package com.setka.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val permissionNote: String? = null
)

private val pages = listOf(
    OnboardingPage(
        icon            = Icons.Default.Bluetooth,
        title           = "Bluetooth",
        description     = "Setka использует Bluetooth для общения с людьми поблизости — без интернета и серверов.",
        permissionNote  = "Разрешение необходимо для поиска и подключения к устройствам"
    ),
    OnboardingPage(
        icon            = Icons.Default.Wifi,
        title           = "WiFi Direct",
        description     = "WiFi Direct даёт дальность до 200 метров — в несколько раз больше чем Bluetooth.",
        permissionNote  = "Разрешение на местоположение требуется Android для WiFi Direct"
    ),
    OnboardingPage(
        icon            = Icons.Default.Mic,
        title           = "Голосовые сообщения",
        description     = "Записывайте и отправляйте голосовые сообщения прямо в чате.",
        permissionNote  = "Микрофон используется только при записи голосовых"
    ),
    OnboardingPage(
        icon            = Icons.Default.Lock,
        title           = "Ваши данные защищены",
        description     = "Все сообщения шифруются end-to-end. Никаких серверов, никаких логов — только вы и ваш собеседник.",
        permissionNote  = null
    )
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding"
        ) { currentPage ->
            val p = pages[currentPage]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(p.icon, null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(32.dp))

                Text(p.title,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center)

                Spacer(Modifier.height(16.dp))

                Text(p.description,
                    fontSize  = 16.sp,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp)

                p.permissionNote?.let { note ->
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color  = MaterialTheme.colorScheme.surfaceVariant,
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(note, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Нижняя панель
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Точки-индикаторы
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == page) 20.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(0.4f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (page < pages.size - 1) page++
                    else onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (page < pages.size - 1) "Далее" else "Начать",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (page < pages.size - 1) {
                TextButton(onClick = onDone) {
                    Text("Пропустить",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
