package com.cnsoftware.synapse.data

import com.cnsoftware.synapse.domain.Note

/**
 * Aramada karşılaştırma için metni sadeleştirir.
 *
 * Arama SQL `LIKE` ile değil burada yapılıyor, çünkü SQLite'ın büyük/küçük harf
 * duyarsızlığı yalnızca ASCII'yi kapsıyor: "çay" araması "Çay"ı bulmuyordu.
 *
 * `lowercase()` Ç/Ş/Ö/Ü/Ğ için doğru çalışıyor ama I/İ/ı/i dörtlüsünde Türkçe
 * kuralını bilmiyor ("I" -> "i", "İ" -> "i" + birleşen nokta). Dördünü de "i"ye
 * indirgiyoruz: "ISI", "ısı" ve "isi" birbirini bulur. Arama için bu gevşeklik,
 * yazılanın bulunamamasından iyidir.
 */
internal fun String.foldForSearch(): String = buildString(length) {
    for (char in this@foldForSearch) {
        when (char) {
            'İ', 'I', 'ı' -> append('i')
            // Birleşen nokta (U+0307): "İ"nin ayrıştırılmış hâlinden gelebilir.
            '̇' -> Unit
            else -> append(char)
        }
    }
}.lowercase()

/**
 * [foldedQuery] ([foldForSearch] ile sadeleştirilmiş) başlıkta, içerikte veya
 * liste maddelerinden birinde geçiyor mu. Liste notlarında içerik boş olduğu
 * için maddelere bakılmazsa bu notlar aramada hiç çıkmaz.
 */
internal fun Note.matchesSearch(foldedQuery: String): Boolean =
    title.foldForSearch().contains(foldedQuery) ||
        content.foldForSearch().contains(foldedQuery) ||
        checklistItems.any { it.text.foldForSearch().contains(foldedQuery) }
