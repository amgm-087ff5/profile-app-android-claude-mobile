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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * デザインシステム「Koke(苔むす)」のトークン。
 *
 * 考え方: 部品は平らなモダンUI(単色の面・くっきりした角丸・1dp の細い線・影はほぼ無し)。
 * 色は墨・白・灰だけに抑え、鮮やかな色は部品の一部に生える苔(黄緑)だけにする。
 * 苔の色は苔の描画(KokeMoss.kt)が持つので、ここには置かない。
 *
 *  - 地: 温かい白灰。紙や漆喰のような、何も主張しない平面
 *  - 面: 地より一段明るい白。1dp の線で縁取る
 *  - 主ボタン: 深い墨緑の面(苔の生える石のような色)。白に近い文字
 *  - 文字: 墨。見出しと名刺の名前は明朝(Serif)で静かに、操作の文字はゴシック(Sans)ではっきり
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class KokeColors(
    /** 画面の地(温かい白灰) */
    val ground: Color,
    /** 部品の面(名刺・入力欄・副ボタン・ダイアログ) */
    val surface: Color,
    /** 面を押している間の色(地に少し近づく) */
    val surfacePressed: Color,
    /** 面の縁の細い線 */
    val line: Color,
    /** 主ボタンの面(深い墨緑) */
    val primary: Color,
    /** 主ボタンを押している間の色(少し明るく、緑みが増す) */
    val primaryPressed: Color,
    /** 主ボタンの上の文字 */
    val onPrimary: Color,
    /** 文字(墨)。入力欄のフォーカス線と、選んでいる選択肢の線にも使う */
    val ink: Color,
    /** 副文字(ラベル・名刺の連絡先) */
    val inkMuted: Color,
    /** さらに薄い文字(入力欄の空のときの案内・アイコン) */
    val inkFaint: Color,
    /** 取り消せない操作・エラー(赤褐色。鮮やかすぎない土の赤) */
    val danger: Color,
    /** ダイアログの後ろを暗くする色 */
    val scrim: Color,
)

val DefaultKokeColors = KokeColors(
    ground = Color(0xFFEEEDE8),
    surface = Color(0xFFFAFAF7),
    surfacePressed = Color(0xFFF1F0EB),
    line = Color(0xFFD9D7CF),
    primary = Color(0xFF1E2A22),
    primaryPressed = Color(0xFF2B3A2F),
    onPrimary = Color(0xFFF4F5F0),
    ink = Color(0xFF1F2420),
    inkMuted = Color(0xFF5E625C),
    inkFaint = Color(0xFF93968F),
    danger = Color(0xFFB4462E),
    scrim = Color(0x521F2420),
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
 * 形のトークン。丸すぎない、くっきりした角丸(苔が角を回り込めるよう、はっきりした「角」を残す)。
 * 大きい部品ほど角丸を大きくして、並べたときの階層が分かるようにする。
 * Material3 の Shapes に渡すので、型は CornerBasedShape。
 */
object KokeShapes {
    val field: CornerBasedShape = RoundedCornerShape(12.dp)
    val control: CornerBasedShape = RoundedCornerShape(14.dp)
    val card: CornerBasedShape = RoundedCornerShape(18.dp)
    val dialog: CornerBasedShape = RoundedCornerShape(24.dp)
}

/** 大きさのトークン。 */
object KokeSize {
    /** ボタン・入力欄の高さ */
    val control = 52.dp

    /** アイコンボタンの一辺 */
    val iconButton = 48.dp

    /** 面の縁の線の太さ */
    val line = 1.dp

    /** フォーカス中・選んでいるときの線の太さ */
    val lineStrong = 1.5.dp
}

/** 動きのトークン。押した反応は平面的に: 面の色が少し変わり、わずかに縮む。 */
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
