package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.selection_close
import synapse.shared.generated.resources.selection_count
import org.jetbrains.compose.resources.stringResource

/** "N seçildi" başlıklı seçim üst barı; aksiyonlar ekrana göre dışarıdan verilir. */
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(Res.string.selection_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.selection_close))
            }
        },
        actions = actions,
    )
}
