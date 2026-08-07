package com.cnsoftware.synapse.ui.common

import org.jetbrains.compose.resources.StringResource

/**
 * Snackbar üzerinden geri alınabilir bir işlem.
 *
 * Mesaj, ViewModel'de metne çevrilmiş hâlde değil, kaynak kimliği olarak taşınır;
 * çeviri UI katmanında yapılır. `id` her olay için benzersizdir, böylece aynı
 * mesajlı ardışık iki olay LaunchedEffect tarafından "değişmedi" sanılmaz.
 */
class UndoEvent(
    val id: Long,
    val message: StringResource,
    val count: Int,
    val undo: suspend () -> Unit,
)
