package com.example.businesscard.ui.kinu

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
 * Kinu のテーマ(単独で使うとき。プレビューや画面のテスト)。
 * アプリ本体は見た目を切り替えるので、BusinessCardAppTheme が同じ値を配る。
 */
@Composable
fun KinuTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKinuColors provides DefaultKinuColors,
        LocalThemeStyle provides ThemeStyle.KINU,
    ) {
        MaterialTheme(
            colorScheme = kinuColorScheme(DefaultKinuColors),
            typography = KinuTypography,
            shapes = KinuMaterialShapes,
            content = content,
        )
    }
}

/**
 * Material3 の ColorScheme への対応づけ。夜の配色なので darkColorScheme を土台にする。
 * 金の帯の上の文字は焦げ茶(ink)、夜のガラスの上の文字はシャンパン(champagne)。
 */
fun kinuColorScheme(c: KinuColors): ColorScheme = darkColorScheme(
    primary = c.goldLit,
    onPrimary = c.ink,
    secondary = c.amber,
    onSecondary = c.ink,
    background = c.tower,
    onBackground = c.champagne,
    surface = c.glassTop,
    onSurface = c.champagne,
    onSurfaceVariant = c.champagneMuted,
    surfaceContainer = c.glassTop,
    error = c.danger,
    onError = c.ink,
    outline = c.champagneMuted,
)

val KinuTypography: Typography = Typography().copy(
    headlineMedium = KinuType.title,
    titleLarge = KinuType.dialogTitle,
    titleMedium = KinuType.cardCompany,
    bodyLarge = KinuType.field,
    bodyMedium = KinuType.body,
    labelLarge = KinuType.button,
    labelMedium = KinuType.label,
)

val KinuMaterialShapes = Shapes(
    extraSmall = KinuShapes.control,
    small = KinuShapes.control,
    medium = KinuShapes.card,
    large = KinuShapes.card,
    extraLarge = KinuShapes.dialog,
)
