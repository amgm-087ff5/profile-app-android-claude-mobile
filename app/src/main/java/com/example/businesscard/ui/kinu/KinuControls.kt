package com.example.businesscard.ui.kinu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.businesscard.R
import com.example.businesscard.ui.glass.rememberGlassFeedback

/** 部品の色の種類。Gold は金の帯(主)、Night は夜のガラス(副)。 */
enum class KinuTone { Gold, Night }

/** 面の上の文字色。金の帯は焦げ茶(ink)、夜のガラスはシャンパン。 */
@Composable
private fun kinuTextOn(tone: KinuTone): Color =
    if (tone == KinuTone.Gold) KinuTheme.colors.ink else KinuTheme.colors.champagne

/** 主ボタン(金の帯)・副ボタン(夜のガラス)。押すと光の線が強まる。 */
@Composable
fun KinuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KinuTone = KinuTone.Gold,
    leadingIcon: ImageVector? = null,
    contentColor: Color = kinuTextOn(tone),
    enabled: Boolean = true,
) {
    val c = KinuTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKinuPress(interaction, feedback)
    val shape = KinuShapes.control
    val gold = tone == KinuTone.Gold && enabled
    val ink = if (enabled) contentColor else c.champagneMuted
    val surface = if (gold) {
        Modifier.kinuRibbon(
            shape,
            line = { KinuLight.LINE_GOLD + KinuLight.LINE_PRESS_BOOST * press.value },
            touch = { press.touch },
            seed = 0.31f,
        )
    } else {
        Modifier.kinuNightGlass(
            shape,
            line = { (if (enabled) KinuLight.LINE_NIGHT else 0f) + KinuLight.LINE_PRESS_BOOST * press.value },
            touch = { press.touch },
        )
    }

    Row(
        modifier = modifier
            .heightIn(min = KinuDimens.controlHeight)
            .then(surface)
            .then(if (enabled) Modifier.kinuTouch(press) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        }
        Text(text = text, style = KinuType.button, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 丸くない四角のアイコンボタン(角は 14dp)。金の帯か夜のガラス。 */
@Composable
fun KinuIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tone: KinuTone = KinuTone.Night,
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKinuPress(interaction, feedback)
    val shape = KinuShapes.control
    val ink = kinuTextOn(tone)
    val surface = if (tone == KinuTone.Gold) {
        Modifier.kinuRibbon(
            shape,
            line = { KinuLight.LINE_GOLD + KinuLight.LINE_PRESS_BOOST * press.value },
            touch = { press.touch },
            seed = 0.52f,
        )
    } else {
        Modifier.kinuNightGlass(
            shape,
            line = { KinuLight.LINE_NIGHT + KinuLight.LINE_PRESS_BOOST * press.value },
            touch = { press.touch },
        )
    }
    Box(
        modifier = modifier
            .size(KinuDimens.controlHeight)
            .then(surface)
            .kinuTouch(press)
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
        Icon(imageVector = icon, contentDescription = contentDescription, tint = ink, modifier = Modifier.size(24.dp))
    }
}

/** 入力欄などの上に置く小さなラベル。字間は広め。 */
@Composable
private fun KinuLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text = text, style = KinuType.label, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/**
 * 入力欄。夜のガラスの中に入力する。入力中は光の線が強まり、奥が琥珀ににじむ。エラーは注記と文字が危険色。
 */
@Composable
fun KinuTextField(
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
    val c = KinuTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier) {
        KinuLabel(
            text = label,
            color = if (isError) c.danger else c.champagneMuted,
            modifier = Modifier.padding(start = 6.dp),
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
            textStyle = KinuType.field.copy(color = c.champagne),
            cursorBrush = SolidColor(c.champagne),
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KinuDimens.controlHeight)
                        .kinuNightGlass(
                            KinuShapes.control,
                            line = { if (focused || isError) KinuLight.LINE_FOCUSED else KinuLight.LINE_NIGHT },
                            ember = { if (focused) 0.6f else 0f },
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = c.champagneMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = KinuType.field, color = c.champagneMuted, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            },
        )
        if (isError && errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorText,
                style = KinuType.caption,
                color = c.danger,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** 画面上部。戻るは左のアイコンボタン、タイトルはその右、右端に [actions]。 */
@Composable
fun KinuTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            KinuIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = KinuType.title,
            color = KinuTheme.colors.champagne,
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

/** 文字だけの案内(一覧が空のときなど)。夜のガラスに載せる。 */
@Composable
fun KinuMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = KinuType.body,
        color = KinuTheme.colors.champagne,
        textAlign = TextAlign.Center,
        modifier = modifier
            .kinuNightGlass(KinuShapes.card, line = { KinuLight.LINE_NIGHT })
            .padding(horizontal = 24.dp, vertical = 20.dp),
    )
}

/**
 * 選択肢。選んでいるものは金の縁(rim)と小さな金の帯の印、選んでいないものは夜のガラスだけ。
 */
@Composable
fun KinuChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KinuTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKinuPress(interaction, feedback)
    val shape = KinuShapes.control
    Row(
        modifier = modifier
            .heightIn(min = KinuDimens.controlHeight)
            .kinuNightGlass(
                shape,
                line = { (if (selected) KinuLight.LINE_FOCUSED else KinuLight.LINE_NIGHT) + KinuLight.LINE_PRESS_BOOST * press.value },
                touch = { press.touch },
            )
            .then(if (selected) Modifier.border(1.dp, c.goldLit, shape) else Modifier)
            .kinuTouch(press)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 20.dp)
                    .kinuRibbon(RoundedCornerShape(2.dp)),
            )
        }
        Text(text = text, style = KinuType.button, color = c.champagne, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * ダイアログ。夜のガラスの面に、上下に金の縁(リップ)。背景は暗い scrim。scrim を押すと閉じる。
 */
@Composable
fun KinuDialog(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KinuTheme.colors
    val lip = KinuDimens.lipHeight
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .kinuNightGlass(KinuShapes.dialog, line = { KinuLight.LINE_NIGHT })
                    // 面の中をタップしても閉じないよう、タップを受け取って消費する
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = lip + 20.dp, end = 24.dp, bottom = lip + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = title, style = KinuType.dialogTitle, color = c.champagne)
                    if (message != null) {
                        Text(text = message, style = KinuType.body, color = c.champagneMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                    content()
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(lip)
                        .kinuRibbon(RectangleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(lip)
                        .kinuRibbon(RectangleShape),
                )
            }
        }
    }
}

/**
 * 取り消せない操作の確認。[キャンセル] [確定] をどちらも夜のガラスで並べ、確定は危険色の文字にする。
 */
@Composable
fun KinuConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    KinuDialog(title = title, message = message, onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KinuButton(
                text = dismissLabel,
                onClick = onDismiss,
                tone = KinuTone.Night,
                modifier = Modifier.weight(1f),
            )
            KinuButton(
                text = confirmLabel,
                onClick = onConfirm,
                tone = KinuTone.Night,
                contentColor = KinuTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
