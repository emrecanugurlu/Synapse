package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Uygulamanın metin alanı dili: dolu, tamamen yuvarlak, alt çizgisiz.
 *
 * Varsayılan `TextField` altı çizili "kutu" görünümüyle geliyor; bu, arama
 * hapı ve yuvarlak kartlarla kurulan Expressive diliyle çelişiyor.
 *
 * Zemin `surfaceContainerHighest` + ince kenarlık: bir alt ton olan
 * `surfaceContainerHigh` açık temada ekran zeminiyle neredeyse aynıydı ve alan
 * görünmez oluyordu. Kenarlık, tonlar birbirine yaklaştığında sınırı garanti eder.
 */
@Composable
fun RoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHighest

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            shape = CircleShape,
        ),
    )
}
