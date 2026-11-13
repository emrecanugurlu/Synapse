package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import org.jetbrains.compose.resources.stringResource
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.app_name
import synapse.shared.generated.resources.nav_archive
import synapse.shared.generated.resources.nav_edit_labels
import synapse.shared.generated.resources.nav_labels
import synapse.shared.generated.resources.nav_notes
import synapse.shared.generated.resources.nav_settings
import synapse.shared.generated.resources.nav_trash

/** Drawer'da hangi bölümün açık olduğunu işaretlemek için. */
enum class DrawerDestination {
    NOTES,
    ARCHIVE,
    TRASH,
    LABELS,
    SETTINGS,
}

/**
 * Uygulamanın gezinme çekmecesi.
 *
 * Tek bir yerde, uygulama seviyesinde duruyor; Notlar, Arşiv, Çöp Kutusu,
 * Etiketler ve Ayarlar aynı çekmeceyi paylaşıyor. Önceden yalnızca not
 * listesine aitti, bu yüzden diğer ekranlar geri okuyla açılıyor ve
 * aralarında doğrudan geçiş yapılamıyordu.
 */
@Composable
fun AppDrawer(
    drawerState: DrawerState,
    current: DrawerDestination,
    labels: List<Label>,
    selectedLabelId: String?,
    gesturesEnabled: Boolean,
    onOpenNotes: (labelId: String?) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenLabels: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            // Varsayılan 360dp telefonda ekranın neredeyse tamamını kaplıyordu.
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Launcher ikonu zaten markayı taşıyor; drawer'da ikinci bir
                    // logo tekrar olur. Yalnızca marka renginde isim yeterli.
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = Spacing.xl),
                    )
                    DrawerItem(
                        label = stringResource(Res.string.nav_notes),
                        icon = Icons.Default.Lightbulb,
                        selected = current == DrawerDestination.NOTES && selectedLabelId == null,
                        onClick = { onOpenNotes(null) },
                    )
                    DrawerItem(
                        label = stringResource(Res.string.nav_archive),
                        icon = Icons.Default.Archive,
                        selected = current == DrawerDestination.ARCHIVE,
                        onClick = onOpenArchive,
                    )
                    DrawerItem(
                        label = stringResource(Res.string.nav_trash),
                        icon = Icons.Default.DeleteOutline,
                        selected = current == DrawerDestination.TRASH,
                        onClick = onOpenTrash,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    Text(
                        text = stringResource(Res.string.nav_labels),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = Spacing.sm),
                    )
                    labels.forEach { label ->
                        DrawerItem(
                            label = label.name,
                            // Etiketin kendi ikonu; kullanıcı listeyi renk/ikonla tanıyor.
                            icon = label.icon.imageVector,
                            selected = current == DrawerDestination.NOTES &&
                                label.id == selectedLabelId,
                            onClick = { onOpenNotes(label.id) },
                        )
                    }
                    // "Etiketleri düzenle" listenin devamı; ayrı bir bölüme
                    // ayrılmak yerine ait olduğu başlığın altında duruyor.
                    // Etiket yokken de görünür, yoksa ilk etiketi oluşturmanın
                    // çekmeceden yolu kalmazdı.
                    DrawerItem(
                        label = stringResource(Res.string.nav_edit_labels),
                        icon = Icons.Default.Edit,
                        selected = current == DrawerDestination.LABELS,
                        onClick = onOpenLabels,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    DrawerItem(
                        label = stringResource(Res.string.nav_settings),
                        icon = Icons.Default.Settings,
                        selected = current == DrawerDestination.SETTINGS,
                        onClick = onOpenSettings,
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = Spacing.md),
    )
}
