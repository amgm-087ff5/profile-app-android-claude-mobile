package com.example.businesscard.ui.kinu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
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
 * Kinu の全部品を並べた確認用画面。Android Studio のプレビューと、スクリーンショットテストの両方で使う。
 */
@Composable
fun KinuCatalog(modifier: Modifier = Modifier) {
    val c = KinuTheme.colors
    var text by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val sample = BusinessCard(
        id = 1L,
        name = "Sample Name",
        company = "Sample Co., Ltd.",
        title = "Position",
        phone = "03-0000-0000",
        email = "sample@example.com",
    )

    KinuBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KinuTopBar(title = "Catalog", onBack = {})

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KinuButton(text = "Gold", onClick = {}, modifier = Modifier.weight(1f))
                KinuButton(text = "Night", onClick = {}, tone = KinuTone.Night, modifier = Modifier.weight(1f))
            }
            KinuButton(text = "Loading", onClick = {})
            KinuButton(text = "Leading icon", onClick = {}, leadingIcon = Icons.Filled.Check)
            KinuButton(text = "Disabled", onClick = {}, enabled = false)
            KinuButton(text = "Danger", onClick = {}, tone = KinuTone.Night, contentColor = c.danger)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KinuIconButton(onClick = {}, icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                KinuIconButton(
                    onClick = {},
                    icon = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tone = KinuTone.Gold,
                )
            }

            KinuTextField(
                value = text,
                onValueChange = { text = it },
                label = "Text field",
                placeholder = "Name",
                leadingIcon = Icons.Outlined.Person,
            )
            KinuTextField(
                value = "",
                onValueChange = {},
                label = "Error",
                isError = true,
                errorText = "Required",
            )

            KinuChoice(text = "Choice A", selected = choice == 0, onClick = { choice = 0 })
            KinuChoice(text = "Choice B", selected = choice == 1, onClick = { choice = 1 })

            KinuMessage(text = "Message")

            KinuBusinessCard(card = sample, onClick = {})

            KinuButton(text = "Dialog", onClick = { showDialog = true }, tone = KinuTone.Night)
            KinuButton(text = "Confirm", onClick = { showConfirm = true }, tone = KinuTone.Night)
        }
    }

    if (showDialog) {
        KinuDialog(title = "Dialog", message = "Message", onDismiss = { showDialog = false }) {
            KinuButton(text = "Close", onClick = { showDialog = false }, tone = KinuTone.Night)
        }
    }
    if (showConfirm) {
        KinuConfirmDialog(
            title = "Confirm",
            message = "Message",
            confirmLabel = "OK",
            dismissLabel = "Cancel",
            onConfirm = { showConfirm = false },
            onDismiss = { showConfirm = false },
        )
    }
}

@Preview(name = "Kinu catalog", widthDp = 412, heightDp = 892)
@Composable
private fun KinuCatalogPreview() {
    KinuCatalog()
}
