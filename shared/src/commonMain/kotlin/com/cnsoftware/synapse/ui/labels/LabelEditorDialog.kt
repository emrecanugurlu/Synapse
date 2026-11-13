package com.cnsoftware.synapse.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.RoundedTextField
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor
import org.jetbrains.compose.resources.stringResource
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_save
import synapse.shared.generated.resources.label_color
import synapse.shared.generated.resources.label_create
import synapse.shared.generated.resources.label_icon
import synapse.shared.generated.resources.label_name
import synapse.shared.generated.resources.label_new
import synapse.shared.generated.resources.labels_edit_title

/** Seçicilerdeki daire boyutu; hem renk hem simge için aynı ritim. */
private val PICKER_ITEM = 44.dp

/** Alt aksiyon butonlarının yüksekliği. */
private val BUTTON_HEIGHT = 52.dp

/**
 * Etiket oluşturma ve düzenleme için ortak alt sayfa: ad, renk ve simge.
 *
 * [existing] null ise oluşturma, doluysa düzenleme modunda açılır — iki ayrı
 * ekran tutmak yerine tek bir yerde, çünkü alanlar birebir aynı.
 *
 * Tasarım notu: eskiden `AlertDialog` içinde sarmalanan iki ızgara vardı
 * (11 renk 3 satıra, 16 simge 4 satıra iniyordu) ve sayfanın boyunun çoğunu
 * bunlar kaplıyordu. Şimdi ikisi de tek satırda, yatay kaydırmalı; yükseklik
 * seçenek sayısından bağımsız sabit kalıyor.
 */
@Composable
fun LabelEditorDialog(
    existing: Label?,
    onConfirm: (name: String, color: NoteColor, icon: LabelIcon) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember { mutableStateOf(existing?.color ?: NoteColor.DEFAULT) }
    var icon by remember { mutableStateOf(existing?.icon ?: LabelIcon.LABEL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // İçerik kısa olduğu için sayfa kendiliğinden bodur duruyordu.
        // Ekranın belirli bir oranını kaplayarak nefes alanı kazanıyor;
        // yarı açık duraktan da doğrudan tam açık hâle geliyor.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // Küçük ekranlarda ızgaralar taşabilir; kaydırma güvenlik ağı.
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
        ) {
            LabelHero(name = name, color = color, icon = icon)

            Spacer(modifier = Modifier.height(Spacing.xxl))

            RoundedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(Res.string.label_name),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))
            SectionTitle(stringResource(Res.string.label_color))
            PickerGrid {
                NoteColor.entries.forEach { option ->
                    ColorSwatch(
                        selected = option == color,
                        background = option.swatchColor(),
                        // Seçili kutucuk seçili simgeyi de gösteriyor: iki
                        // seçiciyi bağlar, eşleşme önizlemeye bakmadan görünür.
                        icon = if (option == color) icon else null,
                        onClick = { color = option },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            SectionTitle(stringResource(Res.string.label_icon))
            PickerGrid {
                LabelIcon.entries.forEach { option ->
                    IconSwatch(
                        selected = option == icon,
                        icon = option,
                        selectedBackground = color.swatchColor(),
                        onClick = { icon = option },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(BUTTON_HEIGHT),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(
                    onClick = { onConfirm(name, color, icon) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT),
                ) {
                    Text(
                        stringResource(
                            if (existing == null) Res.string.label_create else Res.string.action_save,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

/**
 * DEFAULT rengin kendi karşılığı yok; palette `surface`e düşüyor ve seçicide
 * zeminle aynı renge karışıyor. Nötr bir kapsayıcı tonuna çeviriyoruz.
 */
@Composable
private fun NoteColor.swatchColor(): Color =
    if (this == NoteColor.DEFAULT) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        toContainerColor()
    }

/** Etiketin gerçek boyutunda önizlemesi: seçilen renk + simge, altında adı. */
@Composable
private fun LabelHero(name: String, color: NoteColor, icon: LabelIcon) {
    val placeholder = stringResource(Res.string.label_new)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(color.swatchColor()),
        ) {
            Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = name.ifBlank { placeholder },
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = if (name.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.sm),
    )
}

/**
 * Sarmalayan seçici ızgarası.
 *
 * Önceden tek satır yatay kaydırmalıydı; seçeneklerin çoğu ekran dışında
 * kalıyor ve kullanıcı hepsini görmek için yatay kaydırmak zorunda kalıyordu.
 * Alt sayfa artık yeterince yüksek olduğu için hepsi aynı anda görünüyor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickerGrid(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun ColorSwatch(
    selected: Boolean,
    background: Color,
    icon: LabelIcon?,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PICKER_ITEM)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun IconSwatch(
    selected: Boolean,
    icon: LabelIcon,
    selectedBackground: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PICKER_ITEM)
            .clip(CircleShape)
            .background(
                // Seçili simge, seçili rengin zeminini alıyor.
                if (selected) selectedBackground else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
    }
}
