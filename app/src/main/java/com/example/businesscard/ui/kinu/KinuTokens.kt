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
 * 見本は夜の銀座の角の外観(ライトアップ時)。黒い塔の足元に、絹のドレープのように波打つ淡い金の帯が重なり、
 * 各帯の上の縁に仕込まれた細い光の線が面を舐めるように照らす。帯の間のガラスの奥は琥珀色に灯る。
 * 色は写真(1449×2576)から採寸した(2026-10-25)。取り決めは docs/kinu/SPEC.md。
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class KinuColors(
    // 背景(塔と空)
    /** 塔の地(黒に近い、わずかに青み) */
    val tower: Color,
    /** 塔の上のもやの空 */
    val sky: Color,
    /** 塔の縦の銀の波線 */
    val fin: Color,
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
    // 夜のガラス
    val glassTop: Color,
    val glassBottom: Color,
    /** 夜のガラスの縁(細い金の線) */
    val rim: Color,
    // 文字
    /** 夜のガラス・背景の上の文字(シャンパン) */
    val champagne: Color,
    val champagneMuted: Color,
    /** 金の帯・琥珀の窓の上の文字(焦げ茶) */
    val ink: Color,
    val inkMuted: Color,
    val danger: Color,
    val scrim: Color,
)

val DefaultKinuColors = KinuColors(
    tower = Color(0xFF16161A),
    sky = Color(0xFF2E2F2B),
    fin = Color(0xFF63626A),
    goldLit = Color(0xFFD9A57C),
    goldShade = Color(0xFF795C4C),
    goldDeep = Color(0xFF4A3420),
    lineCore = Color(0xFFFFF6E0),
    lineEdge = Color(0xFFEBA874),
    amberHi = Color(0xFFFFDEAA),
    amber = Color(0xFFECB272),
    amberDeep = Color(0xFF997255),
    mullion = Color(0xFF7A5A40),
    glassTop = Color(0xFF1E1E23),
    glassBottom = Color(0xFF141417),
    rim = Color(0x668B7A62),
    champagne = Color(0xFFEBD8B8),
    champagneMuted = Color(0xFFA9997F),
    ink = Color(0xFF2B1D12),
    inkMuted = Color(0xFF5A4030),
    danger = Color(0xFFF0907A),
    scrim = Color(0xB3000000),
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
    /** 足元の帯が画面の下から占める割合 */
    const val FACADE_FRACTION = 0.22f
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
