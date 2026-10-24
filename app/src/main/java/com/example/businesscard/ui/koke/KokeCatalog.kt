package com.example.businesscard.ui.koke

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard

/**
 * Koke のカタログ。全部品を 1 画面に並べる(Android Studio のプレビューと、スクリーンショットテストで使う)。
 * 主ボタンの 1 つは文字が "Press"(押したままの撮影で、このボタンを探す)。
 *
 * 苔は部品の外へ最大 [MossOverhang] はみ出すので、段どうしの間隔はそれより広く取っている。
 */
@Composable
fun KokeCatalog(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("mail@") }
    var choice by remember { mutableStateOf(0) }

    KokeBackground(modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // 上のバーは自分で状態バーを避ける。右端に主のアイコンボタン(右上に小さく苔)と副のアイコンボタン
            KokeTopBar(
                title = "苔むす",
                actions = {
                    KokeIconButton(onClick = {}, icon = Icons.Outlined.Person, contentDescription = "Person")
                    KokeIconButton(onClick = {}, icon = Icons.Outlined.Edit, contentDescription = "Edit", tone = KokeTone.Primary)
                },
            )
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 32.dp),
                // 苔は最大 MossOverhang(10dp)はみ出すので、それより十分広く空ける
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 主ボタン(角に苔)と副ボタン(苔なし)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KokeButton(text = "Press", onClick = {}, modifier = Modifier.weight(1f))
                    KokeButton(text = "Cancel", onClick = {}, tone = KokeTone.Secondary, modifier = Modifier.weight(1f))
                }
                // 編集画面の下の操作と同じ並び: 削除(危険色の文字のアイコン)+ 主ボタン
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    KokeIconButton(
                        onClick = {},
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        contentColor = KokeTheme.colors.danger,
                    )
                    KokeButton(text = "名刺を追加", onClick = {}, leadingIcon = Icons.Outlined.Add, modifier = Modifier.weight(1f))
                }

                // 入力欄(ふつう / エラー)
                KokeTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "名前",
                    placeholder = "山田 太郎",
                    leadingIcon = Icons.Outlined.Person,
                    modifier = Modifier.fillMaxWidth(),
                )
                KokeTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "メール",
                    leadingIcon = Icons.Outlined.Email,
                    isError = true,
                    errorText = "メールアドレスの形で入力してください",
                    modifier = Modifier.fillMaxWidth(),
                )

                // 選択肢(選んでいるものの端に苔)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Edgelit", "Koke(苔むす)").forEachIndexed { i, label ->
                        KokeChoice(text = label, selected = choice == i, onClick = { choice = i }, modifier = Modifier.fillMaxWidth())
                    }
                }

                KokeBusinessCard(card = CatalogCard, onClick = {}, modifier = Modifier.fillMaxWidth())

                // ここから下は1画面に収まらないことがある(案内とダイアログは、空の一覧・見た目の切り替えの撮影でも確認できる)
                KokeMessage(text = "名刺はまだありません。下のボタンから追加できます。", modifier = Modifier.fillMaxWidth())

                // ダイアログの面(窓は開かずに、その場に描く)
                KokeDialogPanel(title = "名刺を削除しますか?", message = "削除すると元に戻せません。") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KokeButton(text = "キャンセル", onClick = {}, tone = KokeTone.Secondary, modifier = Modifier.weight(1f))
                        KokeButton(
                            text = "削除",
                            onClick = {},
                            tone = KokeTone.Secondary,
                            contentColor = KokeTheme.colors.danger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private val CatalogCard = BusinessCard(
    id = 3,
    name = "苔野 しずく",
    company = "Moss & Stone Atelier",
    title = "庭師",
    phone = "090-0000-1234",
    email = "shizuku@example.com",
)

@Preview(name = "Koke catalog", widthDp = 412, heightDp = 1600)
@Composable
private fun KokeCatalogPreview() {
    KokeTheme { KokeCatalog() }
}
