package com.setka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Показывает путь сообщения: A → B → C → D
 */
@Composable
fun MessageRouteView(
    hops: List<String>,
    hopCount: Int,
    modifier: Modifier = Modifier
) {
    if (hops.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.primaryContainer.copy(0.5f),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Путь сообщения ($hopCount прыжков)",
                fontSize   = 10.sp,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                hops.forEachIndexed { index, hop ->
                    // Узел
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0 || index == hops.size - 1)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary.copy(0.7f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            hop.take(1).uppercase(),
                            fontSize   = 9.sp,
                            color      = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Стрелка между узлами
                    if (index < hops.size - 1) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp).padding(horizontal = 1.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                }
            }
            // Никнеймы под кружками
            Row(modifier = Modifier.fillMaxWidth()) {
                hops.take(5).forEachIndexed { index, hop ->
                    Text(
                        hop.take(4),
                        fontSize  = 8.sp,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        modifier  = Modifier.width(28.dp)
                    )
                    if (index < hops.size - 1 && index < 4) {
                        Spacer(Modifier.width(10.dp))
                    }
                }
                if (hops.size > 5) {
                    Text("+${hops.size - 5}", fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                }
            }
        }
    }
}
