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
import com.setka.network.transport.DiscoveredPeer
import com.setka.network.transport.TransportState
import com.setka.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val peers        by vm.discoveredPeers.collectAsState()
    val btState      by vm.btState.collectAsState()
    val wifiState    by vm.wifiState.collectAsState()
    val internetState by vm.internetState.collectAsState()
    var scanning     by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.stopDiscovery(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                title = { Text("Рядом", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Статус транспортов
            Text("Каналы связи", fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportChip(
                    label  = "Bluetooth",
                    icon   = Icons.Default.Bluetooth,
                    active = btState is TransportState.Connected ||
                             btState is TransportState.Scanning ||
                             btState is TransportState.Listening
                )
                TransportChip(
                    label  = "WiFi Direct",
                    icon   = Icons.Default.Wifi,
                    active = wifiState is TransportState.Connected ||
                             wifiState is TransportState.Scanning
                )
                TransportChip(
                    label  = "Интернет",
                    icon   = Icons.Default.Cloud,
                    active = internetState is TransportState.Connected
                )
            }

            Spacer(Modifier.height(16.dp))

            // Кнопки
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (scanning) { vm.stopDiscovery(); scanning = false }
                        else { vm.startDiscovery(); scanning = true }
                    },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (scanning) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (scanning) Icons.Default.Stop else Icons.Default.Search,
                        null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (scanning) "Стоп" else "Сканировать")
                }
                OutlinedButton(
                    onClick  = { vm.startListening(); scanning = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BluetoothSearching, null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ждать")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (scanning && peers.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(32.dp),
                            strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Ищем устройства...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                    }
                }
            } else if (!scanning && peers.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нажмите «Сканировать» для поиска",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                }
            }

            if (peers.isNotEmpty()) {
                Text("Найдено устройств: ${peers.size}",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(bottom = 8.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(peers, key = { it.deviceId }) { peer ->
                    PeerItem(peer = peer, onClick = {
                        vm.stopDiscovery()
                        vm.connectToPeer(peer)
                        scanning = false
                        onBack()
                    })
                }
            }
        }
    }
}

@Composable
private fun TransportChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                modifier = Modifier.size(13.dp),
                tint = if (active) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 12.sp,
                color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
        }
    }
}

@Composable
private fun PeerItem(peer: DiscoveredPeer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PhoneAndroid, null,
                modifier = Modifier.size(32.dp),
                tint     = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.deviceName, fontWeight = FontWeight.Medium)
                if (peer.nickname.isNotEmpty()) {
                    Text(peer.nickname, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(peer.deviceId, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
        }
    }
}
