package com.cnsoftware.synapse.ui.notelist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.ActionsBottomSheet
import com.cnsoftware.synapse.ui.common.EmptyState
import com.cnsoftware.synapse.ui.common.LoadingPlaceholder
import com.cnsoftware.synapse.ui.common.NoteCard
import com.cnsoftware.synapse.ui.common.NoteSection
import com.cnsoftware.synapse.ui.common.PillSearchBar
import com.cnsoftware.synapse.ui.common.SectionedNoteGrid
import com.cnsoftware.synapse.ui.common.SelectionTopBar
import com.cnsoftware.synapse.ui.common.SheetActionRow
import com.cnsoftware.synapse.ui.common.UndoSnackbarEffect
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_close
import synapse.shared.generated.resources.action_ok
import synapse.shared.generated.resources.action_more
import synapse.shared.generated.resources.action_open_menu
import synapse.shared.generated.resources.card_duplicate
import synapse.shared.generated.resources.editor_labels
import synapse.shared.generated.resources.editor_share
import synapse.shared.generated.resources.empty_notes_description
import synapse.shared.generated.resources.empty_notes_title
import synapse.shared.generated.resources.empty_search_description
import synapse.shared.generated.resources.empty_search_title
import synapse.shared.generated.resources.fab_new
import synapse.shared.generated.resources.fab_new_checklist
import synapse.shared.generated.resources.fab_new_note
import synapse.shared.generated.resources.search_notes_hint
import synapse.shared.generated.resources.section_others
import synapse.shared.generated.resources.section_pinned
import synapse.shared.generated.resources.selection_archive
import synapse.shared.generated.resources.selection_delete
import synapse.shared.generated.resources.labels_empty_description
import synapse.shared.generated.resources.selection_label
import synapse.shared.generated.resources.selection_pin
import synapse.shared.generated.resources.selection_unpin
import synapse.shared.generated.resources.sort_color
import synapse.shared.generated.resources.sort_created
import synapse.shared.generated.resources.sort_title
import synapse.shared.generated.resources.sort_updated
import synapse.shared.generated.resources.view_switch_to_grid
import synapse.shared.generated.resources.view_switch_to_list
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoteListScreen(
    onNoteClick: (String) -> Unit,
    onAddNote: () -> Unit,
    onAddChecklist: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: NoteListViewModel = koinViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val selectedLabelId by viewModel.selectedLabelId.collectAsStateWithLifecycle()
    val undoEvent by viewModel.undoEvent.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val allSelectedPinned by viewModel.allSelectedPinned.collectAsStateWithLifecycle()
    val labelPickerOpen by viewModel.labelPickerOpen.collectAsStateWithLifecycle()
    val labelIdsOnAllSelected by viewModel.labelIdsOnAllSelected.collectAsStateWithLifecycle()
    val labelFiltersVisible by viewModel.labelFiltersVisible.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()
    var showSortSheet by remember { mutableStateOf(false) }
    var selectionMenuExpanded by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Seçim ya da arama moduna geçilince açık kalan FAB menüsünü kapat.
    LaunchedEffect(selectionMode, searchActive) {
        if (selectionMode || searchActive) fabExpanded = false
    }

    // Geri tuşu önce açık olan katmanı kapatır; hiçbiri açık değilse
    // sistemin devralmasına izin verilir (uygulamadan çıkış).
    val hasOpenLayer = selectionMode || searchActive || fabExpanded
    BackHandler(enabled = hasOpenLayer) {
        when {
            selectionMode -> viewModel.clearSelection()
            searchActive -> {
                searchActive = false
                viewModel.onSearchQueryChange("")
            }
            fabExpanded -> fabExpanded = false
        }
    }

    UndoSnackbarEffect(
        event = undoEvent,
        snackbarHostState = snackbarHostState,
        onUndo = viewModel::performUndo,
        onDismiss = viewModel::dismissUndo,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when {
                selectionMode -> SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClose = viewModel::clearSelection,
                ) {
                    // Birincil aksiyonlar barda; ikincil olanlar taşma menüsünde.
                    // Seçilenlerin hepsi sabitse buton "sabitlemeyi kaldır"a döner.
                    IconButton(onClick = viewModel::togglePinSelected) {
                        Icon(
                            imageVector = if (allSelectedPinned) {
                                Icons.Outlined.PushPin
                            } else {
                                Icons.Default.PushPin
                            },
                            contentDescription = stringResource(
                                if (allSelectedPinned) {
                                    Res.string.selection_unpin
                                } else {
                                    Res.string.selection_pin
                                },
                            ),
                        )
                    }
                    IconButton(onClick = viewModel::archiveSelected) {
                        Icon(Icons.Default.Archive, contentDescription = stringResource(Res.string.selection_archive))
                    }
                    IconButton(onClick = viewModel::trashSelected) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.selection_delete))
                    }
                    IconButton(onClick = { selectionMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.action_more))
                    }
                }
                else -> PillSearchBar(
                    query = query,
                    searchActive = searchActive,
                    placeholder = stringResource(Res.string.search_notes_hint),
                    onQueryChange = viewModel::onSearchQueryChange,
                    onActivateSearch = { searchActive = true },
                    onCloseSearch = {
                        searchActive = false
                        viewModel.onSearchQueryChange("")
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.action_open_menu))
                        }
                    },
                ) {
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (isGridView) stringResource(Res.string.view_switch_to_list) else stringResource(Res.string.view_switch_to_grid),
                        )
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(Res.string.sort_title))
                    }
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                ExpandableAddFab(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onAddNote = onAddNote,
                    onAddChecklist = onAddChecklist,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Şerit Ayarlar'dan açılıp kapatılıyor. Ancak bir filtre
            // uygulanmışken kapalı olsa bile gösteriliyor: aksi halde
            // kullanıcı aktif filtreyi ne görebilir ne de kaldırabilirdi.
            if (labels.isNotEmpty() && (labelFiltersVisible || selectedLabelId != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.screenPadding, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    labels.forEach { label ->
                        FilterChip(
                            selected = label.id == selectedLabelId,
                            onClick = { viewModel.onLabelFilterChange(label.id) },
                            label = { Text(label.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (label.color == NoteColor.DEFAULT) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    label.color.toContainerColor()
                                },
                            ),
                            leadingIcon = if (label.id == selectedLabelId) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            if (isLoading) {
                LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                return@Column
            }
            val pinnedNotes = notes.filter { it.isPinned }
            val otherNotes = notes.filterNot { it.isPinned }
            SectionedNoteGrid(
                sections = if (pinnedNotes.isEmpty()) {
                    listOf(NoteSection(title = null, notes = otherNotes))
                } else {
                    listOf(
                        NoteSection(title = stringResource(Res.string.section_pinned), notes = pinnedNotes),
                        NoteSection(title = stringResource(Res.string.section_others), notes = otherNotes),
                    )
                },
                empty = {
                    if (query.isNotBlank()) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = stringResource(Res.string.empty_search_title),
                            description = stringResource(Res.string.empty_search_description, query),
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.Lightbulb,
                            title = stringResource(Res.string.empty_notes_title),
                            description = stringResource(Res.string.empty_notes_description),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                // Adaptive yerine sabit 2 sütun: Adaptive, kullanılabilir genişliği
                // her ölçümde yeniden hesaplayıp sütun sayısını oynatabiliyor,
                // bu da kartların yer değiştirmesine yol açıyor.
                columns = if (isGridView) {
                    StaggeredGridCells.Fixed(2)
                } else {
                    StaggeredGridCells.Fixed(1)
                },
            ) { note ->
                SwipeableNoteCard(
                    note = note,
                    enabled = !selectionMode,
                    onArchive = { viewModel.archive(note) },
                    onTrash = { viewModel.moveToTrash(note) },
                ) {
                    NoteCard(
                        note = note,
                        onClick = {
                            if (selectionMode) viewModel.toggleSelection(note.id) else onNoteClick(note.id)
                        },
                        onLongClick = { viewModel.toggleSelection(note.id) },
                        selected = note.id in selectedIds,
                    )
                }
            }
        }
    }

    // Seçim modundaki ikincil işlemler: küçük açılır menü yerine alt sayfa.
    if (selectionMenuExpanded) {
        ActionsBottomSheet(onDismiss = { selectionMenuExpanded = false }) {
            SheetActionRow(
                icon = Icons.Default.Share,
                label = stringResource(Res.string.editor_share),
                onClick = {
                    selectionMenuExpanded = false
                    viewModel.shareSelected()
                },
            )
            SheetActionRow(
                icon = Icons.Default.ContentCopy,
                label = stringResource(Res.string.card_duplicate),
                onClick = {
                    selectionMenuExpanded = false
                    viewModel.duplicateSelected()
                },
            )
            SheetActionRow(
                icon = Icons.Default.Label,
                label = stringResource(Res.string.selection_label),
                onClick = {
                    // Seçim korunuyor: etiket seçici kapanınca temizlenecek.
                    selectionMenuExpanded = false
                    viewModel.openLabelPicker()
                },
            )
        }
    }

    if (labelPickerOpen) {
        BulkLabelDialog(
            labels = labels,
            checkedLabelIds = labelIdsOnAllSelected,
            onToggle = viewModel::toggleLabelForSelection,
            onDismiss = viewModel::closeLabelPicker,
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            selected = sortOption,
            onSelect = {
                viewModel.onSortOptionChange(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

/**
 * Toplu etiketleme kutusu.
 * İşaretli = etiket seçilen notların hepsinde var. Dokunmak hepsinden kaldırır;
 * işaretsizken (hiçbirinde ya da bir kısmında varken) hepsine ekler.
 */
@Composable
private fun BulkLabelDialog(
    labels: List<Label>,
    checkedLabelIds: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.editor_labels)) },
        text = {
            if (labels.isEmpty()) {
                Text(
                    text = stringResource(Res.string.labels_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    labels.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(label.id) }
                                .padding(vertical = Spacing.xs),
                        ) {
                            Checkbox(
                                checked = label.id in checkedLabelIds,
                                onCheckedChange = { onToggle(label.id) },
                            )
                            Text(
                                text = label.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_ok))
            }
        },
    )
}

private fun SortOption.labelRes() = when (this) {
    SortOption.UPDATED -> Res.string.sort_updated
    SortOption.CREATED -> Res.string.sort_created
    SortOption.COLOR -> Res.string.sort_color
}

/** Sıralama seçenekleri için alt sayfa (menü yerine). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    selected: SortOption,
    onSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Text(
            text = stringResource(Res.string.sort_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        )
        SortOption.entries.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                )
                Text(
                    text = stringResource(option.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}


/**
 * "+" butonu; dokununca etiketli iki seçenek açılır: Not ve Liste.
 *
 * Material 3 Expressive'in `FloatingActionButtonMenu` bileşeni kullanılıyor.
 * Daha önce bu davranış elle yazılmıştı (AnimatedVisibility + mini FAB'lar);
 * o kurulumda menü öğelerinin gölgesi alfa katmanı içinde köşeli bir blok
 * olarak rasterize oluyordu. Expressive bileşeni açılma/kapanma hareketini,
 * şekil morph'unu ve erişilebilirlik semantiğini kendisi yönetiyor.
 */
@Composable
private fun ExpandableAddFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddNote: () -> Unit,
    onAddChecklist: () -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
            ) {
                // `checkedProgress` açılma ilerlemesini 0..1 aralığında verir.
                // Yarıyı geçince ikon çarpıya döner; `animateIcon` geçişin
                // ölçek/saydamlık kısmını kendisi yürütür.
                val icon by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add
                    }
                }
                Icon(
                    imageVector = icon,
                    contentDescription = if (expanded) {
                        stringResource(Res.string.action_close)
                    } else {
                        stringResource(Res.string.fab_new)
                    },
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                onExpandedChange(false)
                onAddChecklist()
            },
            icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
            text = { Text(stringResource(Res.string.fab_new_checklist)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                onExpandedChange(false)
                onAddNote()
            },
            icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
            text = { Text(stringResource(Res.string.fab_new_note)) },
        )
    }
}

/** Sağa kaydırma arşivler, sola kaydırma çöpe atar. Seçim modunda devre dışı bırakılır. */
@Composable
private fun SwipeableNoteCard(
    note: Note,
    enabled: Boolean,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Aksiyon not başına yalnızca bir kez tetiklenmeli: confirmValueChange
    // yerleşim/animasyon sırasında birden fazla kez çağrılabiliyor ve her çağrı
    // yeni bir "geri al" olayı üretip arşivleme akışını bozuyordu.
    var actionFired by remember(note.id) { mutableStateOf(false) }

    // Durum not kimliğine bağlanıyor: liste yeniden düzenlendiğinde bir kartın
    // yarım kalmış kaydırma durumu başka bir karta miras kalmasın.
    val dismissState = key(note.id) {
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    // Aksiyonu tetikle ama "kaydırıldı" konumuna kilitlenme (false):
                    // not zaten listeden düşeceği için görsel olarak kaybolur.
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (!actionFired) {
                            actionFired = true
                            onArchive()
                        }
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (!actionFired) {
                            actionFired = true
                            onTrash()
                        }
                        false
                    }
                    // KRİTİK: Settled için true dönmeli. false dönülürse kutu hiçbir
                    // zaman "durgun" duruma geçemiyor, sürekli yeniden yerleşmeye
                    // çalışıyor ve ızgarayı ölçüm döngüsüne sokuyor -- kartların
                    // durmadan yer değiştirmesinin sebebi buydu.
                    SwipeToDismissBoxValue.Settled -> true
                }
            },
        )
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            val (icon, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive to Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete to Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> null to Alignment.Center
            }
            if (icon != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = alignment,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        content()
    }
}
