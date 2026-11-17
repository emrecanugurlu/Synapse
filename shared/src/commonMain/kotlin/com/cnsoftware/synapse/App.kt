package com.cnsoftware.synapse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.ThemeMode
import com.cnsoftware.synapse.ui.archive.ArchiveScreen
import com.cnsoftware.synapse.ui.common.AppDrawer
import com.cnsoftware.synapse.ui.common.DrawerDestination
import com.cnsoftware.synapse.ui.editor.NoteEditorScreen
import com.cnsoftware.synapse.ui.labels.LabelsScreen
import com.cnsoftware.synapse.ui.notelist.NoteListScreen
import com.cnsoftware.synapse.ui.notelist.NoteListViewModel
import com.cnsoftware.synapse.ui.settings.SettingsScreen
import com.cnsoftware.synapse.ui.theme.SynapseTheme
import com.cnsoftware.synapse.ui.trash.TrashScreen
import com.cnsoftware.synapse.util.generateId
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private sealed interface Screen {
    data object List : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Labels : Screen
    data object Settings : Screen

    // editorKey: var olan not için noteId, yeni not için her seferinde taze üretilen
    // benzersiz bir anahtar -- ardışık iki "yeni not" oturumunun aynı ViewModel
    // örneğini (ve dolayısıyla eski state'i) paylaşmasını engeller.
    //
    // origin: editörün hangi listeden açıldığı. Geri dönüşte oraya dönülüyor;
    // sabit olarak listeye dönmek, arşivdeki bir notu açan kullanıcıyı
    // arşivden çıkarıyordu.
    data class Editor(
        val noteId: String?,
        val editorKey: String,
        val startAsChecklist: Boolean = false,
        val origin: Screen = List,
        val initialTitle: String = "",
        val initialContent: String = "",
    ) : Screen
}

private fun Screen.tag(): String = when (this) {
    is Screen.List -> "list"
    is Screen.Archive -> "archive"
    is Screen.Trash -> "trash"
    is Screen.Labels -> "labels"
    is Screen.Settings -> "settings"
    is Screen.Editor -> "editor"
}

private fun screenFromTag(tag: String): Screen = when (tag) {
    "archive" -> Screen.Archive
    "trash" -> Screen.Trash
    "labels" -> Screen.Labels
    "settings" -> Screen.Settings
    else -> Screen.List
}

/**
 * Paylaşılan metin kayıtlı duruma bu boydan uzunsa yazılmaz. Android kayıtlı
 * durumu Binder üzerinden taşıyor (toplam ~1 MB sınırı); çok uzun bir metin
 * uygulamayı arka plana alırken çökertebilir.
 */
private const val MAX_SAVED_PREFILL_CHARS = 20_000

/**
 * Gezinme durumunu ekran döndürme, tema/dil değişimi ve süreç ölümünde korur.
 * Bu olmadan Activity her yeniden kurulduğunda kullanıcı editörden listeye atılıyordu.
 */
private val ScreenSaver: Saver<Screen, Any> = listSaver<Screen, String>(
    save = { screen ->
        if (screen is Screen.Editor) {
            val prefillFits =
                screen.initialTitle.length + screen.initialContent.length <= MAX_SAVED_PREFILL_CHARS
            listOf(
                screen.tag(),
                // Not id'leri hiçbir zaman boş değil; boş dize "yeni not" demek.
                screen.noteId.orEmpty(),
                screen.editorKey,
                screen.startAsChecklist.toString(),
                screen.origin.tag(),
                if (prefillFits) screen.initialTitle else "",
                if (prefillFits) screen.initialContent else "",
            )
        } else {
            listOf(screen.tag())
        }
    },
    restore = { saved ->
        if (saved.firstOrNull() == "editor" && saved.size == 7) {
            Screen.Editor(
                noteId = saved[1].ifEmpty { null },
                editorKey = saved[2],
                startAsChecklist = saved[3].toBoolean(),
                origin = screenFromTag(saved[4]),
                initialTitle = saved[5],
                initialContent = saved[6],
            )
        } else {
            screenFromTag(saved.firstOrNull().orEmpty())
        }
    },
)

private fun Screen.toDrawerDestination(): DrawerDestination = when (this) {
    is Screen.Archive -> DrawerDestination.ARCHIVE
    is Screen.Trash -> DrawerDestination.TRASH
    is Screen.Labels -> DrawerDestination.LABELS
    is Screen.Settings -> DrawerDestination.SETTINGS
    // Editördeyken çekmece, notun geldiği bölümü işaretli göstersin.
    is Screen.Editor -> origin.toDrawerDestination()
    is Screen.List -> DrawerDestination.NOTES
}

@Composable
@Preview
fun App(
    settings: AppSettings = koinInject(),
    /**
     * Uygulama dışından gelen açılış isteği. Her yeni intent'te farklı bir
     * nesne gelmeli ki `LaunchedEffect` yeniden tetiklensin.
     */
    startAction: StartAction? = null,
    /**
     * İstek işlendikten sonra çağrılır; platform katmanı isteği sıfırlamalı.
     * Sıfırlanmazsa aynı istek ikinci kez geldiğinde (ör. "Yeni not" kısayoluna
     * art arda iki kez dokunmak) değer değişmediği için hiçbir şey olmuyordu.
     */
    onStartActionHandled: () -> Unit = {},
    repository: NoteRepository = koinInject(),
) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SynapseTheme(darkTheme = darkTheme) {
        var screen by rememberSaveable(stateSaver = ScreenSaver) {
            mutableStateOf<Screen>(Screen.List)
        }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Etiket filtresi not listesine ait; drawer da onu okuyup yazıyor.
        // Aynı ViewModelStoreOwner altında olduğumuz için bu, NoteListScreen'in
        // kullandığı örneğin ta kendisi.
        val listViewModel: NoteListViewModel = koinViewModel()
        val labels by listViewModel.labels.collectAsStateWithLifecycle()
        val selectedLabelId by listViewModel.selectedLabelId.collectAsStateWithLifecycle()

        val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

        // Çekmece açıkken geri tuşu onu kapatır; ekranların kendi geri
        // davranışına inmez. Drawer artık burada olduğu için bu da burada.
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        // Kısayol veya paylaşımla açıldıysa doğrudan editöre git.
        // Anahtar startAction: aynı istek iki kez işlenmesin, yenisi işlensin.
        LaunchedEffect(startAction) {
            when (startAction) {
                null -> Unit
                is StartAction.NewNote -> screen = Screen.Editor(
                    noteId = null,
                    editorKey = generateId(),
                )
                is StartAction.NewChecklist -> screen = Screen.Editor(
                    noteId = null,
                    editorKey = generateId(),
                    startAsChecklist = true,
                )
                is StartAction.ShareText -> screen = Screen.Editor(
                    noteId = null,
                    editorKey = generateId(),
                    initialTitle = startAction.title,
                    initialContent = startAction.text,
                )
                is StartAction.OpenNote -> {
                    // Bildirim gösterildikten sonra not kalıcı silinmiş olabilir;
                    // o durumda boş bir editör açmak yerine olduğumuz yerde kalıyoruz.
                    val note = repository.getNoteById(startAction.noteId)
                    if (note != null) {
                        screen = Screen.Editor(
                            noteId = note.id,
                            editorKey = note.id,
                            // Geri dönüşte notun bulunduğu bölüme gidilsin.
                            origin = when {
                                note.isDeleted -> Screen.Trash
                                note.isArchived -> Screen.Archive
                                else -> Screen.List
                            },
                        )
                    }
                }
            }
            // En sonda: bu çağrı anahtarı değiştirip bu bloğu iptal ediyor.
            if (startAction != null) onStartActionHandled()
        }

        fun navigate(target: Screen) {
            scope.launch { drawerState.close() }
            screen = target
        }

        AppDrawer(
            drawerState = drawerState,
            current = screen.toDrawerDestination(),
            labels = labels,
            selectedLabelId = selectedLabelId,
            // Editör ve Ayarlar detay ekranı: kenardan çekmece açılmasın.
            gesturesEnabled = screen !is Screen.Editor && screen !is Screen.Settings,
            onOpenNotes = { labelId ->
                listViewModel.setLabelFilter(labelId)
                navigate(Screen.List)
            },
            onOpenArchive = { navigate(Screen.Archive) },
            onOpenTrash = { navigate(Screen.Trash) },
            onOpenLabels = { navigate(Screen.Labels) },
            onOpenSettings = { navigate(Screen.Settings) },
        ) {
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
                        onOpenDrawer = openDrawer,
                        viewModel = listViewModel,
                    )
                    is Screen.Labels -> LabelsScreen(
                        onOpenDrawer = openDrawer,
                        onBack = { screen = Screen.List },
                    )
                    is Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.List },
                    )
                    is Screen.Archive -> ArchiveScreen(
                        onOpenDrawer = openDrawer,
                        onBack = { screen = Screen.List },
                        onNoteClick = { id ->
                            screen = Screen.Editor(
                                noteId = id,
                                editorKey = id,
                                origin = Screen.Archive,
                            )
                        },
                    )
                    is Screen.Trash -> TrashScreen(
                        onOpenDrawer = openDrawer,
                        onBack = { screen = Screen.List },
                        onNoteClick = { id ->
                            screen = Screen.Editor(
                                noteId = id,
                                editorKey = id,
                                origin = Screen.Trash,
                            )
                        },
                    )
                    is Screen.Editor -> NoteEditorScreen(
                        noteId = current.noteId,
                        editorKey = current.editorKey,
                        startAsChecklist = current.startAsChecklist,
                        initialTitle = current.initialTitle,
                        initialContent = current.initialContent,
                        // Not nereden açıldıysa oraya dönülüyor.
                        onBack = { screen = current.origin },
                    )
                }
            }
        }
    }
}
