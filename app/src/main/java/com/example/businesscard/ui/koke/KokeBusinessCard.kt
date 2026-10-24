package com.example.businesscard.ui.koke

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.ui.glass.rememberGlassFeedback

/**
 * 名刺(レイアウト固定)。角の欠けた黒い濡れた岩(大きなファセット)。文字は左寄せ、淡い白:
 *
 *   会社名(小さく)
 *   肩書き(副文字)
 *
 *   名前(明朝・大きく)
 *   ──(短い苔の黄緑の線)
 *   電話 / メール(副文字)
 *
 * 右側の角と辺に、苔が 2 群れ生える(名刺ごとに id から種を決め、隣の名刺と同じ形にならない)。
 * 文字は左に寄せてあるので、苔は文字に掛からない。
 *
 * スクロールで画面の中の位置が変わると、面(文字と苔ごと)がわずかにずれ、中心側の側面が少し見える。
 *
 * @param onClick 指定すると面全体がボタンになる。押すとほんの少し縮み、岩が濡れたように少し明るくなる。苔は指の近くが沈む
 */
@Composable
fun KokeBusinessCard(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = KokeTheme.colors
    val rock = KokeRocks.card
    val view = rememberKokeViewOffset()
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = if (onClick != null) rememberKokePress(interaction, feedback) else null
    // 保存前の名刺(id = 0)でも形が決まるよう、名前も混ぜる
    val seed = remember(card.id, card.name) { (card.id * 7919L).toInt() xor card.name.hashCode() }
    val patches = remember(seed) { KokeMossSpots.card(seed) }

    Column(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .trackKokeViewOffset(view)
            .kokeParallax(view)
            .kokeShrink(press, shrink = KokeMotion.CARD_SHRINK)
            .kokeMoss(
                patches = patches,
                shape = rock.mossShape,
                seed = seed,
                interactionSource = if (onClick != null) interaction else null,
                view = view,
            )
            .kokeRock(rock, seed, view = view, press = press)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            feedback.click()
                            onClick()
                        },
                    )
                } else {
                    Modifier
                },
            )
            .fillMaxSize()
            // 右は苔の分を広めに空ける
            .padding(start = 26.dp, top = 22.dp, end = 44.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (card.company.isNotEmpty()) {
                Text(text = card.company, style = KokeType.cardCompany, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (card.title.isNotEmpty()) {
                Text(text = card.title, style = KokeType.cardDetail, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column {
            Text(text = card.name, style = KokeType.cardName, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            // 名前の下の短い苔の黄緑の線(名刺の唯一の飾り)
            Box(
                Modifier
                    .width(KokeSize.accentRule)
                    .height(1.dp)
                    .background(c.moss),
            )
            Spacer(Modifier.height(10.dp))
            if (card.phone.isNotEmpty()) {
                Text(text = card.phone, style = KokeType.cardDetail, color = c.inkMuted, maxLines = 1)
            }
            if (card.email.isNotEmpty()) {
                Text(text = card.email, style = KokeType.cardDetail, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
