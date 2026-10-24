package com.example.businesscard.ui.koke

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * デザインシステム「Koke(苔むす)」のトークン。
 *
 * 考え方: 霧のような明るい灰の地に、黒い濡れた岩の部品が置かれ、その一部に苔が生える。
 * 岩はフラット寄り(数枚〜十数枚の平らなファセットの塗り分け + 控えめなつや)で、角が欠けた不揃いな形。
 * 鮮やかな色は苔の黄緑だけ。主ボタンの文字と細い縁にも、苔の黄緑を差し色に使う。
 *
 *  - 地: 霧の灰(#D8DAD5)。黒い岩がくっきり映え、苔の黄緑が浮く
 *  - 岩: わずかに青みのある黒(#131517〜#2C2F33)
 *  - 岩の上の文字: 淡い白。地の上の文字(画面タイトル・入力欄のラベル)は墨
 *
 * 文字色は「岩の上」と「地の上」で別のトークンにする(ink / inkMuted / danger は岩の上。AppComponents も岩の上で使う)。
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class KokeColors(
    /** 画面の地(霧の灰) */
    val ground: Color,
    /** 岩のファセットの色の幅(暗い側・明るい側) */
    val rockLo: Color,
    val rockHi: Color,
    /** 苔の黄緑(主ボタンの文字・細い縁・選んでいる印) */
    val moss: Color,
    /** 主ボタンの上の文字(= 苔の黄緑) */
    val onPrimary: Color,
    /** 岩の上の文字(淡い白)。フォーカス中の入力欄の縁の線にも使う */
    val ink: Color,
    /** 岩の上の副文字(名刺の連絡先・ダイアログの説明) */
    val inkMuted: Color,
    /** 岩の上の、さらに薄い文字(入力欄の空のときの案内・アイコン) */
    val inkFaint: Color,
    /** 岩の上の危険色(取り消せない操作の文字・エラーの縁)。黒い岩の上で読める明るい赤褐色 */
    val danger: Color,
    /** 地の上の文字(画面タイトル) */
    val groundInk: Color,
    /** 地の上の副文字(入力欄のラベル) */
    val groundInkMuted: Color,
    /** 地の上の危険色(入力欄のエラーの注記) */
    val groundDanger: Color,
    /** ダイアログの後ろを暗くする色 */
    val scrim: Color,
)

val DefaultKokeColors = KokeColors(
    ground = Color(0xFFD8DAD5),
    rockLo = Color(0xFF131517),
    rockHi = Color(0xFF2C2F33),
    moss = Color(0xFFC8DE6E),
    onPrimary = Color(0xFFC8DE6E),
    ink = Color(0xFFECEDE8),
    inkMuted = Color(0xFFA4A9A3),
    inkFaint = Color(0xFF767C76),
    danger = Color(0xFFEF8B74),
    groundInk = Color(0xFF1C201D),
    groundInkMuted = Color(0xFF555B55),
    groundDanger = Color(0xFFA63A26),
    scrim = Color(0x66101211),
)

val LocalKokeColors = staticCompositionLocalOf { DefaultKokeColors }

/** `KokeTheme.colors.ink` のように参照する入口。 */
object KokeTheme {
    val colors: KokeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKokeColors.current
}

/**
 * 岩の形と面の決まり。
 * @param chip 角の欠けの大きさ(角ごとに 0.75〜1.15 倍に散らす)
 * @param facet ファセットの大きさ(大きい部品ほど大きく、塗り分けが細かくなりすぎないように)
 * @param relief ファセットの明暗の強さ(文字が多い面ほど弱く)
 */
@Immutable
data class KokeRockStyle(val chip: Dp, val facet: Dp, val relief: Float) {
    /**
     * 苔に渡す形。苔は角丸の 45 度の点を基準に角を回り込むので、欠けた角の中ほどに来る角丸
     * (半径 = 欠けの約 1.7 倍)を渡す。
     */
    val mossShape: CornerBasedShape get() = RoundedCornerShape(chip * 1.7f)
}

/** 部品ごとの岩。 */
object KokeRocks {
    val button = KokeRockStyle(chip = 10.dp, facet = 40.dp, relief = 0.8f)
    val icon = KokeRockStyle(chip = 9.dp, facet = 26.dp, relief = 0.8f)
    val field = KokeRockStyle(chip = 8.dp, facet = 44.dp, relief = 0.5f)
    val card = KokeRockStyle(chip = 16.dp, facet = 64.dp, relief = 1.0f)
    val message = KokeRockStyle(chip = 14.dp, facet = 56.dp, relief = 0.6f)
    val dialog = KokeRockStyle(chip = 18.dp, facet = 72.dp, relief = 0.45f)
}

/**
 * 形のトークン(Material3 の Shapes に渡す分と、苔・押した判定の目安)。
 * 部品の見た目の輪郭は岩(KokeRock.kt)が描くので、ここは欠けの大きさに近い角丸にしておく。
 * Material3 の Shapes に渡すので、型は CornerBasedShape。
 */
object KokeShapes {
    val field: CornerBasedShape = KokeRocks.field.mossShape
    val control: CornerBasedShape = KokeRocks.button.mossShape
    val card: CornerBasedShape = KokeRocks.card.mossShape
    val dialog: CornerBasedShape = KokeRocks.dialog.mossShape
}

/** 岩の厚みとパララクス(画面の中の位置 -1..1 に掛ける量)。動きはごく小さく、文字の読みやすさを損なわない。 */
object KokeDepth {
    /** 画面の中央にあるときに見える下の側面 */
    val side = 2.5.dp

    /** 画面の上端・下端で、側面が増減する量(上にあるほど下の側面が長い) */
    val sideTilt = 4.5.dp

    /** 左右の端で見える横の側面 */
    val sideTiltX = 3.dp

    /** 面(文字・苔ごと)が画面の中心から遠ざかる量。苔はさらに 1〜2dp 多くずれる(KokeMoss.kt) */
    val faceShift = 2.5.dp

    /** 岩の側面・影を描くため、部品のまわりに広げる幅 */
    val drawMargin = 12.dp
}

/** 大きさのトークン。 */
object KokeSize {
    /** ボタン・入力欄の高さ */
    val control = 52.dp

    /** アイコンボタンの一辺 */
    val iconButton = 48.dp

    /** 縁の線の太さ */
    val line = 1.dp

    /** フォーカス中・選んでいるときの線の太さ */
    val lineStrong = 1.5.dp

    /** 名刺の名前の下の短い線 */
    val accentRule = 24.dp
}

/** 動きのトークン。押した反応は控えめに: 岩がわずかに縮み、濡れたように少し明るくなる。 */
object KokeMotion {
    /** 押し始め: すっと縮む(跳ねない) */
    val pressSpring: AnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 2200f)

    /** 離したとき: ほんの少しだけ行き過ぎて戻る */
    val releaseSpring: AnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 700f)

    /** 押しきったときに縮む割合(ボタン) */
    const val PRESS_SHRINK = 0.03f

    /** 押しきったときに縮む割合(名刺。大きいので控えめに) */
    const val CARD_SHRINK = 0.015f

    /** 線の色・太さが変わる時間(入力欄のフォーカスなど) */
    const val CHANGE_MILLIS = 160
}

/**
 * 文字トークン。
 * 見出し(画面タイトル・ダイアログ・名刺の名前)は明朝(Serif)の細め・字間広めで静かに。
 * 操作・本文はゴシック(端末の標準)で、太さと大きさの差で階層をはっきりさせる。
 * 端末に日本語の明朝が無いときは、標準の書体で表示される。
 */
object KokeType {
    val title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp,
    )
    val dialogTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.6.sp,
    )
    val cardName = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.sp,
    )
    val cardCompany = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
    val cardDetail = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp)
    val field = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal)
    val button = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
    val label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp)
}

/**
 * 苔の生え方の決まり(どの部品のどこに、どれくらい)。
 * 苔は部品の外へ最大 MossOverhang はみ出すので、部品を並べる間隔はその分を見込んで取る。
 */
internal object KokeMossSpots {
    /** 主ボタンの角に 1 群れ。どの角かはボタンごとの seed で決める(文字は中央なので角なら掛からない) */
    fun primaryButton(seed: Int): List<MossPatch> = listOf(
        MossPatch(
            anchor = if ((seed and 1) == 0) MossAnchor.TopEnd else MossAnchor.BottomStart,
            spread = 38.dp,
            depth = 12.dp,
            height = 6.dp,
        ),
    )

    /** 主のアイコンボタンの右上に、小さく 1 群れ */
    val primaryIcon: List<MossPatch> = listOf(
        MossPatch(anchor = MossAnchor.TopEnd, spread = 22.dp, depth = 9.dp, height = 4.dp),
    )

    /** 選んでいる選択肢の右の辺に、小さく 1 群れ(文字は左寄せなので掛からない) */
    val selectedChoice: List<MossPatch> = listOf(
        MossPatch(anchor = MossAnchor.EdgeEnd, spread = 30.dp, depth = 10.dp, height = 5.dp, along = 0.5f),
    )

    /**
     * 名刺の角と辺に 2 群れ。文字は左寄せなので、苔は右側に寄せる。
     * 名刺ごとの seed で 2 通りの生え方を選び、隣り合う名刺が同じ形にならないようにする。
     */
    fun card(seed: Int): List<MossPatch> = if ((seed and 1) == 0) {
        listOf(
            MossPatch(anchor = MossAnchor.BottomEnd, spread = 64.dp, depth = 20.dp, height = 8.dp),
            MossPatch(anchor = MossAnchor.EdgeTop, spread = 40.dp, depth = 11.dp, height = 5.dp, along = 0.8f),
        )
    } else {
        listOf(
            MossPatch(anchor = MossAnchor.TopEnd, spread = 60.dp, depth = 18.dp, height = 8.dp),
            MossPatch(anchor = MossAnchor.EdgeEnd, spread = 36.dp, depth = 10.dp, height = 5.dp, along = 0.72f),
        )
    }
}
