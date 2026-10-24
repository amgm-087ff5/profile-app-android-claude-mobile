package com.example.businesscard.ui.koke

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.LocalThemeStyle

/**
 * Koke のテーマ(単独で使うとき。プレビューや画面のテスト)。
 * アプリ本体は見た目を切り替えるので、BusinessCardAppTheme が同じ値を配る。
 */
@Composable
fun KokeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKokeColors provides DefaultKokeColors,
        LocalThemeStyle provides ThemeStyle.KOKE,
    ) {
        MaterialTheme(
            colorScheme = kokeColorScheme(DefaultKokeColors),
            typography = KokeTypography,
            shapes = KokeMaterialShapes,
            content = content,
        )
    }
}

/**
 * Material3 の ColorScheme への対応づけ(Material3 の標準部品が Koke の色と食い違わないようにするため)。
 * 地は明るい霧の灰(システムバーのアイコンは暗い色)なので lightColorScheme を土台にし、
 * 面(surface)は黒い岩、その上の文字は淡い白にする。
 */
fun kokeColorScheme(c: KokeColors): ColorScheme = lightColorScheme(
    primary = c.rockLo,
    onPrimary = c.onPrimary,
    secondary = c.groundInkMuted,
    onSecondary = c.ground,
    background = c.ground,
    onBackground = c.groundInk,
    surface = c.rockLo,
    onSurface = c.ink,
    surfaceVariant = c.rockHi,
    onSurfaceVariant = c.inkMuted,
    surfaceContainer = c.rockLo,
    error = c.groundDanger,
    outline = c.groundInkMuted,
    outlineVariant = c.inkFaint,
    scrim = c.scrim,
)

val KokeTypography: Typography = Typography().copy(
    headlineMedium = KokeType.title,
    titleLarge = KokeType.dialogTitle,
    titleMedium = KokeType.cardCompany,
    bodyLarge = KokeType.field,
    bodyMedium = KokeType.body,
    bodySmall = KokeType.caption,
    labelLarge = KokeType.button,
    labelMedium = KokeType.label,
)

val KokeMaterialShapes = Shapes(
    extraSmall = KokeShapes.field,
    small = KokeShapes.control,
    medium = KokeShapes.card,
    large = KokeShapes.card,
    extraLarge = KokeShapes.dialog,
)
