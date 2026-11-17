package com.cnsoftware.synapse.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.ui.theme.Spacing

/** Boş ekranlar için ortak görsel dil: yumuşak daire içinde ikon, başlık ve açıklama. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Düz daire yerine asimetrik, "çizilmiş" hissi veren bir form.
        // Boş ekran uygulamanın en sessiz anı; karakteri buradan geliyor.
        //
        // NOT: Burada `MaterialShapes.Cookie9Sided` daha doğal dururdu ama onu
        // Shape'e çeviren `toShape()` uzantısı androidx.graphics.shapes'te ve
        // material3 bu kütüphaneyi dışarı açmıyor (implementation bağımlılığı).
        // Ek bağımlılık getirmemek için köşeleri farklı yarıçaplı bir form.
        // Köşe yarıçapları sonsuz döngüde yavaşça yer değiştirir: form "nefes alır".
        val breathe = rememberInfiniteTransition(label = "emptyBreathe")
        val big by breathe.animateFloat(
            initialValue = 48f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
            label = "emptyBig",
        )
        val small = 68f - big
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = big.dp,
                        topEnd = small.dp,
                        bottomEnd = big.dp,
                        bottomStart = small.dp,
                    ),
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = title,
            // Emphasized ölçek: bu ekrandaki tek odak noktası.
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
