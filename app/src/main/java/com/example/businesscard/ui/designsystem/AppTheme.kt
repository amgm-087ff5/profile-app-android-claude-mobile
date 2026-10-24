package com.example.businesscard.ui.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.koke.DefaultKokeColors
import com.example.businesscard.ui.koke.KokeMaterialShapes
import com.example.businesscard.ui.koke.KokeTypography
import com.example.businesscard.ui.koke.LocalKokeColors
import com.example.businesscard.ui.koke.kokeColorScheme
import com.example.businesscard.ui.porcelain.DefaultPorcelainColors
import com.example.businesscard.ui.porcelain.LocalPorcelainColors
import com.example.businesscard.ui.porcelain.PorcelainMaterialShapes
import com.example.businesscard.ui.porcelain.PorcelainTheme
import com.example.businesscard.ui.porcelain.PorcelainTypography
import com.example.businesscard.ui.porcelain.porcelainColorScheme
import com.example.businesscard.ui.theme.BusinessCardTheme

/**
 * いま描いている見た目。画面は `App*` の共通部品だけを使い、部品がこの値を見て描き分ける。
 *
 * 各デザインシステムのテーマ([PorcelainTheme] など)が自分の値を配る。何も配られていないとき
 * (Edgelit の [BusinessCardTheme] だけで包んだプレビューやテスト)は Edgelit として描く。
 */
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.EDGELIT }

/**
 * 画面の余白・間隔。部品の影の長さが見た目ごとに違うので、間隔も見た目ごとに決める。
 */
@Immutable
data class AppDimens(
    /** 画面の左右の余白 */
    val screenHorizontal: Dp,
    /** 一覧の名刺どうしの間隔(影と光が次の名刺にかからない広さ) */
    val listSpacing: Dp,
    /** 入力欄どうしの間隔 */
    val fieldSpacing: Dp,
    /** 一覧・入力欄の上下の余白 */
    val contentTop: Dp,
    val contentBottom: Dp,
    /** 画面下の操作の上下の余白 */
    val actionsTop: Dp,
    val actionsBottom: Dp,
    /** 表示画面(横画面)の外周の余白と、名刺と操作の間隔 */
    val detailPadding: PaddingValues,
    val detailSpacing: Dp,
)

private val EdgelitDimens = AppDimens(
    screenHorizontal = 24.dp,
    listSpacing = 36.dp,
    fieldSpacing = 24.dp,
    contentTop = 8.dp,
    contentBottom = 40.dp,
    actionsTop = 12.dp,
    actionsBottom = 24.dp,
    // 影は右下に落ちるので、右と下の余白を左上より広く取る
    detailPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 32.dp, bottom = 32.dp),
    detailSpacing = 32.dp,
)

private val PorcelainDimens = AppDimens(
    screenHorizontal = 20.dp,
    listSpacing = 30.dp,
    fieldSpacing = 18.dp,
    contentTop = 8.dp,
    contentBottom = 32.dp,
    actionsTop = 12.dp,
    actionsBottom = 20.dp,
    detailPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 22.dp, bottom = 24.dp),
    detailSpacing = 24.dp,
)

private val KokeDimens = AppDimens(
    screenHorizontal = 24.dp,
    listSpacing = 28.dp,
    fieldSpacing = 20.dp,
    contentTop = 8.dp,
    contentBottom = 40.dp,
    actionsTop = 12.dp,
    actionsBottom = 24.dp,
    // 苔が縁からはみ出す分の余白を取る
    detailPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 28.dp, bottom = 28.dp),
    detailSpacing = 28.dp,
)

/** 見た目ごとの余白・間隔。 */
object AppTheme {
    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = when (LocalThemeStyle.current) {
            ThemeStyle.EDGELIT -> EdgelitDimens
            ThemeStyle.PORCELAIN -> PorcelainDimens
            ThemeStyle.KOKE -> KokeDimens
        }
}

/**
 * アプリ全体のテーマ。選んでいる見た目([style])のトークンを配る。
 *
 * 見た目を切り替えても画面の状態(開いている画面・入力中の文字・ダイアログ)が消えないよう、
 * [content] は見た目によらず同じ場所から呼ぶ(when で分けて呼ぶと、別の画面として作り直される)。
 * そのため、どちらの見た目でも両方のトークンを配り、Material3 のテーマだけを見た目に合わせて差し替える。
 */
@Composable
fun BusinessCardAppTheme(style: ThemeStyle, content: @Composable () -> Unit) {
    // Edgelit のトークン(LocalSoftGlassColors と Material3 のテーマ)は常に配っておく
    BusinessCardTheme {
        val edgelit = MaterialTheme.colorScheme
        val edgelitType = MaterialTheme.typography
        val edgelitShapes = MaterialTheme.shapes
        CompositionLocalProvider(
            LocalThemeStyle provides style,
            LocalPorcelainColors provides DefaultPorcelainColors,
            LocalKokeColors provides DefaultKokeColors,
        ) {
            MaterialTheme(
                colorScheme = when (style) {
                    ThemeStyle.EDGELIT -> edgelit
                    ThemeStyle.PORCELAIN -> porcelainColorScheme(DefaultPorcelainColors)
                    ThemeStyle.KOKE -> kokeColorScheme(DefaultKokeColors)
                },
                typography = when (style) {
                    ThemeStyle.EDGELIT -> edgelitType
                    ThemeStyle.PORCELAIN -> PorcelainTypography
                    ThemeStyle.KOKE -> KokeTypography
                },
                shapes = when (style) {
                    ThemeStyle.EDGELIT -> edgelitShapes
                    ThemeStyle.PORCELAIN -> PorcelainMaterialShapes
                    ThemeStyle.KOKE -> KokeMaterialShapes
                },
                content = content,
            )
        }
    }
}

/** 見た目が暗い配色か(システムバーのアイコンを白にするかどうか)。 */
val ThemeStyle.isDark: Boolean
    get() = when (this) {
        ThemeStyle.EDGELIT -> true
        ThemeStyle.PORCELAIN -> false
        ThemeStyle.KOKE -> true
    }
