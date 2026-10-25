package com.example.businesscard.ui.kinu

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * デザインシステム「Kinu(絹、仮称)」のトークン。
 *
 * 見本は夜の銀座の角の外観(ライトアップ時)のうち、波打つ外装の部分だけ(黒い塔・道路は使わない)。
 * 絹のドレープのように波打つシャンパンゴールドの金属の帯が重なり、各帯の縁に仕込まれた細い光の線が面を照らす。
 * 帯の陰の面は赤みのある灰紫のトープ、帯の間の窓はクリーム〜琥珀に灯る。
 * 色は切り出した写真から採寸した(2026-10-25 12:35)。取り決めは docs/kinu/SPEC.md(v2)。
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class KinuColors(
    // 背景(外装そのもの。2026-10-25 12:31 の指示で黒い塔は使わない)
    /** 帯の陰の面(赤みのある灰紫のトープ) */
    val facadeShade: Color,
    /** 帯の陰の面の冷たい側(角の回り込み) */
    val facadeMauve: Color,
    /** 帯の折り返しの金(下向きの面が照らされた所) */
    val foldGold: Color,
    // 金の帯
    /** 光の当たる所(下からの琥珀の照り返し・つや) */
    val goldLit: Color,
    /** 陰の面(赤みのある茶) */
    val goldShade: Color,
    /** いちばん暗い溝 */
    val goldDeep: Color,
    // 光
    /** 光の線の芯(通常の画面ではこれで止まる。HDR では白を超える) */
    val lineCore: Color,
    /** 光の線の際の色 */
    val lineEdge: Color,
    // 窓(琥珀)
    val amberHi: Color,
    val amber: Color,
    val amberDeep: Color,
    /** 窓の縦の桟 */
    val mullion: Color,
    // サテンの金属(帯の陰の面。副ボタン・入力欄・ダイアログ。旧「夜のガラス」)
    val glassTop: Color,
    val glassBottom: Color,
    /** サテンの金属の縁(光を受けた細い線) */
    val rim: Color,
    // 文字
    /** サテンの金属・背景の上の文字(シャンパン) */
    val champagne: Color,
    val champagneMuted: Color,
    /** 金の帯・琥珀の窓の上の文字(焦げ茶) */
    val ink: Color,
    val inkMuted: Color,
    val danger: Color,
    val scrim: Color,
)

val DefaultKinuColors = KinuColors(
    facadeShade = Color(0xFF6B564D),
    facadeMauve = Color(0xFF7A6970),
    foldGold = Color(0xFFC6A772),
    goldLit = Color(0xFFEDBE8F),
    goldShade = Color(0xFF8A6A58),
    goldDeep = Color(0xFF5E463C),
    lineCore = Color(0xFFFFF4E6),
    lineEdge = Color(0xFFF9E2CE),
    amberHi = Color(0xFFF1CFAF),
    amber = Color(0xFFE7AD84),
    amberDeep = Color(0xFFBA7D50),
    mullion = Color(0xFF987960),
    glassTop = Color(0xFF8E7A78),
    glassBottom = Color(0xFF6B564D),
    rim = Color(0x99F9E2CE),
    champagne = Color(0xFFFBEBD6),
    champagneMuted = Color(0xFFE2CDB4),
    ink = Color(0xFF3A2619),
    inkMuted = Color(0xFF6A4C38),
    danger = Color(0xFF9E2F1F),
    scrim = Color(0x995E463C),
)

val LocalKinuColors = staticCompositionLocalOf { DefaultKinuColors }

object KinuTheme {
    val colors: KinuColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKinuColors.current
}

/** 形。建物の大きく丸い角から。pill にはしない。 */
object KinuShapes {
    val control = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(22.dp)
    val dialog = RoundedCornerShape(26.dp)
}

/** 寸法(光の線・縁)。 */
object KinuDimens {
    /** 名刺・ダイアログの上下の金の縁(リップ)の高さ */
    val lipHeight = 10.dp
    /** 名刺の下の縁の波の振幅 */
    val lipWave = 3.dp
    /** ボタン・入力欄の高さ */
    val controlHeight = 52.dp
    /** 上端・下端の帯の重なりが画面の高さに占める割合(シェーダーの定数と揃える) */
    const val FACADE_FRACTION = 0.16f
}

/** 光の強さ(シェーダーの uniform `line` に渡す値)。 */
object KinuLight {
    const val LINE_GOLD = 1.0f
    const val LINE_NIGHT = 0.35f
    const val LINE_FOCUSED = 1.0f
    /** 押している間に足す量(金の帯は 1.0→1.6) */
    const val LINE_PRESS_BOOST = 0.6f
}

/** 動き。 */
object KinuMotion {
    /** 画面を開いたとき、光の線が左から右へ灯る時間 */
    const val IGNITE_MILLIS = 600
    /** 押した・離したときに光が変わる時間 */
    const val PRESS_MILLIS = 90
    const val RELEASE_MILLIS = 260
    const val CHANGE_MILLIS = 200
}

/** 文字。タイトル・名前は明朝、ラベルは字間を広めに。 */
object KinuType {
    val title = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp, letterSpacing = 0.04.em)
    val dialogTitle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 0.04.em)
    val cardName = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.02.em)
    val cardCompany = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.08.em)
    val cardDetail = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val button = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.06.em)
    val label = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.08.em)
    val field = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
    val body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp)
    val caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp)
}
