package com.cnsoftware.synapse.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_undo
import org.jetbrains.compose.resources.stringResource

/**
 * UndoEvent akışını verilen SnackbarHostState'e bağlar:
 * olay geldiğinde "Geri al" aksiyonlu snackbar gösterir.
 */
@Composable
fun UndoSnackbarEffect(
    event: UndoEvent?,
    snackbarHostState: SnackbarHostState,
    onUndo: (UndoEvent) -> Unit,
    onDismiss: (UndoEvent) -> Unit,
) {
    // Metin, LaunchedEffect'e girmeden önce kompozisyonda çözümlenmeli.
    val message = event?.let { stringResource(it.message, it.count) }
    val undoLabel = stringResource(Res.string.action_undo)

    LaunchedEffect(event) {
        if (event == null || message == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo(event)
        } else {
            onDismiss(event)
        }
    }
}
