package com.cnsoftware.synapse.prefs

/**
 * Basit anahtar-değer tercih deposu.
 * Android: SharedPreferences, iOS: NSUserDefaults.
 */
interface PreferencesStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)

    companion object {
        const val KEY_SORT_OPTION = "sort_option"
        const val KEY_GRID_VIEW = "grid_view"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_TRASH_RETENTION_DAYS = "trash_retention_days"
        const val KEY_AUTOSAVE = "autosave_enabled"
        const val KEY_LABEL_FILTERS = "label_filters_visible"
        const val KEY_AUTO_BACKUP = "auto_backup_frequency"
        const val KEY_BACKUP_FOLDER = "auto_backup_folder_uri"
        const val KEY_LAST_BACKUP_AT = "auto_backup_last_at"
    }
}
