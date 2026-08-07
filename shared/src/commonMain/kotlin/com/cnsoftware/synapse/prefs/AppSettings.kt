package com.cnsoftware.synapse.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Etiket metinleri UI katmanında çözümlenir (yerelleştirme için). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Çöp kutusundaki notların kalıcı silinmeden önce bekleyeceği süre. */
enum class TrashRetention(val days: Int) {
    WEEK(7),
    TWO_WEEKS(14),
    MONTH(30),
    ;

    companion object {
        val DEFAULT = WEEK

        fun fromDays(days: Int): TrashRetention = entries.firstOrNull { it.days == days } ?: DEFAULT
    }
}

/**
 * Uygulama genelinde geçerli, anında etki eden ayarlar.
 * Tek örnek (Koin single) olarak tutulur; hem tema hem de ekranlar aynı akışı dinler.
 */
class AppSettings(private val preferences: PreferencesStore) {

    private val _themeMode = MutableStateFlow(
        preferences.getString(PreferencesStore.KEY_THEME_MODE)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _trashRetention = MutableStateFlow(
        TrashRetention.fromDays(
            preferences.getInt(
                PreferencesStore.KEY_TRASH_RETENTION_DAYS,
                default = TrashRetention.DEFAULT.days,
            ),
        ),
    )
    val trashRetention: StateFlow<TrashRetention> = _trashRetention.asStateFlow()

    /**
     * Kapalıyken editör hiçbir şeyi kendiliğinden kaydetmez; kullanıcı Kaydet'e basar.
     * Varsayılan açık — veri kaybını önleyen davranış bu.
     */
    private val _autoSaveEnabled = MutableStateFlow(
        preferences.getBoolean(PreferencesStore.KEY_AUTOSAVE, default = true),
    )
    val autoSaveEnabled: StateFlow<Boolean> = _autoSaveEnabled.asStateFlow()

    /**
     * Not listesinin üstündeki etiket filtresi şeridi.
     * Varsayılan kapalı: ekran açılır açılmaz notlarla karşılaşılsın.
     */
    private val _labelFiltersVisible = MutableStateFlow(
        preferences.getBoolean(PreferencesStore.KEY_LABEL_FILTERS, default = false),
    )
    val labelFiltersVisible: StateFlow<Boolean> = _labelFiltersVisible.asStateFlow()

    fun setLabelFiltersVisible(visible: Boolean) {
        _labelFiltersVisible.value = visible
        preferences.putBoolean(PreferencesStore.KEY_LABEL_FILTERS, visible)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        _autoSaveEnabled.value = enabled
        preferences.putBoolean(PreferencesStore.KEY_AUTOSAVE, enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.putString(PreferencesStore.KEY_THEME_MODE, mode.name)
    }

    fun setTrashRetention(retention: TrashRetention) {
        _trashRetention.value = retention
        preferences.putInt(PreferencesStore.KEY_TRASH_RETENTION_DAYS, retention.days)
    }
}
