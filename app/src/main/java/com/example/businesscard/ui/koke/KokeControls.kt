package com.example.businesscard.ui.koke

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.businesscard.R
import com.example.businesscard.ui.glass.GlassFeedback
import com.example.businesscard.ui.glass.rememberGlassFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ボタンの役割。
 *  - Primary: その画面の主操作。墨緑の面で、角に苔が生える
 *  - Secondary: 副操作。白い面に細い線。苔は生えない(主操作との対比のため)
 */
enum class KokeTone { Primary, Secondary }

/** 役割ごとの文字色の既定。 */
@Composable
private fun inkOn(tone: KokeTone): Color = when (tone) {
    KokeTone.Primary -> KokeTheme.colors.onPrimary
    KokeTone.Secondary -> KokeTheme.colors.ink
}

// ---- 押した反応(平面的に: 面の色が少し変わり、わずかに縮む) ----

/** 押している度合い。0 = 休んでいる、1 = 押しきった。 */
@Stable
internal class KokePress {
    internal val amount = Animatable(0f)
    val value: Float get() = amount.value
}

/**
 * [interactionSource] の押す・離すに合わせて [KokePress] を動かし、触覚を返す。
 * 素早く叩いても、押しきった所までは見せてから戻す。
 */
@Composable
internal fun rememberKokePress(interactionSource: InteractionSource, feedback: GlassFeedback?): KokePress {
    val press = remember { KokePress() }
    LaunchedEffect(interactionSource, feedback) {
        var pressIn: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    feedback?.pressDown()
                    pressIn = launch { press.amount.animateTo(1f, KokeMotion.pressSpring) }
                }
                is PressInteraction.Release -> {
                    feedback?.pressUp()
                    val pressing = pressIn
                    launch {
                        pressing?.join()
                        press.amount.animateTo(0f, KokeMotion.releaseSpring)
                    }
                }
                is PressInteraction.Cancel -> {
                    launch { press.amount.animateTo(0f, KokeMotion.releaseSpring) }
                }
            }
        }
    }
    return press
}

/** 押している間、わずかに縮める(苔ごと縮むよう、苔より外側に付ける)。 */
internal fun Modifier.kokeShrink(press: KokePress?, shrink: Float = KokeMotion.PRESS_SHRINK): Modifier =
    if (press == null) {
        this
    } else {
        graphicsLayer {
            val s = 1f - shrink * press.value
            scaleX = s
            scaleY = s
        }
    }

/**
 * 平らな面: 単色の塗り + 内側に収まる細い線。影もグラデーションも付けない。
 * 押している間は [pressedFill] へ少し寄る(色の変化は描くときに読むので、作り直しは起きない)。
 */
internal fun Modifier.kokeFace(
    shape: Shape,
    fill: Color,
    pressedFill: Color = fill,
    line: Color? = null,
    lineWidth: Dp = KokeSize.line,
    press: KokePress? = null,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val w = lineWidth.toPx()
    // 線は面の内側に収める(線の太さが変わっても部品の大きさが変わらない)
    val inner = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
    onDrawBehind {
        val p = press?.value ?: 0f
        drawOutline(outline, if (p == 0f) fill else lerp(fill, pressedFill, p))
        if (line != null && w > 0f) {
            translate(w / 2f, w / 2f) { drawOutline(inner, line, style = Stroke(w)) }
        }
    }
}

// ---- 部品 ----

/**
 * ボタン。平らな面に、はっきりした文字。
 * 主ボタン([KokeTone.Primary])は墨緑の面で、角に苔が 1 群れ生える(どの角か・形はボタンの文字から決まる種で変わる)。
 * 押すと面の色が少し変わり、わずかに縮む。苔は指の近くの株が沈む。押す・離すで短く振動し、確定でクリック音。
 *
 * @param contentColor 文字・アイコンの色。省略すると面に合う色
 */
@Composable
fun KokeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KokeTone = KokeTone.Primary,
    leadingIcon: ImageVector? = null,
    contentColor: Color = inkOn(tone),
) {
    val c = KokeTheme.colors
    val shape = KokeShapes.control
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKokePress(interaction, feedback)
    val seed = remember(text) { text.hashCode() }

    Row(
        modifier = modifier
            .heightIn(min = KokeSize.control)
            .kokeShrink(press)
            .then(
                when (tone) {
                    KokeTone.Primary -> Modifier
                        .kokeMoss(patches = KokeMossSpots.primaryButton(seed), shape = shape, seed = seed, interactionSource = interaction)
                        .kokeFace(shape, fill = c.primary, pressedFill = c.primaryPressed, press = press)
                    KokeTone.Secondary -> Modifier
                        .kokeFace(shape, fill = c.surface, pressedFill = c.surfacePressed, line = c.line, press = press)
                },
            )
            // 押した反応は面の色と縮みで返すので、標準の波紋は出さない
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Text(text = text, style = KokeType.button, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * アイコンだけの四角いボタン(角丸)。戻る・編集・削除・見た目の切り替えなど。
 * 主([KokeTone.Primary])は墨緑の面で、右上に小さく苔が生える。副は白い面に細い線。
 */
@Composable
fun KokeIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tone: KokeTone = KokeTone.Secondary,
    contentColor: Color = inkOn(tone),
) {
    val c = KokeTheme.colors
    val shape = KokeShapes.control
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKokePress(interaction, feedback)
    val seed = remember(icon) { icon.name.hashCode() }

    Box(
        modifier = modifier
            .size(KokeSize.iconButton)
            .kokeShrink(press, shrink = KokeMotion.PRESS_SHRINK * 1.5f)
            .then(
                when (tone) {
                    KokeTone.Primary -> Modifier
                        .kokeMoss(patches = KokeMossSpots.primaryIcon, shape = shape, seed = seed, interactionSource = interaction)
                        .kokeFace(shape, fill = c.primary, pressedFill = c.primaryPressed, press = press)
                    KokeTone.Secondary -> Modifier
                        .kokeFace(shape, fill = c.surface, pressedFill = c.surfacePressed, line = c.line, press = press)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(22.dp))
    }
}

/**
 * 入力欄。平らな面に細い線。上にラベル、下にエラーの注記。
 * 入力中は線が墨色になって少し太くなり、エラーのときは線とラベルが危険色になる。苔は生えない。
 */
@Composable
fun KokeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = KokeTheme.colors
    val shape = KokeShapes.field
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val lineColor by animateColorAsState(
        targetValue = when {
            isError -> c.danger
            focused -> c.ink
            else -> c.line
        },
        animationSpec = tween(KokeMotion.CHANGE_MILLIS),
        label = "kokeFieldLine",
    )
    val lineWidth by animateDpAsState(
        targetValue = if (focused || isError) KokeSize.lineStrong else KokeSize.line,
        animationSpec = tween(KokeMotion.CHANGE_MILLIS),
        label = "kokeFieldLineWidth",
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) c.ink else c.inkFaint,
        animationSpec = tween(KokeMotion.CHANGE_MILLIS),
        label = "kokeFieldIcon",
    )

    Column(modifier = modifier) {
        Text(
            text = label,
            style = KokeType.label,
            color = if (isError) c.danger else c.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp),
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError && errorText != null) error(errorText)
                },
            singleLine = true,
            textStyle = KokeType.field.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KokeSize.control)
                        .kokeFace(shape, fill = c.surface, line = lineColor, lineWidth = lineWidth)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(imageVector = leadingIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = KokeType.field, color = c.inkFaint, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            },
        )
        if (isError && errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorText, style = KokeType.caption, color = c.danger, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

/**
 * 画面上部。戻るは左の四角いアイコンボタン、タイトル(明朝)はその右、右端に [actions]。
 * 状態バーは自分で避ける。
 */
@Composable
fun KokeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (onBack != null) {
            KokeIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = KokeType.title,
            color = KokeTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actions != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** 文字だけの案内(一覧が空のときなど)。白い面に細い線、苔は生えない。 */
@Composable
fun KokeMessage(text: String, modifier: Modifier = Modifier) {
    val c = KokeTheme.colors
    Text(
        text = text,
        style = KokeType.body,
        color = c.inkMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .kokeFace(KokeShapes.card, fill = c.surface, line = c.line)
            .padding(horizontal = 28.dp, vertical = 24.dp),
    )
}

/**
 * 選択肢。左に印(選んでいるものは墨緑の丸にチェック、選んでいないものは細い線の丸)、その右に文字。
 * 選んでいるものは線が墨色で少し太くなり、右の辺に小さく苔が生える。
 */
@Composable
fun KokeChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KokeTheme.colors
    val shape = KokeShapes.control
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKokePress(interaction, feedback)
    val seed = remember(text) { text.hashCode() * 31 + 7 }

    Row(
        modifier = modifier
            .heightIn(min = KokeSize.control)
            .kokeShrink(press, shrink = KokeMotion.PRESS_SHRINK * 0.6f)
            .then(
                if (selected) {
                    Modifier.kokeMoss(patches = KokeMossSpots.selectedChoice, shape = shape, seed = seed, interactionSource = interaction)
                } else {
                    Modifier
                },
            )
            .kokeFace(
                shape,
                fill = c.surface,
                pressedFill = c.surfacePressed,
                line = if (selected) c.ink else c.line,
                lineWidth = if (selected) KokeSize.lineStrong else KokeSize.line,
                press = press,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    feedback.tick()
                    onClick()
                },
            )
            .semantics { this.selected = selected }
            // 右端は苔の分を空ける
            .padding(start = 16.dp, top = 14.dp, end = 40.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceMark(selected)
        Text(text = text, style = KokeType.button, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 選択肢の左の印。 */
@Composable
private fun ChoiceMark(selected: Boolean) {
    val c = KokeTheme.colors
    if (selected) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(c.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(14.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(KokeSize.lineStrong, c.line, CircleShape),
        )
    }
}

/** ダイアログ。白い面に角丸、ごく薄い影。苔は生えない。中身は [content] に縦に並べる。 */
@Composable
fun KokeDialog(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        // 影と、中の部品の苔がはみ出す分の余白
        Box(modifier = Modifier.padding(16.dp)) {
            KokeDialogPanel(title = title, message = message, content = content)
        }
    }
}

/** ダイアログの面と中身(窓を開かずに描けるよう分けている。カタログでも使う)。 */
@Composable
internal fun KokeDialogPanel(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KokeTheme.colors
    val shape = KokeShapes.dialog
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .shadow(elevation = 10.dp, shape = shape, clip = false, ambientColor = c.ink.copy(alpha = 0.12f), spotColor = c.ink.copy(alpha = 0.16f))
            .kokeFace(shape, fill = c.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = title, style = KokeType.dialogTitle, color = c.ink)
        if (message != null) {
            Text(text = message, style = KokeType.body, color = c.inkMuted)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/**
 * 取り消せない操作の確認。[キャンセル] [確定] をどちらも白い面で並べ、確定は危険色の文字にする
 * (墨緑の面と苔は「進める」操作の印なので、取り消せない操作には使わない)。
 */
@Composable
fun KokeConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KokeDialog(title = title, message = message, onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KokeButton(
                text = dismissLabel,
                onClick = onDismiss,
                tone = KokeTone.Secondary,
                modifier = Modifier.weight(1f),
            )
            KokeButton(
                text = confirmLabel,
                onClick = onConfirm,
                tone = KokeTone.Secondary,
                contentColor = KokeTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
