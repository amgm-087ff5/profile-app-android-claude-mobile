package com.example.businesscard.ui.moss

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.LocalThemeStyle

/**
 * Moss のテーマ(単独で使うとき。プレビューや画面のテスト)。
 * アプリ本体は見た目を切り替えるので、BusinessCardAppTheme が同じ値を配る。
 */
@Composable
fun MossTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMossColors provides DefaultMossColors,
        LocalThemeStyle provides ThemeStyle.MOSS,
    ) {
        MaterialTheme(
            colorScheme = mossColorScheme(DefaultMossColors),
            typography = MossTypography,
            shapes = MossMaterialShapes,
            content = content,
        )
    }
}

/**
 * Material3 の ColorScheme への対応づけ(Material3 の標準部品が Moss の色と食い違わないようにするため)。
 * 暗い岩の上に明るい文字の配色なので、darkColorScheme を土台にする。
 */
fun mossColorScheme(c: MossColors): ColorScheme = darkColorScheme(
    primary = c.mossBright,
    onPrimary = c.onMoss,
    secondary = c.mossMid,
    onSecondary = c.onMoss,
    background = c.rockDeep,
    onBackground = c.ink,
    surface = c.rockMid,
    onSurface = c.ink,
    surfaceVariant = c.rockLight,
    onSurfaceVariant = c.inkMuted,
    surfaceContainer = c.rockMid,
    error = c.danger,
    onError = c.rockDeep,
    outline = c.inkFaint,
)

val MossTypography: Typography = Typography().copy(
    headlineMedium = MossType.title,
    titleLarge = MossType.dialogTitle,
    titleMedium = MossType.cardCompany,
    bodyLarge = MossType.field,
    bodyMedium = MossType.body,
    labelLarge = MossType.button,
    labelMedium = MossType.label,
)

val MossMaterialShapes = Shapes(
    extraSmall = MossShapes.field,
    small = MossShapes.control,
    medium = MossShapes.card,
    large = MossShapes.card,
    extraLarge = MossShapes.dialog,
)
