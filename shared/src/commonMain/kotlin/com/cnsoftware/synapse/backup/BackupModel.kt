package com.cnsoftware.synapse.backup

import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import kotlinx.serialization.Serializable

/**
 * Yedek dosyasının şu anki biçim sürümü.
 *
 * Alan eklemek sürümü artırmayı gerektirmez (varsayılanlar sayesinde eski
 * dosyalar okunmaya devam eder). Sürüm yalnızca geriye dönük uyumsuz bir
 * değişiklikte artmalı; okuma tarafı bilmediği bir sürümü reddeder.
 */
const val BACKUP_VERSION = 1

/**
 * Yedeğin kök nesnesi.
 *
 * Domain sınıfları doğrudan serileştirilmiyor: domain modeli uygulamanın
 * iç ihtiyacına göre değişebilir, dosya biçimi ise kullanıcının diskinde
 * kalıcı. Ayrı DTO'lar bu ikisini birbirinden bağımsız kılıyor.
 */
@Serializable
data class BackupFile(
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val labels: List<BackupLabel> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
data class BackupLabel(
    val id: String,
    val name: String,
    val color: String = NoteColor.DEFAULT.name,
    val icon: String = LabelIcon.LABEL.name,
)

@Serializable
data class BackupNote(
    val id: String,
    val title: String = "",
    val content: String = "",
    val color: String = NoteColor.DEFAULT.name,
    val isChecklist: Boolean = false,
    val items: List<BackupChecklistItem> = emptyList(),
    /** Yalnızca id'ler; etiketin kendisi [BackupFile.labels] içinde. */
    val labelIds: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val reminderAt: Long? = null,
)

@Serializable
data class BackupChecklistItem(
    val id: String,
    val text: String = "",
    val isChecked: Boolean = false,
    val position: Int = 0,
)

// --- Domain -> yedek ---

fun Label.toBackup() = BackupLabel(
    id = id,
    name = name,
    color = color.name,
    icon = icon.name,
)

fun Note.toBackup() = BackupNote(
    id = id,
    title = title,
    content = content,
    color = color.name,
    isChecklist = isChecklist,
    items = checklistItems.map {
        BackupChecklistItem(
            id = it.id,
            text = it.text,
            isChecked = it.isChecked,
            position = it.position,
        )
    },
    labelIds = labels.map { it.id },
    isPinned = isPinned,
    isArchived = isArchived,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reminderAt = reminderAt,
)

// --- Yedek -> domain ---

/**
 * Enum'lar dosyada AD olarak duruyor. Tanınmayan bir ad (ileri sürümden
 * gelen dosya, elle düzenlenmiş JSON) tüm geri yüklemeyi çökertmesin diye
 * varsayılana düşülüyor.
 */
fun BackupLabel.toDomain() = Label(
    id = id,
    name = name,
    color = NoteColor.entries.firstOrNull { it.name == color } ?: NoteColor.DEFAULT,
    icon = LabelIcon.entries.firstOrNull { it.name == icon } ?: LabelIcon.LABEL,
)

fun BackupNote.toDomain(labelsById: Map<String, Label>) = Note(
    id = id,
    title = title,
    content = content,
    color = NoteColor.entries.firstOrNull { it.name == color } ?: NoteColor.DEFAULT,
    isChecklist = isChecklist,
    checklistItems = items
        .sortedBy { it.position }
        .mapIndexed { index, item ->
            ChecklistItem(
                id = item.id,
                text = item.text,
                isChecked = item.isChecked,
                // Konumlar dosyada bozuk olabilir; sıraya göre yeniden veriliyor.
                position = index,
            )
        },
    // Yedekte olmayan bir etikete atıf varsa sessizce atlanıyor.
    labels = labelIds.mapNotNull { labelsById[it] },
    isPinned = isPinned,
    isArchived = isArchived,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reminderAt = reminderAt,
)
