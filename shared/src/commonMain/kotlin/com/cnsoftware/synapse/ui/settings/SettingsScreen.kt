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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.ThemeMode
import com.cnsoftware.synapse.prefs.TrashRetention
import com.cnsoftware.synapse.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.settings_appearance
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
    onBack: () -> Unit,
    settings: AppSettings = koinInject(),
) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val trashRetention by settings.trashRetention.collectAsStateWithLifecycle()
    val autoSaveEnabled by settings.autoSaveEnabled.collectAsStateWithLifecycle()
    val labelFiltersVisible by settings.labelFiltersVisible.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    Scaffold(
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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
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
        )
    }
}
