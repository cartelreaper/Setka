# Setka — Сетка

Мессенджер который работает **без интернета** - через Bluetooth и WiFi Direct.

## Как работает

| Режим | Дальность | Когда используется |
|---|---|---|
| WiFi Direct | до 200м | Приоритет — быстрее и дальше |
| Bluetooth | до 10м | Fallback если WiFi Direct недоступен |
| Internet | ∞ | В разработке |

приложение **автоматически** выбирает лучший канал. пользователь не замечает переключения.

## Запуск

1. клонируй репозиторий
2. открой в Android Studio (Hedgehog+)
3. `Build → Generate APKs`
4. установи на два Android-устройства (API 26+)

## Использование

- **Устройство A** → нажми **«Ждать»** (становится сервером)  
- **Устройство B** → нажми **«Сканировать»** → найди устройство A → подключись
- пишите

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
