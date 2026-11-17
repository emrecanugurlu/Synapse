package com.cnsoftware.synapse.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.AutoBackupFrequency
import com.cnsoftware.synapse.backup.BackupResult
import com.cnsoftware.synapse.backup.FailureReason
import com.cnsoftware.synapse.prefs.ThemeMode
import com.cnsoftware.synapse.prefs.TrashRetention
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.util.formatDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_open_menu
import synapse.shared.generated.resources.settings_appearance
import synapse.shared.generated.resources.backup_error_invalid
import synapse.shared.generated.resources.backup_error_io
import synapse.shared.generated.resources.backup_error_version
import synapse.shared.generated.resources.backup_success
import synapse.shared.generated.resources.settings_backup
import synapse.shared.generated.resources.settings_auto_backup
import synapse.shared.generated.resources.settings_auto_backup_daily
import synapse.shared.generated.resources.settings_auto_backup_off
import synapse.shared.generated.resources.settings_auto_backup_weekly
import synapse.shared.generated.resources.settings_backup_folder
import synapse.shared.generated.resources.settings_backup_folder_choose
import synapse.shared.generated.resources.settings_backup_folder_lost
import synapse.shared.generated.resources.settings_backup_folder_none
import synapse.shared.generated.resources.settings_backup_folder_set
import synapse.shared.generated.resources.settings_backup_last
import synapse.shared.generated.resources.settings_backup_description
import synapse.shared.generated.resources.settings_backup_export
import synapse.shared.generated.resources.settings_backup_import
import synapse.shared.generated.resources.settings_autosave
import synapse.shared.generated.resources.settings_autosave_off_description
import synapse.shared.generated.resources.settings_autosave_on_description
import synapse.shared.generated.resources.settings_label_filters
import synapse.shared.generated.resources.settings_label_filters_description
import synapse.shared.generated.resources.settings_notes
import synapse.shared.generated.resources.settings_retention_14
import synapse.shared.generated.resources.settings_retention_30
import synapse.shared.generated.resources.settings_retention_7
import synapse.shared.generated.resources.settings_theme_dark
import synapse.shared.generated.resources.settings_theme_light
import synapse.shared.generated.resources.settings_theme_system
import synapse.shared.generated.resources.settings_title
import synapse.shared.generated.resources.settings_trash
import synapse.shared.generated.resources.settings_trash_description

/**
 * Ayarlar.
 *
 * Tasarım notu: eskiden her seçenek tam genişlikte kendi satırındaydı
 * (3 tema + 3 saklama süresi = 6 radyo satırı, aralarında ayraçlar). Az sayıda
 * ayar, kalabalık bir ekran gibi duruyordu. Şimdi:
 *  - Birbirini dışlayan seçimler tek satırlık segment kontrolünde,
 *  - İlgili ayarlar yuvarlak kartlarda gruplanıyor (ayraçlara gerek kalmadı),
 *  - "Etiketler" ve "Editör" başlıkları tek bir "Notlar" kartında birleşti.
 */
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    settings: AppSettings = koinInject(),
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val trashRetention by settings.trashRetention.collectAsStateWithLifecycle()
    val autoSaveEnabled by settings.autoSaveEnabled.collectAsStateWithLifecycle()
    val labelFiltersVisible by settings.labelFiltersVisible.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val autoBackupFrequency by settings.autoBackupFrequency.collectAsStateWithLifecycle()
    val backupFolderUri by settings.backupFolderUri.collectAsStateWithLifecycle()
    val lastBackupAt by settings.lastBackupAt.collectAsStateWithLifecycle()
    val folderAccessLost by viewModel.folderAccessLost.collectAsStateWithLifecycle()
    val backupResult by viewModel.result.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sonuç bir kez gösterilip temizleniyor; ekran döndüğünde tekrar çıkmasın.
    val successMessage = stringResource(Res.string.backup_success)
    val invalidMessage = stringResource(Res.string.backup_error_invalid)
    val versionMessage = stringResource(Res.string.backup_error_version)
    val ioMessage = stringResource(Res.string.backup_error_io)
    LaunchedEffect(backupResult) {
        val outcome = backupResult ?: return@LaunchedEffect
        val message = when (outcome) {
            is BackupResult.Success -> successMessage
            is BackupResult.Cancelled -> return@LaunchedEffect
            is BackupResult.Failure -> when (outcome.reason) {
                FailureReason.INVALID_FILE -> invalidMessage
                FailureReason.UNSUPPORTED_VERSION -> versionMessage
                FailureReason.IO -> ioMessage
            }
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeResult()
    }

    BackHandler { onBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(Res.string.action_open_menu),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SettingsGroup(title = stringResource(Res.string.settings_appearance)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = mode == themeMode,
                            onClick = { settings.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> Res.string.settings_theme_system
                                        ThemeMode.LIGHT -> Res.string.settings_theme_light
                                        ThemeMode.DARK -> Res.string.settings_theme_dark
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = stringResource(Res.string.settings_notes)) {
                SwitchRow(
                    title = stringResource(Res.string.settings_label_filters),
                    description = stringResource(Res.string.settings_label_filters_description),
                    checked = labelFiltersVisible,
                    onCheckedChange = settings::setLabelFiltersVisible,
                )
                SwitchRow(
                    title = stringResource(Res.string.settings_autosave),
                    description = if (autoSaveEnabled) {
                        stringResource(Res.string.settings_autosave_on_description)
                    } else {
                        stringResource(Res.string.settings_autosave_off_description)
                    },
                    checked = autoSaveEnabled,
                    onCheckedChange = settings::setAutoSaveEnabled,
                )
            }

            SettingsGroup(title = stringResource(Res.string.settings_trash)) {
                Text(
                    text = stringResource(Res.string.settings_trash_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TrashRetention.entries.forEachIndexed { index, retention ->
                        SegmentedButton(
                            selected = retention == trashRetention,
                            onClick = { settings.setTrashRetention(retention) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TrashRetention.entries.size,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    when (retention) {
                                        TrashRetention.WEEK -> Res.string.settings_retention_7
                                        TrashRetention.TWO_WEEKS -> Res.string.settings_retention_14
                                        TrashRetention.MONTH -> Res.string.settings_retention_30
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = stringResource(Res.string.settings_backup)) {
                Text(
                    text = stringResource(Res.string.settings_backup_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(
                        onClick = viewModel::exportBackup,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.settings_backup_export))
                    }
                    OutlinedButton(
                        onClick = viewModel::importBackup,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.settings_backup_import))
                    }
                }
                if (busy) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    LoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.lg))

                Text(
                    text = stringResource(Res.string.settings_auto_backup),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AutoBackupFrequency.entries.forEachIndexed { index, frequency ->
                        SegmentedButton(
                            selected = frequency == autoBackupFrequency,
                            onClick = { viewModel.setFrequency(frequency) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AutoBackupFrequency.entries.size,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    when (frequency) {
                                        AutoBackupFrequency.OFF -> Res.string.settings_auto_backup_off
                                        AutoBackupFrequency.DAILY -> Res.string.settings_auto_backup_daily
                                        AutoBackupFrequency.WEEKLY -> Res.string.settings_auto_backup_weekly
                                    },
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Klasör satırı: durum + yeniden seçme.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { viewModel.chooseBackupFolder() }
                        .padding(vertical = Spacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_backup_folder),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = when {
                                folderAccessLost -> stringResource(Res.string.settings_backup_folder_lost)
                                backupFolderUri.isBlank() ->
                                    stringResource(Res.string.settings_backup_folder_none)
                                lastBackupAt > 0L ->
                                    stringResource(Res.string.settings_backup_last, formatDateTime(lastBackupAt))
                                else -> stringResource(Res.string.settings_backup_folder_set)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (folderAccessLost) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    TextButton(onClick = viewModel::chooseBackupFolder, enabled = !busy) {
                        Text(stringResource(Res.string.settings_backup_folder_choose))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

/**
 * Başlık + yuvarlak kart. Ayraç yerine kapsayıcı kullanılıyor: Expressive'de
 * gruplama işi çizgilerin değil yüzeylerin.
 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.sm),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.lg), content = content)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            // Satırın tamamı tıklanabilir; anahtar ikinci bir hedef olmasın.
            onCheckedChange = null,
            // Expressive anahtar: açıkken başparmakta onay işareti.
            thumbContent = if (checked) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            },
        )
    }
}
