package com.diligenciard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AzulOscuro,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E4F5),
    onPrimaryContainer = AzulProfundo,
    secondary = VerdeMejor,
    onSecondary = Color.White,
    tertiary = AmbarAviso,
    error = RojoCongestion,
    background = GrisSuperficie,
    surface = Color.White,
    onSurface = Color(0xFF1A1E23),
    onSurfaceVariant = GrisTexto,
)

private val DarkColors = darkColorScheme(
    primary = AzulOscuroDark,
    onPrimary = AzulProfundo,
    primaryContainer = Color(0xFF2A4468),
    onPrimaryContainer = Color(0xFFD8E4F5),
    secondary = VerdeMejor,
    tertiary = AmbarAviso,
    error = RojoCongestion,
    background = SuperficieDark,
    surface = SuperficieAltaDark,
    onSurface = Color(0xFFE4E7EB),
    onSurfaceVariant = Color(0xFFAeb6c0),
)

@Composable
fun DiligenciaRDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiligenciaTypography,
        content = content
    )
}
