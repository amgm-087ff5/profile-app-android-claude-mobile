package com.example.businesscard.ui.moss

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * 名刺(レイアウト固定)。石の板の上に苔が縁に少し生えた、見本の名刺。
 * 氏名は明朝(MossType.cardName)。並びは会社・役職が上、氏名が中ほど、電話・メールが下。
 *
 * @param onClick 指定すると面全体がボタンになる。押すと板が沈み込み、離すとばねで浮き上がる
 */
@Composable
fun MossBusinessCard(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = MossTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = if (onClick != null) rememberMossPress(interaction, feedback) else null

    Column(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .mossSurface(shape = MossShapes.card, tone = MossTone.Stone, pressed = press, moss = 0.8f)
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (card.company.isNotEmpty()) {
                Text(text = card.company, style = MossType.cardCompany, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (card.title.isNotEmpty()) {
                Text(text = card.title, style = MossType.body, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(text = card.name, style = MossType.cardName, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column {
            if (card.phone.isNotEmpty()) {
                Text(text = card.phone, style = MossType.body, color = c.inkMuted, maxLines = 1)
            }
            if (card.email.isNotEmpty()) {
                Text(text = card.email, style = MossType.body, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
