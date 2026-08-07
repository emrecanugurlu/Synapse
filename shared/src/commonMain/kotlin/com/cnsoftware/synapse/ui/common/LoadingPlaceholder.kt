package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Göstergenin kendisinin "yanıp sönmemesi" için beklenen süre. */
private const val SPINNER_DELAY_MILLIS = 150L

/**
 * Veri yüklenirken gösterilen alan.
 *
 * Yerel veritabanı okuması genelde çok hızlı bittiği için gösterge hemen
 * çizilirse kendisi bir flaş yaratır. Bu yüzden kısa bir gecikmeden sonra
 * beliriyor: hızlı yüklemede kullanıcı hiçbir şey görmez, yavaş yüklemede
 * bekleme geri bildirimi alır.
 */
@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    var showSpinner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(SPINNER_DELAY_MILLIS)
        showSpinner = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (showSpinner) {
            // Expressive LoadingIndicator: dönen halka yerine şekilden şekile
            // dönüşen bir gösterge. Kısa beklemeler için önerilen bileşen.
            LoadingIndicator()
        }
    }
}
