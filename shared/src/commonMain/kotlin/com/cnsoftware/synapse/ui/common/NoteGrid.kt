package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.ui.theme.Spacing

/** Başlıklı bir not grubu; başlık null ise başlıksız listelenir. */
data class NoteSection(
    val title: String?,
    val notes: List<Note>,
)

/** Bölümlü ızgara: her bölümün başlığı tam satır kaplar (ör. Sabitlenmiş / Diğerleri). */
@Composable
fun SectionedNoteGrid(
    sections: List<NoteSection>,
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells = StaggeredGridCells.Adaptive(minSize = 160.dp),
    empty: @Composable () -> Unit = {},
    content: @Composable (Note) -> Unit,
) {
    val isEmpty = sections.all { it.notes.isEmpty() }
    if (isEmpty) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            empty()
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = modifier,
            contentPadding = PaddingValues(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
            verticalItemSpacing = Spacing.gridGutter,
        ) {
            sections.forEachIndexed { sectionIndex, section ->
                if (section.notes.isEmpty()) return@forEachIndexed
                section.title?.let { title ->
                    item(key = "header_$sectionIndex", span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                        )
                    }
                }
                // Anahtar bölüme göre kapsamlanıyor.
                // Sadece not id'si kullanıldığında, bir not sabitlenip "Diğerleri"nden
                // "Sabitlenmiş"e taşındığında lazy ızgara onu "taşınan aynı öğe" sayıp
                // eski şerit (lane) bilgisiyle yeniden yerleştirmeye çalışıyor;
                // tam satır kaplayan başlık araya girdiği için öğe hiç çizilmiyordu.
                // Bölüm önekiyle öğe eskisi silinip yenisi oluşturulmuş gibi ele alınıyor.
                items(section.notes, key = { "s${sectionIndex}_${it.id}" }) { note ->
                    // NOT: Burada bilerek animateItem() kullanılmıyor.
                    // Staggered grid + tam satır kaplayan bölüm başlığı ("Sabitlenmiş"/"Diğerleri")
                    // ile birlikte yerleşim animasyonu kendini besleyen bir döngü kuruyordu:
                    // animasyon konumu değiştiriyor -> sütun yükseklikleri değişiyor ->
                    // yeniden yerleşim -> animasyon yeniden başlıyor. Sonuç: kartlar sürekli oynuyor.
                    content(note)
                }
            }
        }
    }
}

/** Tek bölümlük kullanım için kısayol (Arşiv/Çöp Kutusu ekranları). */
@Composable
fun NoteStaggeredGrid(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells = StaggeredGridCells.Adaptive(minSize = 160.dp),
    empty: @Composable () -> Unit = {},
    content: @Composable (Note) -> Unit,
) {
    SectionedNoteGrid(
        sections = listOf(NoteSection(title = null, notes = notes)),
        modifier = modifier,
        columns = columns,
        empty = empty,
        content = content,
    )
}
