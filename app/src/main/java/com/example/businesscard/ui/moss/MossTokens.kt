package com.example.businesscard.ui.moss

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.ui.unit.sp

/**
 * デザインシステム「Moss(苔岩、仮称)」のトークン。
 *
 * 見本(雨に濡れた黒い岩の上に、細い葉の苔がびっしり密集している写真3枚)から、色と質感を採寸した。
 *
 *  - 地: 濡れた黒い岩。細かい粒の凹凸があり、溝は真っ黒。白っぽい小さなつやの点が散る
 *  - 苔: 1株が数mmの星形。光の当たる上面は明るい黄緑、側面・奥は深緑、根元の隙間は黒緑。
 *    株の先が細かく白く光る(水滴)
 *  - 杉の枯れ葉の赤茶、白っぽい地衣類の斑点が少し混ざる
 *  - 上の方は浅いぼけで、緑と灰色の柔らかい塊になる
 *
 * 色は採寸した中央値をもとにし、文字の色は WCAG のコントラスト比を確かめて決めた
 * (数値は scratchpad の palette.json に記録)。
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class MossColors(
    // 岩(地・板)
    /** 濡れた黒い岩の暗部。溝や画面の奥の暗がり */
    val rockDeep: Color,
    /** 濡れた黒い岩の中間。名刺や入力欄の面の基本色 */
    val rockMid: Color,
    /** 岩の明るい面(磨いた石の表)。文字の下でいちばん明るくなる岩 */
    val rockLight: Color,
    /** 濡れたつやの縁。上の縁に淡く乗る */
    val rockSheen: Color,
    /** 濡れて光る白い点 */
    val glint: Color,
    // 苔
    /** 苔の光が当たる上面。最も明るい黄緑 */
    val mossBright: Color,
    /** 苔の中間の緑 */
    val mossMid: Color,
    /** 側面・奥の深緑 */
    val mossDeep: Color,
    /** 根元・隙間の黒緑 */
    val mossShadow: Color,
    // 添え物
    /** 白っぽい地衣類の斑点 */
    val lichen: Color,
    /** 杉の枯れ葉の赤茶(危険の色にも使う) */
    val cedar: Color,
    /** 上部のぼけた緑 */
    val bokehGreen: Color,
    /** 上部のぼけた灰 */
    val bokehGrey: Color,
    /** 苔の淡い光(フォーカス時に縁が灯る色) */
    val focusGlow: Color,
    // 文字
    /** 岩の上の文字。淡い白緑(岩の明るい面の上で 4.5:1 以上) */
    val ink: Color,
    /** 副の文字。ink より少し暗い */
    val inkMuted: Color,
    /** ヒントや装飾の文字。暗い岩の上でだけ使う */
    val inkFaint: Color,
    /** 明るい苔の上の文字。黒緑(苔の上で 4.5:1 以上) */
    val onMoss: Color,
    /** 危険(削除)。杉の枯れ葉の赤茶を、暗い石の上で読めるよう明るくした */
    val danger: Color,
    /** ダイアログの後ろを暗くする幕 */
    val scrim: Color,
)

/** 採寸した値から決めた色。 */
val DefaultMossColors = MossColors(
    rockDeep = Color(0xFF141312),
    rockMid = Color(0xFF3F3B37),
    rockLight = Color(0xFF6B6763),
    rockSheen = Color(0xFF8A867E),
    glint = Color(0xFFE5E5DD),
    mossBright = Color(0xFFB7DA53),
    mossMid = Color(0xFF809A36),
    mossDeep = Color(0xFF3C4E16),
    mossShadow = Color(0xFF1A240B),
    lichen = Color(0xFFABADA3),
    cedar = Color(0xFF9A6548),
    bokehGreen = Color(0xFF9DB04E),
    bokehGrey = Color(0xFF8E8C84),
    focusGlow = Color(0xFFDCEFA8),
    ink = Color(0xFFE6EEDA),
    inkMuted = Color(0xFFD8E0CC),
    inkFaint = Color(0xFFAEB8A0),
    onMoss = Color(0xFF17200A),
    danger = Color(0xFFD89878),
    scrim = Color(0xCC0C0D0A),
)

val LocalMossColors = staticCompositionLocalOf { DefaultMossColors }

/** `MossTheme.colors.ink` のように参照する入口。 */
object MossTheme {
    val colors: MossColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMossColors.current
}

/**
 * 文字トークン。タイトルと名刺の名前は端末の明朝(FontFamily.Serif)で、落ち着いた和の雰囲気にする。
 * 本文とボタンは端末の既定の書体のまま読みやすさを優先する。
 */
object MossType {
    private val serif = FontFamily.Serif

    val title = TextStyle(fontFamily = serif, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val dialogTitle = TextStyle(fontFamily = serif, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    val cardName = TextStyle(fontFamily = serif, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
    val cardCompany = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val cardDetail = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val field = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    /** ボタンの文字は太字にして、苔の上で読みやすくする */
    val button = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
}

/**
 * 形のトークン。自然の石なので、角丸は控えめ(完全な丸にはしない)。
 * 苔のクッションは形を shader で描くため、ここは当たり判定と枠の形にだけ使う。
 */
object MossShapes {
    val card: CornerBasedShape = RoundedCornerShape(14.dp)
    val control: CornerBasedShape = RoundedCornerShape(12.dp)
    val field: CornerBasedShape = RoundedCornerShape(10.dp)
    val dialog: CornerBasedShape = RoundedCornerShape(18.dp)
}

/**
 * 立体のトークン。板の厚みと、描く範囲の余白。
 *
 * シェーダーは部品の範囲(0..size)を上面とし、その下に厚み分の側面を描く。
 * 側面と接地の暗がりが縁の外へ出るため、余白(PAD)はその分まで広く取る。
 */
object MossDepth {
    /** 石の板の厚み(磨いた石の板の側面の高さ) */
    val STONE_THICK = 8.dp

    /** 苔のクッションの厚み(苔は石より厚く盛り上がって見える) */
    val MOSS_THICK = 10.dp

    /** 彫り込みの溝の厚み。溝は面より下に盛らないので 0 */
    val FIELD_THICK = 0.dp

    /** シェーダーが描く縁の外側の余白(側面と接地の暗がりが入る幅) */
    val PAD = 20.dp
}

/**
 * 動きのトークン。押すと苔や石がわずかに沈み込み、離すとばねで戻る。
 * 触覚のフィードバックは既存の GlassFeedback を使う。
 */
object MossMotion {
    /** 押し始め: 少し弾まず、素早く沈んで止まる */
    val pressSpring: AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 900f)

    /** 離したとき: 少し弾んで戻る */
    val releaseSpring: AnimationSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 400f)

    /** 押したときの縮む割合 */
    const val PRESS_SCALE = 0.97f

    /** 色や光が変わるまでの時間(ミリ秒) */
    const val CHANGE_MILLIS = 220

    /** 画面を開いたとき、苔の光が灯るまでの時間(ミリ秒) */
    const val ENTER_MILLIS = 700
}
