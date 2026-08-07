package com.cnsoftware.synapse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Synapse marka rengi. */
val SynapseGold = Color(0xFFD99B21)

private val LightColors = lightColorScheme(
    // Marka rengi koyu bir altın; üzerine beyaz yazı yeterli kontrast vermiyor,
    // bu yüzden onPrimary koyu kahve.
    primary = SynapseGold,
    onPrimary = Color(0xFF2E1F00),
    primaryContainer = Color(0xFFF9E3B3),
    onPrimaryContainer = Color(0xFF2A1D00),
    secondary = Color(0xFF6D5C3D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E1BC),
    onSecondaryContainer = Color(0xFF251A04),
    // Zemin ılık ve hafif koyu; kartlar (surface) onun üzerinde açık kalıp öne çıkıyor.
    background = Color(0xFFF3EEE4),
    onBackground = Color(0xFF1E1B14),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF1E1B14),
    surfaceVariant = Color(0xFFE8E0CF),
    onSurfaceVariant = Color(0xFF4D463A),
    outline = Color(0xFF857D6C),
    // Drawer, bottom sheet ve menüler bu rolleri kullanıyor. Tanımlanmazsa
    // Material'ın soğuk gri varsayılanları geliyor ve sıcak paletin yanında
    // yamalı duruyordu.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF8EF),
    surfaceContainer = Color(0xFFF7F2E9),
    surfaceContainerHigh = Color(0xFFF1ECE3),
    surfaceContainerHighest = Color(0xFFEBE6DD),
    surfaceDim = Color(0xFFDFDACF),
    surfaceBright = Color(0xFFFFFCF6),
)

private val DarkColors = darkColorScheme(
    // Koyu temada marka rengi okunabilirlik için bir tık açılıyor.
    primary = Color(0xFFECBD57),
    onPrimary = Color(0xFF3D2B00),
    primaryContainer = Color(0xFF574000),
    onPrimaryContainer = Color(0xFFF9E3B3),
    secondary = Color(0xFFDCC49A),
    onSecondary = Color(0xFF3D2F14),
    secondaryContainer = Color(0xFF544428),
    onSecondaryContainer = Color(0xFFF2E1BC),
    // Karanlık modda da aynı ilişki: zemin en koyu, kartlar bir ton açık.
    background = Color(0xFF17140D),
    onBackground = Color(0xFFEAE3D6),
    surface = Color(0xFF241F16),
    onSurface = Color(0xFFEAE3D6),
    surfaceVariant = Color(0xFF4A4335),
    onSurfaceVariant = Color(0xFFD2C7B2),
    outline = Color(0xFF9A9080),
    surfaceContainerLowest = Color(0xFF110F09),
    surfaceContainerLow = Color(0xFF1F1B13),
    surfaceContainer = Color(0xFF241F16),
    surfaceContainerHigh = Color(0xFF2E2920),
    surfaceContainerHighest = Color(0xFF39342A),
    surfaceDim = Color(0xFF17140D),
    surfaceBright = Color(0xFF3E382E),
)

/** Expressive dilin belirgin, yuvarlak köşe ölçeği. */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Uygulamanın *kendi* teması karanlık mı?
 *
 * `isSystemInDarkTheme()` yerine bu kullanılmalı: kullanıcı ayarlardan
 * cihazdan bağımsız bir tema seçebiliyor. İkisini karıştırmak, açık paletli
 * not kartlarının koyu temada açık renkli metinle çizilmesine (yani metnin
 * kaybolmasına) yol açıyordu.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            motionScheme = MotionScheme.expressive(),
            shapes = ExpressiveShapes,
            typography = SynapseTypography,
        ) {
            // Opak zemin: ekran geçişlerindeki çapraz solmada altta pencere
            // arka planı (beyaz) görünüp flaş yaratıyordu.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    }
}
