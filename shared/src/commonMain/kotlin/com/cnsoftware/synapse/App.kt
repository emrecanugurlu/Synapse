package com.cnsoftware.synapse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.ThemeMode
import com.cnsoftware.synapse.ui.archive.ArchiveScreen
import com.cnsoftware.synapse.ui.editor.NoteEditorScreen
import com.cnsoftware.synapse.ui.labels.LabelsScreen
import com.cnsoftware.synapse.ui.notelist.NoteListScreen
import com.cnsoftware.synapse.ui.settings.SettingsScreen
import com.cnsoftware.synapse.ui.theme.SynapseTheme
import com.cnsoftware.synapse.ui.trash.TrashScreen
import com.cnsoftware.synapse.util.generateId
import org.koin.compose.koinInject

private sealed interface Screen {
    data object List : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Labels : Screen
    data object Settings : Screen

    // editorKey: var olan not için noteId, yeni not için her seferinde taze üretilen
    // benzersiz bir anahtar -- ardışık iki "yeni not" oturumunun aynı ViewModel
    // örneğini (ve dolayısıyla eski state'i) paylaşmasını engeller.
    data class Editor(
        val noteId: String?,
        val editorKey: String,
        val startAsChecklist: Boolean = false,
    ) : Screen
}

@Composable
@Preview
fun App(settings: AppSettings = koinInject()) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SynapseTheme(darkTheme = darkTheme) {
        var screen by remember { mutableStateOf<Screen>(Screen.List) }

        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { current ->
            when (current) {
                is Screen.List -> NoteListScreen(
                    onNoteClick = { id -> screen = Screen.Editor(noteId = id, editorKey = id) },
                    onAddNote = { screen = Screen.Editor(noteId = null, editorKey = generateId()) },
                    onAddChecklist = {
                        screen = Screen.Editor(
                            noteId = null,
                            editorKey = generateId(),
                            startAsChecklist = true,
                        )
                    },
                    onOpenArchive = { screen = Screen.Archive },
                    onOpenTrash = { screen = Screen.Trash },
                    onOpenLabels = { screen = Screen.Labels },
                    onOpenSettings = { screen = Screen.Settings },
                )
                is Screen.Labels -> LabelsScreen(
                    onBack = { screen = Screen.List },
                )
                is Screen.Settings -> SettingsScreen(
                    onBack = { screen = Screen.List },
                )
                is Screen.Archive -> ArchiveScreen(
                    onBack = { screen = Screen.List },
                    onNoteClick = { id -> screen = Screen.Editor(noteId = id, editorKey = id) },
                )
                is Screen.Trash -> TrashScreen(
                    onBack = { screen = Screen.List },
                    onNoteClick = { id -> screen = Screen.Editor(noteId = id, editorKey = id) },
                )
                is Screen.Editor -> NoteEditorScreen(
                    noteId = current.noteId,
                    editorKey = current.editorKey,
                    startAsChecklist = current.startAsChecklist,
                    onBack = { screen = Screen.List },
                )
            }
        }
    }
}
