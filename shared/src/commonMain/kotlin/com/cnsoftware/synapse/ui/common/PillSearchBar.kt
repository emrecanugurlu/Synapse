package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.ui.theme.Spacing
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_clear
import synapse.shared.generated.resources.search_close
import org.jetbrains.compose.resources.stringResource

/**
 * Uygulamanın ortak üst çubuğu: başlık yerine yuvarlak bir arama hapı.
 *
 * Pasif hâlde [navigationIcon] + arama metni + [actions] gösterir;
 * arama alanına dokunulunca aynı hap düzenlenebilir alana dönüşür
 * (ayrı bir arama ekranına geçilmez).
 */
@Composable
fun PillSearchBar(
    query: String,
    searchActive: Boolean,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onActivateSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) runCatching { focusRequester.requestFocus() }
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            // Edge-to-edge modda durum çubuğunun altına kaymamak için;
            // TopAppBar bunu kendisi yapıyordu, özel bileşende elle veriyoruz.
            .statusBarsPadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .height(64.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.sm),
        ) {
            if (searchActive) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.search_close))
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_clear))
                    }
                }
            } else {
                navigationIcon()
                Text(
                    text = query.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (query.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onActivateSearch)
                        .padding(vertical = Spacing.md),
                )
                actions()
            }
        }
    }
}
