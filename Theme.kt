package it.scadenziario.app

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

val Seeds = listOf(
    "Blu" to Color(0xFF0B4F6C),
    "Verde" to Color(0xFF1B7F4B),
    "Arancio" to Color(0xFFC4630A),
    "Viola" to Color(0xFF6B3FA0),
    "Rosso" to Color(0xFFB3261E)
)

val FontFamilies = listOf(
    "Predefinito" to FontFamily.Default,
    "Sans" to FontFamily.SansSerif,
    "Serif" to FontFamily.Serif,
    "Monospazio" to FontFamily.Monospace
)

private fun seedScheme(seed: Color, dark: Boolean): ColorScheme =
    if (!dark) lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = lerp(seed, Color.White, 0.82f),
        onPrimaryContainer = lerp(seed, Color.Black, 0.6f),
        secondaryContainer = lerp(seed, Color.White, 0.82f),
        onSecondaryContainer = lerp(seed, Color.Black, 0.6f)
    ) else darkColorScheme(
        primary = lerp(seed, Color.White, 0.55f),
        onPrimary = lerp(seed, Color.Black, 0.75f),
        primaryContainer = lerp(seed, Color.Black, 0.55f),
        onPrimaryContainer = lerp(seed, Color.White, 0.8f),
        secondaryContainer = lerp(seed, Color.Black, 0.55f),
        onSecondaryContainer = lerp(seed, Color.White, 0.8f)
    )

private fun Typography.withFamily(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun ScadenziarioTheme(s: AppSettings, content: @Composable () -> Unit) {
    val dark = when (s.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val scheme = if (s.colorIndex < 0 && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        seedScheme(Seeds[s.colorIndex.coerceIn(0, Seeds.lastIndex)].second, dark)
    }
    val typo = Typography().withFamily(FontFamilies[s.font.coerceIn(0, FontFamilies.lastIndex)].second)

    // Icone di stato e barra di navigazione leggibili anche se il tema scelto è diverso da quello del telefono
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * s.fontScale)) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}

/** Colori (sfondo, testo) per lo stato di scadenza. */
@Composable
fun levelColors(level: Level): Pair<Color, Color> {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (level) {
        Level.OK -> if (dark) Color(0xFF163A29) to Color(0xFF6FD49B) else Color(0xFFE1F3E8) to Color(0xFF1B7F4B)
        Level.WARN -> if (dark) Color(0xFF40300F) to Color(0xFFF2B45A) else Color(0xFFFDECCB) to Color(0xFF8A5000)
        Level.BAD -> if (dark) Color(0xFF491B18) to Color(0xFFFF958D) else Color(0xFFFBE0DD) to Color(0xFFB3261E)
    }
}
