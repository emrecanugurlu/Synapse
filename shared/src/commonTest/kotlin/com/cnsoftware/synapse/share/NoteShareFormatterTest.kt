package com.cnsoftware.synapse.share

import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteShareFormatterTest {

    private fun note(title: String, content: String) = Note(
        id = "1",
        title = title,
        content = content,
        color = NoteColor.DEFAULT,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun plainNoteIncludesTitleAndContent() {
        val text = formatNoteForShare(
            title = "Başlık",
            content = "İçerik satırı",
            isChecklist = false,
            checklistItems = emptyList(),
        )
        assertEquals("Başlık\n\nİçerik satırı", text)
    }

    @Test
    fun blankTitleIsOmitted() {
        val text = formatNoteForShare(
            title = "",
            content = "Sadece içerik",
            isChecklist = false,
            checklistItems = emptyList(),
        )
        assertEquals("Sadece içerik", text)
    }

    @Test
    fun checklistUsesCheckboxSymbolsAndSkipsBlankItems() {
        val text = formatNoteForShare(
            title = "Alışveriş",
            content = "",
            isChecklist = true,
            checklistItems = listOf(
                ChecklistItem(id = "1", text = "süt", isChecked = true, position = 0),
                ChecklistItem(id = "2", text = "", isChecked = false, position = 1),
                ChecklistItem(id = "3", text = "ekmek", isChecked = false, position = 2),
            ),
        )
        assertEquals("Alışveriş\n\n☑ süt\n☐ ekmek", text)
    }

    @Test
    fun multipleNotesAreJoinedWithSeparator() {
        val text = listOf(note("Bir", "a"), note("İki", "b")).toShareText()
        assertEquals("Bir\n\na\n\n———\n\nİki\n\nb", text)
    }
}
