package com.cnsoftware.synapse

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.ThemeMode
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sonuç önemli değil */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate'ten önce çağrılmalı.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            val settings = koinInject<AppSettings>()
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            // Compose Foundation bu modülde yok; sistem temasını Android
            // yapılandırmasından okuyoruz. LocalConfiguration izlendiği için
            // cihaz teması değişince bu değer de güncellenir.
            val configuration = LocalConfiguration.current
            val systemDark =
                (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Durum/gezinme çubuğu ikonlarının rengi UYGULAMANIN temasına göre
            // belirlenmeli. Varsayılan enableEdgeToEdge() cihazın temasına bakıyor;
            // kullanıcı ayarlardan farklı bir tema seçtiğinde ikonlar zemine
            // karışıp görünmez oluyordu (koyu zemin + koyu saat/batarya).
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    ) { darkTheme },
                )
            }

            App()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
