package com.cnsoftware.synapse.ui.editor

/**
 * Editör ViewModel'inin kuruluş argümanları.
 *
 * Koin'e tek tek gönderilmiyor: `noteId`, `initialTitle` ve `initialContent`
 * üçü de String olduğu için tip bazlı çözümleme hangisinin hangisi olduğunu
 * ayırt edemiyordu. Tek bir tipli nesne bu belirsizliği ortadan kaldırıyor.
 */
data class EditorArgs(
    val noteId: String? = null,
    val startAsChecklist: Boolean = false,
    val initialTitle: String = "",
    val initialContent: String = "",
    /** Yeni notun alacağı id; var olan not açılırken null. */
    val newNoteId: String? = null,
)
