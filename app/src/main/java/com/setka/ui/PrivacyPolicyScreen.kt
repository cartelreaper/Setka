package com.setka.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = { Text("Политика конфиденциальности", fontWeight = FontWeight.Medium) },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PolicySection(
                title = "Что мы собираем",
                body  = "Setka не собирает никаких персональных данных. Приложение работает полностью peer-to-peer: ваши сообщения передаются напрямую между устройствами через Bluetooth или WiFi Direct без прохождения через серверы."
            )
            PolicySection(
                title = "Хранение данных",
                body  = "Все сообщения хранятся только на вашем устройстве в зашифрованной локальной базе данных. Мы не имеем доступа к вашей переписке."
            )
            PolicySection(
                title = "Шифрование",
                body  = "Все сообщения защищены end-to-end шифрованием (ECDH + AES-256-GCM). Расшифровать сообщения может только получатель."
            )
            PolicySection(
                title = "Разрешения",
                body  = "Bluetooth и WiFi Direct: используются для установки прямого соединения между устройствами.\n\nМестоположение: требуется Android для работы с WiFi Direct и Bluetooth-сканированием. Ваше местоположение не отправляется на серверы.\n\nМикрофон: используется только при записи голосовых сообщений."
            )
            PolicySection(
                title = "Третьи стороны",
                body  = "Setka не передаёт ваши данные третьим лицам. Приложение не содержит рекламы и аналитических SDK."
            )
            PolicySection(
                title = "Интернет-режим",
                body  = "При использовании интернет-режима сообщения проходят через сервер для доставки. Сервер не хранит содержимое сообщений — только метаданные маршрутизации, которые удаляются сразу после доставки."
            )
            PolicySection(
                title = "Контакт",
                body  = "По вопросам конфиденциальности: privacy@setka.app"
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Последнее обновление: 2024",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(body, fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp)
    }
}
