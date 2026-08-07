package com.cnsoftware.synapse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cnsoftware.synapse.domain.NoteColor

/** Google Keep'in açık tema paleti. */
private val lightPalette = mapOf(
    NoteColor.RED to Color(0xFFF28B82),
    NoteColor.ORANGE to Color(0xFFFBBC04),
    NoteColor.YELLOW to Color(0xFFFFF475),
    NoteColor.GREEN to Color(0xFFCCFF90),
    NoteColor.TEAL to Color(0xFFA7FFEB),
    NoteColor.BLUE to Color(0xFFCBF0F8),
    NoteColor.PURPLE to Color(0xFFD7AEFB),
    NoteColor.PINK to Color(0xFFFDCFE8),
    NoteColor.BROWN to Color(0xFFE6C9A8),
    NoteColor.GRAY to Color(0xFFE8EAED),
)

/** Karanlık tema için kısılmış tonlar (Keep'in dark paletine yakın). */
private val darkPalette = mapOf(
    NoteColor.RED to Color(0xFF5C2B29),
    NoteColor.ORANGE to Color(0xFF614A19),
    NoteColor.YELLOW to Color(0xFF635D19),
    NoteColor.GREEN to Color(0xFF345920),
    NoteColor.TEAL to Color(0xFF16504B),
    NoteColor.BLUE to Color(0xFF2D555E),
    NoteColor.PURPLE to Color(0xFF42275E),
    NoteColor.PINK to Color(0xFF5B2245),
    NoteColor.BROWN to Color(0xFF442F19),
    NoteColor.GRAY to Color(0xFF3C3F43),
)

@Composable
fun NoteColor.toContainerColor(): Color {
    // Sistem teması değil, uygulamanın kendi teması belirleyici:
    // kullanıcı cihazdan bağımsız bir tema seçmiş olabilir.
    val palette = if (LocalIsDarkTheme.current) darkPalette else lightPalette
    // DEFAULT paletin dışında: ılık zemin üzerinde açık kart yüzeyi olarak durur.
    return palette[this] ?: MaterialTheme.colorScheme.surface
}
