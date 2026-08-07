package com.cnsoftware.synapse.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cnsoftware.synapse.util.combineDateTime
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_ok
import synapse.shared.generated.resources.reminder_next
import synapse.shared.generated.resources.reminder_pick_time
import org.jetbrains.compose.resources.stringResource

/**
 * İki adımlı hatırlatıcı seçici: önce tarih, sonra saat.
 * Sonuç epoch millis olarak onConfirm'e verilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    if (selectedDateMillis == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { selectedDateMillis = datePickerState.selectedDateMillis },
                    enabled = datePickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(Res.string.reminder_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.reminder_pick_time)) },
            text = {
                Column { TimePicker(state = timePickerState) }
            },
            confirmButton = {
                TextButton(onClick = {
                    val millis = combineDateTime(
                        dateUtcMidnightMillis = selectedDateMillis!!,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                    )
                    onConfirm(millis)
                }) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDateMillis = null }) { Text(stringResource(Res.string.action_back)) }
            },
        )
    }
}
