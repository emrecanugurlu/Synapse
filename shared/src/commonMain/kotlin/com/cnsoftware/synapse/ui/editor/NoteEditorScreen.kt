package com.cnsoftware.synapse.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.ActionsBottomSheet
import com.cnsoftware.synapse.ui.common.SheetActionRow
import com.cnsoftware.synapse.ui.common.RoundedTextField
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.toContainerColor
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.formatDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.action_close
import synapse.shared.generated.resources.action_discard
import synapse.shared.generated.resources.editor_save
import synapse.shared.generated.resources.editor_unsaved_message
import synapse.shared.generated.resources.editor_unsaved_title
import synapse.shared.generated.resources.action_delete
import synapse.shared.generated.resources.action_more
import synapse.shared.generated.resources.action_ok
import synapse.shared.generated.resources.checklist_add_item
import synapse.shared.generated.resources.checklist_completed_count
import synapse.shared.generated.resources.checklist_drag_handle
import synapse.shared.generated.resources.checklist_check_all
import synapse.shared.generated.resources.checklist_clear_completed
import synapse.shared.generated.resources.checklist_uncheck_all
import synapse.shared.generated.resources.stats_checklist
import synapse.shared.generated.resources.stats_text
import synapse.shared.generated.resources.checklist_item_hint
import synapse.shared.generated.resources.checklist_remove_item
import synapse.shared.generated.resources.color_dialog_title
import synapse.shared.generated.resources.editor_archive
import synapse.shared.generated.resources.editor_content_hint
import synapse.shared.generated.resources.editor_labels
import synapse.shared.generated.resources.editor_pick_color
import synapse.shared.generated.resources.editor_reminder
import synapse.shared.generated.resources.editor_reminder_remove
import synapse.shared.generated.resources.editor_share
import synapse.shared.generated.resources.editor_title_hint
import synapse.shared.generated.resources.editor_to_checklist
import synapse.shared.generated.resources.editor_to_text
import synapse.shared.generated.resources.editor_unarchive
import synapse.shared.generated.resources.label_create
import synapse.shared.generated.resources.labels_empty_description
import synapse.shared.generated.resources.label_new
import synapse.shared.generated.resources.label_remove
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    // Var olan bir not için noteId; yeni not için her navigasyonda benzersiz bir anahtar.
    // Bu olmadan Koin/ViewModelStore aynı NoteEditorViewModel sınıfını -- farklı notlar
    // arasında geçişte bile -- tek örnek olarak önbelleğe alıp eski notun state'ini geri döndürebilir.
    // Yeni notta bu anahtar aynı zamanda notun id'si olur (bkz. EditorArgs.newNoteId).
    editorKey: String,
    startAsChecklist: Boolean = false,
    /** Paylaş menüsünden gelen metinle açılan yeni not için ön dolgu. */
    initialTitle: String = "",
    initialContent: String = "",
    viewModel: NoteEditorViewModel = koinViewModel(key = editorKey) {
        parametersOf(
            EditorArgs(
                noteId = noteId,
                startAsChecklist = startAsChecklist,
                initialTitle = initialTitle,
                initialContent = initialContent,
                newNoteId = editorKey.takeIf { noteId == null },
            ),
        )
    },
) {
    // ViewModel önbellekten gelmiş olabilir; ekran her açıldığında notun
    // güncel hâlini veritabanından tazele.
    LaunchedEffect(viewModel) { viewModel.onOpened() }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    val pendingFocusItemId by viewModel.pendingFocusItemId.collectAsStateWithLifecycle()
    val lastSavedAt by viewModel.lastSavedAt.collectAsStateWithLifecycle()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showActionsSheet by remember { mutableStateOf(false) }
    val autoSaveEnabled by viewModel.autoSaveEnabled.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    var showUnsavedDialog by remember { mutableStateOf(false) }

    /**
     * Otomatik kaydetme açıkken çıkış sessizce kaydeder.
     * Kapalıyken kullanıcının kararı sorulur — sessizce kaydetmek de,
     * sessizce atmak da beklenmedik olurdu.
     */
    fun handleBack() {
        when {
            autoSaveEnabled -> {
                viewModel.save()
                onBack()
            }
            hasUnsavedChanges -> showUnsavedDialog = true
            else -> onBack()
        }
    }

    // Sistem geri tuşu/jesti üst bardaki geri okuyla aynı davranmalı
    // (uygulamadan çıkılmamalı).
    BackHandler { handleBack() }
    // Renk seçildiğinde zemin sertçe değişmesin.
    // Süre elle verilmiyor: renk gibi "efekt" nitelikli geçişler Expressive
    // hareket şemasından geliyor, böylece tüm uygulama aynı ritmi paylaşıyor.
    val backgroundColor by animateColorAsState(
        targetValue = state.color.toContainerColor(),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "noteBackground",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    // Üst bar yalnızca "nota dair durum" aksiyonlarını taşır;
                    // düzenleme araçları alt barda.
                    EditorActionButton(
                        icon = Icons.Default.Alarm,
                        contentDescription = stringResource(Res.string.editor_reminder),
                        active = state.reminderAt != null,
                        onClick = { showReminderDialog = true },
                    )
                    if (!state.isNew) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        EditorActionButton(
                            icon = if (state.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (state.isArchived) stringResource(Res.string.editor_unarchive) else stringResource(Res.string.editor_archive),
                            active = state.isArchived,
                            toggleable = true,
                            onClick = viewModel::toggleArchived,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    EditorActionButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.action_more),
                        onClick = { showActionsSheet = true },
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                // Klavye açıldığında bar klavyenin üstüne çıksın.
                BottomAppBar(
                    containerColor = backgroundColor,
                    modifier = Modifier.imePadding(),
                ) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    EditorActionButton(
                        icon = Icons.Default.Palette,
                        contentDescription = stringResource(Res.string.editor_pick_color),
                        onClick = { showColorDialog = true },
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    EditorActionButton(
                        icon = Icons.Default.Label,
                        contentDescription = stringResource(Res.string.editor_labels),
                        active = state.labels.isNotEmpty(),
                        onClick = { showLabelDialog = true },
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    EditorActionButton(
                        icon = if (state.isChecklist) Icons.Default.Notes else Icons.Default.Checklist,
                        contentDescription = if (state.isChecklist) stringResource(Res.string.editor_to_text) else stringResource(Res.string.editor_to_checklist),
                        active = state.isChecklist,
                        toggleable = true,
                        onClick = viewModel::toggleChecklistMode,
                    )

                    // Otomatik kaydetme kapalıyken kullanıcının tek kayıt yolu bu.
                    if (!autoSaveEnabled) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        EditorActionButton(
                            icon = Icons.Default.Save,
                            contentDescription = stringResource(Res.string.editor_save),
                            active = hasUnsavedChanges,
                                onClick = { viewModel.save() },
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (!state.isNew) {
                        Text(
                            text = formatDateTime(lastSavedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                    }

                    Spacer(modifier = Modifier.width(Spacing.xs))
                }
            }
        },
        containerColor = backgroundColor,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            TextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        text = stringResource(Res.string.editor_title_hint),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmallEmphasized,
                colors = transparentTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            // Hatırlatıcı ve etiketler nota ait niteliklerdir; başlığın hemen
            // altında duruyorlar. Önceden içeriğin ALTINDAydılar, yani uzun bir
            // notta görmek için sonuna kadar kaydırmak gerekiyordu.
            NoteAttributes(
                reminderAt = state.reminderAt,
                labels = state.labels,
                onReminderClick = { showReminderDialog = true },
                onReminderRemove = viewModel::clearReminder,
                onLabelRemove = viewModel::toggleLabel,
            )

            if (state.isChecklist) {
                ChecklistEditor(
                    items = state.checklistItems,
                    pendingFocusItemId = pendingFocusItemId,
                    onToggle = viewModel::toggleChecklistItemChecked,
                    onTextChange = viewModel::onChecklistItemTextChange,
                    onRemove = viewModel::removeChecklistItem,
                    onAddItem = viewModel::addChecklistItem,
                    onAddItemAfter = viewModel::addChecklistItemAfter,
                    onBackspaceOnEmpty = viewModel::onChecklistBackspaceOnEmpty,
                    onFocusHandled = viewModel::onFocusHandled,
                    onMove = viewModel::moveChecklistItem,
                )
            } else {
                TextField(
                    value = state.content,
                    onValueChange = viewModel::onContentChange,
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.editor_content_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = transparentTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }

    if (showActionsSheet) {
        NoteActionsSheet(
            canDelete = !state.isNew,
            state = state,
            onShare = {
                showActionsSheet = false
                viewModel.share()
            },
            onDelete = {
                showActionsSheet = false
                viewModel.delete()
                onBack()
            },
            onToggleAll = {
                showActionsSheet = false
                viewModel.toggleAllChecklistItems()
            },
            onClearCompleted = {
                showActionsSheet = false
                viewModel.clearCompletedChecklistItems()
            },
            onDismiss = { showActionsSheet = false },
        )
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(Res.string.editor_unsaved_title)) },
            text = { Text(stringResource(Res.string.editor_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    viewModel.save()
                    onBack()
                }) {
                    Text(stringResource(Res.string.editor_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    viewModel.discardChanges()
                    onBack()
                }) {
                    Text(stringResource(Res.string.action_discard))
                }
            },
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            selected = state.color,
            onSelect = {
                viewModel.onColorChange(it)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false },
        )
    }

    if (showReminderDialog) {
        ReminderPickerDialog(
            onConfirm = { millis ->
                viewModel.setReminder(millis)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false },
        )
    }

    if (showLabelDialog) {
        LabelPickerDialog(
            allLabels = allLabels,
            selectedIds = state.labels.map { it.id }.toSet(),
            onToggle = viewModel::toggleLabel,
            onCreate = viewModel::createAndAttachLabel,
            onDismiss = { showLabelDialog = false },
        )
    }
}

@Composable
private fun LabelPickerDialog(
    allLabels: List<com.cnsoftware.synapse.domain.Label>,
    selectedIds: Set<String>,
    onToggle: (com.cnsoftware.synapse.domain.Label) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabelName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.editor_labels)) },
        text = {
            Column {
                if (allLabels.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.labels_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Uzun etiket listesinde dialog taşmasın.
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        allLabels.forEach { label ->
                            val checked = label.id in selectedIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(label) }
                                    .padding(vertical = Spacing.xs),
                            ) {
                                // Etiketin rengi ve ikonu burada da görünüyor;
                                // kullanıcı listeyi adla değil görselle tarıyor.
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (label.color == NoteColor.DEFAULT) {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            } else {
                                                label.color.toContainerColor()
                                            },
                                        ),
                                ) {
                                    Icon(
                                        imageVector = label.icon.imageVector,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Checkbox(
                                    checked = checked,
                                    // Satırın tamamı zaten tıklanabilir; kutu
                                    // ikinci bir hedef olmasın diye null.
                                    onCheckedChange = null,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RoundedTextField(
                        value = newLabelName,
                        onValueChange = { newLabelName = it },
                        placeholder = stringResource(Res.string.label_new),
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(
                        onClick = {
                            onCreate(newLabelName)
                            newLabelName = ""
                        },
                        enabled = newLabelName.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.label_create))
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

@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    pendingFocusItemId: String?,
    onToggle: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onAddItem: () -> Unit,
    onAddItemAfter: (String) -> Unit,
    onBackspaceOnEmpty: (String) -> Unit,
    onFocusHandled: () -> Unit,
    onMove: (String, Int) -> Unit,
) {
    // Tamamlananlar listenin sonuna, ayrı bir başlık altına iner.
    val pending = items.filterNot { it.isChecked }
    val completed = items.filter { it.isChecked }

    // Her öğe için kalıcı bir FocusRequester; Enter sonrası yeni satıra odaklanmak için.
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val currentIds = items.map { it.id }.toSet()
    focusRequesters.keys.retainAll(currentIds)

    // Sürükleyerek sıralama.
    // Sürükleme boyunca veri DEĞİŞMEZ: sürüklenen satır parmağı birebir takip eder,
    // aradaki satırlar animasyonla kayar. Gerçek sıralama parmak kalkınca bir kez uygulanır.
    // (Veriyi anlık değiştirmek her adımda yeniden kompozisyon ve takılma yaratıyordu.)
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragFromIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(0) }
    val currentPending by rememberUpdatedState(pending)

    val dragTargetIndex = if (draggingId != null && rowHeightPx > 0 && dragFromIndex >= 0) {
        (dragFromIndex + (dragOffsetY / rowHeightPx).roundToInt())
            .coerceIn(0, (pending.size - 1).coerceAtLeast(0))
    } else {
        -1
    }

    LaunchedEffect(pendingFocusItemId) {
        val id = pendingFocusItemId ?: return@LaunchedEffect
        // Yeni satırın kompozisyona girmesini bekle.
        withFrameNanos { }
        runCatching { focusRequesters[id]?.requestFocus() }
        onFocusHandled()
    }

    @Composable
    fun row(item: ChecklistItem, index: Int, draggable: Boolean) {
        val requester = focusRequesters.getOrPut(item.id) { FocusRequester() }
        val isDragging = draggingId == item.id

        // Sürüklenen satırın geçtiği aralıktaki satırlar bir satır boyu kayar.
        val displacement = when {
            draggingId == null || isDragging || rowHeightPx == 0 -> 0f
            dragFromIndex < dragTargetIndex && index in (dragFromIndex + 1)..dragTargetIndex ->
                -rowHeightPx.toFloat()
            dragFromIndex > dragTargetIndex && index in dragTargetIndex until dragFromIndex ->
                rowHeightPx.toFloat()
            else -> 0f
        }
        val shift by animateFloatAsState(
            targetValue = displacement,
            // Konum değiştiren hareket -> spatial spec.
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "checklistRowShift",
        )

        ChecklistItemRow(
            item = item,
            focusRequester = requester,
            onToggle = onToggle,
            onTextChange = onTextChange,
            onRemove = onRemove,
            onEnter = { onAddItemAfter(item.id) },
            onBackspaceOnEmpty = { onBackspaceOnEmpty(item.id) },
            isDragging = isDragging,
            modifier = Modifier
                .onGloballyPositioned { if (rowHeightPx == 0) rowHeightPx = it.size.height }
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    // Sürüklenen satır parmağı birebir izler; diğerleri yumuşak kayar.
                    translationY = if (isDragging) dragOffsetY else shift
                    if (isDragging) {
                        scaleX = 1.02f
                        scaleY = 1.02f
                    }
                },
            dragHandle = if (!draggable) {
                null
            } else {
                {
                    Icon(
                        // Yatay iki çizgi ("=") taşıma değil "menü" çağrıştırıyordu.
                        // DragIndicator (altı nokta) sıralama için standart işaret.
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = stringResource(Res.string.checklist_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            // Sadece item.id'ye bağlı: sürükleme boyunca jest kopmasın.
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = item.id
                                        dragFromIndex = currentPending.indexOfFirst { it.id == item.id }
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        // Hedefi burada, canlı state'lerden hesaplıyoruz.
                                        // pointerInput bloğu yeniden çalışmadığı için
                                        // kompozisyonda hesaplanan değer bayat kalıyordu.
                                        val from = dragFromIndex
                                        val height = rowHeightPx
                                        val lastIndex = (currentPending.size - 1).coerceAtLeast(0)
                                        val to = if (from >= 0 && height > 0) {
                                            (from + (dragOffsetY / height).roundToInt())
                                                .coerceIn(0, lastIndex)
                                        } else {
                                            -1
                                        }
                                        draggingId = null
                                        dragFromIndex = -1
                                        dragOffsetY = 0f
                                        if (from >= 0 && to >= 0 && from != to) {
                                            onMove(item.id, to)
                                        }
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragFromIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffsetY += amount.y
                                    },
                                )
                            },
                    )
                }
            },
        )
    }

    Column {
        pending.forEachIndexed { index, item ->
            row(item, index = index, draggable = pending.size > 1)
        }
        TextButton(onClick = onAddItem) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(stringResource(Res.string.checklist_add_item))
        }
        if (completed.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            Text(
                text = stringResource(Res.string.checklist_completed_count, completed.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xs),
            )
            completed.forEach { item -> row(item, index = -1, draggable = false) }
        }
    }
}

/** Sürükleme tutamacı için ayrılan sabit genişlik. */
private val DRAG_HANDLE_SLOT = 28.dp

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    focusRequester: FocusRequester,
    onToggle: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isDragging) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                },
                shape = MaterialTheme.shapes.small,
            ),
    ) {
        // Tutamaç yuvası her durumda aynı genişlikte.
        // Önceden tutamaç varken sadece 4dp, yokken 24dp boşluk vardı; bu yüzden
        // listede tek öğe olunca hizalama farklı, iki öğe olunca satırlar sola
        // yapışık görünüyordu. Sabit yuva ikisini de aynı x'te başlatıyor.
        Box(
            modifier = Modifier.width(DRAG_HANDLE_SLOT),
            contentAlignment = Alignment.Center,
        ) {
            dragHandle?.invoke()
        }
        Checkbox(checked = item.isChecked, onCheckedChange = { onToggle(item.id) })
        TextField(
            value = item.text,
            onValueChange = { onTextChange(item.id, it) },
            placeholder = { Text(stringResource(Res.string.checklist_item_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEnter() }),
            textStyle = if (item.isChecked) {
                LocalTextStyle.current.copy(
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LocalTextStyle.current
            },
            colors = transparentTextFieldColors(),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    // Boş satırda geri silme: satırı kaldırıp bir öncekine dön.
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        item.text.isEmpty()
                    ) {
                        onBackspaceOnEmpty()
                        true
                    } else {
                        false
                    }
                },
        )
        IconButton(onClick = { onRemove(item.id) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.checklist_remove_item), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

/** Nota dair ikincil işlemler; üst bardaki ⋮ ile açılır. */
@Composable
private fun NoteActionsSheet(
    canDelete: Boolean,
    state: NoteEditorUiState,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    ActionsBottomSheet(onDismiss = onDismiss) {
        // Liste notlarına özgü toplu işlemler; metin notunda anlamı yok.
        if (state.isChecklist && state.checklistItems.isNotEmpty()) {
            val allChecked = state.checklistItems.all { it.isChecked }
            SheetActionRow(
                icon = if (allChecked) Icons.Default.CheckBoxOutlineBlank else Icons.Default.DoneAll,
                label = stringResource(
                    if (allChecked) {
                        Res.string.checklist_uncheck_all
                    } else {
                        Res.string.checklist_check_all
                    },
                ),
                onClick = onToggleAll,
            )
            if (state.checklistItems.any { it.isChecked }) {
                SheetActionRow(
                    icon = Icons.Default.PlaylistRemove,
                    label = stringResource(Res.string.checklist_clear_completed),
                    onClick = onClearCompleted,
                )
            }
        }

        SheetActionRow(
            icon = Icons.Default.Share,
            label = stringResource(Res.string.editor_share),
            onClick = onShare,
        )
        if (canDelete) {
            SheetActionRow(
                icon = Icons.Default.Delete,
                label = stringResource(Res.string.action_delete),
                destructive = true,
                onClick = onDelete,
            )
        }

        NoteStats(state)
    }
}

/**
 * Notun büyüklüğü: liste notunda öğe sayısı, metin notunda kelime ve karakter.
 * Alt menünün en altında, ayraçla ayrılmış bilgi satırı.
 */
@Composable
private fun NoteStats(state: NoteEditorUiState) {
    val text = if (state.isChecklist) {
        val total = state.checklistItems.size
        val done = state.checklistItems.count { it.isChecked }
        stringResource(Res.string.stats_checklist, done, total)
    } else {
        // Ardışık boşluklar tek ayraç sayılsın diye filtreleniyor.
        val words = state.content.split(' ', '\n', '\t').count { it.isNotBlank() }
        stringResource(Res.string.stats_text, words, state.content.length)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.xs),
    )
}

/**
 * Nota ait nitelikler: hatırlatıcı ve etiketler.
 *
 * Sarmalayan bir düzen (FlowRow) kullanılıyor; önceden etiketler yatay
 * kaydırmalı tek satırdaydı ve ikiden fazla etikette geri kalanlar ekran
 * dışında kalıyordu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteAttributes(
    reminderAt: Long?,
    labels: List<com.cnsoftware.synapse.domain.Label>,
    onReminderClick: () -> Unit,
    onReminderRemove: () -> Unit,
    onLabelRemove: (com.cnsoftware.synapse.domain.Label) -> Unit,
) {
    if (reminderAt == null && labels.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm, bottom = Spacing.md),
    ) {
        reminderAt?.let { at ->
            // Zamanı geçmiş hatırlatıcı uyarı renginde: kullanıcı kaçırdığını
            // tarihi okumadan anlıyor.
            val overdue = at < currentTimeMillis()
            AssistChip(
                onClick = onReminderClick,
                label = { Text(formatDateTime(at)) },
                leadingIcon = {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    ChipRemoveButton(
                        contentDescription = stringResource(Res.string.editor_reminder_remove),
                        onClick = onReminderRemove,
                    )
                },
                colors = if (overdue) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        trailingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
            )
        }

        labels.forEach { label ->
            AssistChip(
                onClick = { onLabelRemove(label) },
                label = { Text(label.name) },
                // Etiketin kendi ikonu ve rengi çipte de geçerli.
                leadingIcon = {
                    Icon(
                        label.icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                trailingIcon = {
                    ChipRemoveButton(
                        contentDescription = stringResource(Res.string.label_remove),
                        onClick = { onLabelRemove(label) },
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (label.color == NoteColor.DEFAULT) {
                        Color.Transparent
                    } else {
                        label.color.toContainerColor()
                    },
                ),
            )
        }
    }
}

/**
 * Çip içindeki kaldırma düğmesi.
 * Çıplak `Icon` + `clickable` 14dp'lik bir dokunma hedefi bırakıyordu;
 * ikon aynı boyutta kalıyor ama tıklanabilir alan büyütülüyor.
 */
@Composable
private fun ChipRemoveButton(contentDescription: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * Editördeki aksiyonlar için ortak buton yüzeyi.
 * Renkli not zeminlerinde çıplak ikonlar kaybolduğu için hepsi tonal bir kapsayıcı alır.
 *
 * Gerçekten aç/kapa olanlar (arşiv, liste modu) `FilledTonalIconToggleButton`
 * kullanıyor: Expressive temada basılıyken şekli morph ediyor ve ekran okuyucuya
 * "seçili/seçili değil" durumunu bildiriyor. Dialog açan veya tek seferlik olanlar
 * (hatırlatıcı, etiket, kaydet) toggle değil — durumu yalnızca renkle belli
 * ediyorlar, yoksa ekran okuyucu yanlışlıkla "seçili" diye okurdu.
 *
 * Aktif durum için dolu `primary` (tam turuncu) kullanılmıyor: aynı ekranda birden
 * fazla aktif buton olduğunda çok baskın duruyordu. Onun yerine yumuşak
 * `primaryContainer` tonu; vurgu okunuyor ama ekranı ele geçirmiyor.
 */
@Composable
private fun EditorActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    toggleable: Boolean = false,
) {
    val idleContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val idleContent = MaterialTheme.colorScheme.onSurface

    if (toggleable) {
        FilledTonalIconToggleButton(
            checked = active,
            onCheckedChange = { onClick() },
            colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                containerColor = idleContainer,
                contentColor = idleContent,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    idleContainer
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    idleContent
                },
            ),
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    selected: NoteColor,
    onSelect: (NoteColor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.color_dialog_title)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NoteColor.entries.forEach { color ->
                    val swatch = if (color == NoteColor.DEFAULT) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        color.toContainerColor()
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (selected == color) 2.dp else 1.dp,
                                color = if (selected == color) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .clickable { onSelect(color) },
                    ) {
                        if (selected == color) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}
