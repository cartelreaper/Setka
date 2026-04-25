# Setka — Сетка 📡

Мессенджер который работает **без интернета** — через Bluetooth и WiFi Direct.

## Как работает

| Режим | Дальность | Когда используется |
|---|---|---|
| WiFi Direct | до 200м | Приоритет — быстрее и дальше |
| Bluetooth | до 10м | Fallback если WiFi Direct недоступен |
| Internet | ∞ | В разработке |

Приложение **автоматически** выбирает лучший канал. Пользователь не замечает переключения.

## Запуск

1. Клонируй репозиторий
2. Открой в Android Studio (Hedgehog+)
3. `Build → Generate APKs`
4. Установи на два Android-устройства (API 26+)

## Использование

- **Устройство A** → нажми **«Ждать»** (становится сервером)  
- **Устройство B** → нажми **«Сканировать»** → найди устройство A → подключись
- Начинайте писать!

## Архитектура

```
data/
  model/        — User, Chat, Message, Contact, ChatMember
  db/           — Room БД + DAO
  repository/   — репозитории данных

network/
  bluetooth/    — BluetoothTransport (BT Classic RFCOMM)
  wifidirect/   — WifiDirectTransport (WiFi P2P)
  transport/    — TransportManager, NetworkService, пакеты

ui/             — Jetpack Compose экраны
viewmodel/      — ChatViewModel (MVVM)
```

## Технологии

- Kotlin + Jetpack Compose + Material 3
- Room (SQLite) для хранения сообщений
- Bluetooth Classic RFCOMM
- WiFi Direct (WifiP2pManager)
- Kotlin Coroutines + StateFlow

## Лицензия

MIT
