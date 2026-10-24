package com.example.businesscard.ui.moss

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
import com.example.businesscard.R
import com.example.businesscard.ui.glass.rememberGlassFeedback

/** ボタン・アイコンボタン・入力欄の高さ(タップしやすい 48dp 以上) */
private val ControlHeight = 52.dp

/** 部品の上の文字の色。苔の上は黒緑、石の上は淡い白緑。 */
@Composable
private fun inkOn(tone: MossTone): Color {
    val c = MossTheme.colors
    return if (tone == MossTone.Moss) c.onMoss else c.ink
}

/**
 * 苔のクッション、または石の板のボタン。見本の主ボタン(苔)と副ボタン(石)。
 * 押すと沈み込み(外の影が消え、少し下がる)、離すとばねで戻る。押す・離すで振動、確定でクリック音。
 *
 * @param contentColor 文字・アイコンの色。省略すると、面の種類に合う色
 */
@Composable
fun MossButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MossTone = MossTone.Moss,
    leadingIcon: ImageVector? = null,
    contentColor: Color = inkOn(tone),
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberMossPress(interaction, feedback)

    Row(
        modifier = modifier
            .heightIn(min = ControlHeight)
            .mossSurface(shape = MossShapes.control, tone = tone, pressed = press)
            // 押した反応は面そのもの(沈み込み)で返すので、標準の波紋は出さない
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .mossSink(press)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Text(text = text, style = MossType.button, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 丸いアイコンボタン。戻る・編集・削除など。石の板か、苔の丸いクッション。 */
@Composable
fun MossIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tone: MossTone = MossTone.Stone,
    contentColor: Color = inkOn(tone),
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberMossPress(interaction, feedback)

    Box(
        modifier = modifier
            .size(ControlHeight)
            .mossSurface(shape = CircleShape, tone = tone, pressed = press)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .mossSink(press),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(24.dp))
    }
}

/**
 * 入力欄。石の板に彫り込まれた溝で、入力中(フォーカス中)は縁が苔のように淡く光る。
 * エラー時は注記と項目名が危険色になる。
 */
@Composable
fun MossTextField(
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
    val c = MossTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MossType.label,
            color = if (isError) c.danger else c.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            textStyle = MossType.field.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ControlHeight)
                        .mossSurface(
                            shape = MossShapes.field,
                            tone = MossTone.Stone,
                            sunken = true,
                            glow = if (focused) 1f else 0f,
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = c.inkMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = MossType.field, color = c.inkFaint, maxLines = 1)
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
                style = MossType.caption,
                color = c.danger,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** 文字だけの案内(一覧が空のときなど)。石の板に載せる。 */
@Composable
fun MossMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MossType.body,
        color = MossTheme.colors.ink,
        textAlign = TextAlign.Center,
        modifier = modifier
            .mossSurface(shape = MossShapes.card, tone = MossTone.Stone)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    )
}

/**
 * 選択肢。選んでいるものは苔のクッションにチェック、選んでいないものは石の板に空の丸。
 */
@Composable
fun MossChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MossButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        tone = if (selected) MossTone.Moss else MossTone.Stone,
        leadingIcon = if (selected) Icons.Filled.Check else RingIcon,
    )
}

/** ダイアログの板(石)。中身は [content] に縦に並べる。 */
@Composable
fun MossDialog(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MossTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        // 影が窓の外で切れないよう、周りに余白を取る
        Box(modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 20.dp)) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .mossSurface(shape = MossShapes.dialog, tone = MossTone.Stone)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MossType.dialogTitle,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )
                if (message != null) {
                    Text(text = message, style = MossType.body, color = c.inkMuted)
                }
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
    }
}

/**
 * 取り消せない操作の確認。[キャンセル] [確定] をどちらも石の板で並べ、確定は危険色(杉の赤茶)の文字にする
 * (苔のクッションは「進める」操作に使う色なので、取り消せない操作には使わない)。
 */
@Composable
fun MossConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MossDialog(title = title, message = message, onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MossButton(
                text = dismissLabel,
                onClick = onDismiss,
                tone = MossTone.Stone,
                modifier = Modifier.weight(1f),
            )
            MossButton(
                text = confirmLabel,
                onClick = onConfirm,
                tone = MossTone.Stone,
                contentColor = MossTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 画面上部。戻るは左の丸いボタン、タイトルはその右、右端に [actions]。 */
@Composable
fun MossTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            MossIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = MossType.title,
            color = MossTheme.colors.ink,
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

/** 選んでいない選択肢の空の丸。 */
private val RingIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MossRing",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(12f, 4f)
            arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 16f)
            arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -16f)
            close()
        }
    }.build()
}
