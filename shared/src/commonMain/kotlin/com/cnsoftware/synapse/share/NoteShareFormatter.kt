package com.cnsoftware.synapse.share

import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note

fun formatNoteForShare(
    title: String,
    content: String,
    isChecklist: Boolean,
    checklistItems: List<ChecklistItem>,
): String = buildString {
    if (title.isNotBlank()) {
        appendLine(title)
        appendLine()
    }
    if (isChecklist) {
        checklistItems
            .filter { it.text.isNotBlank() }
            .forEach { item ->
                appendLine(if (item.isChecked) "☑ ${item.text}" else "☐ ${item.text}")
            }
    } else {
        append(content)
    }
}.trim()

fun Note.toShareText(): String =
    formatNoteForShare(title, content, isChecklist, checklistItems)

fun List<Note>.toShareText(): String =
    joinToString(separator = "\n\n———\n\n") { it.toShareText() }
