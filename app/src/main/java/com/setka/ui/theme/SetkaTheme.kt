package com.setka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Цвета ────────────────────────────────────────────────────────────
val Blue500   = Color(0xFF1565C0)
val Blue400   = Color(0xFF1976D2)
val Blue100   = Color(0xFFBBDEFB)
val Blue50    = Color(0xFFE3F2FD)

val Green500  = Color(0xFF2E7D32)
val Green400  = Color(0xFF388E3C)
val Green100  = Color(0xFFC8E6C9)

val Gray900   = Color(0xFF121212)
val Gray800   = Color(0xFF1E1E1E)
val Gray700   = Color(0xFF2C2C2C)
val Gray200   = Color(0xFFEEEEEE)
val Gray100   = Color(0xFFF5F5F5)

val White     = Color(0xFFFFFFFF)
val Black     = Color(0xFF000000)

private val LightColors = lightColorScheme(
    primary          = Blue500,
    onPrimary        = White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue500,
    secondary        = Green500,
    onSecondary      = White,
    secondaryContainer = Green100,
    onSecondaryContainer = Green500,
    background       = White,
    onBackground     = Gray900,
    surface          = White,
    onSurface        = Gray900,
    surfaceVariant   = Gray100,
    onSurfaceVariant = Color(0xFF555555),
    outline          = Color(0xFFCCCCCC)
)

private val DarkColors = darkColorScheme(
    primary          = Blue100,
    onPrimary        = Blue500,
    primaryContainer = Blue500,
    onPrimaryContainer = Blue100,
    secondary        = Green100,
    onSecondary      = Green500,
    secondaryContainer = Green500,
    onSecondaryContainer = Green100,
    background       = Gray900,
    onBackground     = White,
    surface          = Gray800,
    onSurface        = White,
    surfaceVariant   = Gray700,
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline          = Color(0xFF444444)
)

@Composable
fun SetkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
