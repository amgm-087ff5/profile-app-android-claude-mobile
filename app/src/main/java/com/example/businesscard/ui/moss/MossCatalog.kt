package com.example.businesscard.ui.moss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard

/**
 * Moss のカタログ。主ボタン・副ボタン・アイコンボタン・入力欄(通常とエラー)・選択肢・名刺・案内を、
 * 岩の背景の上に縦に並べる。Android Studio のプレビューと、スクリーンショットテストの両方で使う。
 *
 * 主ボタンの文字は "Press"(押した状態の撮影で探すため)。
 */
@Composable
fun MossCatalog(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(0) }

    MossBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MossButton(text = "Press", onClick = {}, modifier = Modifier.weight(1f))
                MossButton(text = "Sub", onClick = {}, tone = MossTone.Stone, modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MossIconButton(onClick = {}, icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                MossIconButton(onClick = {}, icon = Icons.Outlined.Edit, contentDescription = "Edit", tone = MossTone.Moss)
                MossChoice(text = "Selected", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            }

            MossTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = "Name",
                leadingIcon = Icons.Outlined.Person,
            )

            MossTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "Email",
                isError = true,
                errorText = "Invalid address",
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MossChoice(text = "Moss", selected = picked == 0, onClick = { picked = 0 }, modifier = Modifier.weight(1f))
                MossChoice(text = "Stone", selected = picked == 1, onClick = { picked = 1 }, modifier = Modifier.weight(1f))
            }

            MossBusinessCard(card = SampleCard, onClick = {})

            MossMessage(text = "Nothing here yet", modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 見本用の名刺の中身(実在の人物や会社ではない架空の例)。 */
private val SampleCard = BusinessCard(
    id = 1L,
    name = "Taro Moss",
    company = "Moss Studio",
    title = "Gardener",
    phone = "03-0000-0000",
    email = "moss@example.com",
)

@Preview(name = "Moss catalog", widthDp = 412, heightDp = 892)
@Composable
private fun MossCatalogPreview() {
    MossTheme { MossCatalog() }
}
