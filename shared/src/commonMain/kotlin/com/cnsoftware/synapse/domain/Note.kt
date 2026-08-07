package com.cnsoftware.synapse.domain

/**
 * Not için domain modeli. `remoteId` ve `syncStatus` şu an kullanılmıyor,
 * ileride buluta senkronizasyon eklenince şema/migration değişikliği gerekmesin diye baştan var.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val color: NoteColor = NoteColor.DEFAULT,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val labels: List<Label> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val reminderAt: Long? = null,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)

data class Label(
    val id: String,
    val name: String,
    /** Rozet/çip zemini. Not renkleriyle aynı palet — iki yerde ayrı palet tutmamak için. */
    val color: NoteColor = NoteColor.DEFAULT,
    val icon: LabelIcon = LabelIcon.LABEL,
)

/**
 * Etiket ikonları sabit bir kümeden seçilir ve veritabanına ADIYLA yazılır.
 * Yeni değerler yalnızca sona eklenmeli; mevcut sabitler yeniden adlandırılırsa
 * kayıtlı etiketlerin ikonu çözülemez (ProGuard kuralları da ada dayanıyor).
 */
enum class LabelIcon {
    LABEL,
    STAR,
    FAVORITE,
    WORK,
    HOME,
    PERSON,
    SHOPPING,
    FLIGHT,
    SCHOOL,
    FITNESS,
    RESTAURANT,
    MUSIC,
    IDEA,
    MONEY,
    EVENT,
    CHECK,
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
    val position: Int = 0,
)

enum class SyncStatus {
    LOCAL_ONLY,
    SYNCED,
    PENDING,
    CONFLICT,
}

enum class NoteColor {
    DEFAULT,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    TEAL,
    BLUE,
    PURPLE,
    PINK,
    BROWN,
    GRAY,
}
