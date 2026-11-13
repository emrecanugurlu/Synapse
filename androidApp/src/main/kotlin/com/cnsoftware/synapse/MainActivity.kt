package com.cnsoftware.synapse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.cnsoftware.synapse.backup.AndroidFilePicker
import com.cnsoftware.synapse.backup.AndroidFolderPicker
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.ThemeMode
import org.koin.compose.koinInject

/** Launcher kısayollarının kullandığı intent action'ları. */
private const val ACTION_NEW_NOTE = "com.cnsoftware.synapse.NEW_NOTE"
private const val ACTION_NEW_CHECKLIST = "com.cnsoftware.synapse.NEW_CHECKLIST"

// Yedek dosyasının MIME tipi. Kaydederken "application/json" kullanılıyor;
// bazı dosya sağlayıcıları JSON'u böyle etiketlemediği için okuma tarafında
// joker tip de kabul ediliyor (aşağıda openDocument).
private const val BACKUP_MIME = "application/json"

class MainActivity :
    ComponentActivity(),
    AndroidFilePicker.Host,
    AndroidFolderPicker.Host {

    /**
     * Açılışı tetikleyen intent. Uygulama zaten açıkken gelen intent'ler
     * [onNewIntent] üzerinden buraya yazılır; `mutableStateOf` olduğu için
     * Compose bunu görüp editörü açar.
     */
    private var startAction by mutableStateOf<StartAction?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sonuç önemli değil */ }

    // Storage Access Framework sonuçları askıdaki coroutine'e geri veriliyor.
    // Launcher'lar Activity kurulurken kaydedilmek zorunda; bu yüzden burada.
    private var pendingUri: ((Uri?) -> Unit)? = null

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
            pendingUri?.invoke(uri)
            pendingUri = null
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingUri?.invoke(uri)
            pendingUri = null
        }

    override suspend fun createDocument(suggestedName: String): Uri? =
        suspendCancellableCoroutine { continuation ->
            pendingUri = { uri -> continuation.resume(uri) }
            createDocumentLauncher.launch(suggestedName)
            continuation.invokeOnCancellation { pendingUri = null }
        }

    private var pendingFolder: ((Uri?) -> Unit)? = null

    private val openTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Kalıcı izin: uygulama yeniden başlasa da klasör erişilebilir
                // kalsın. Bu olmadan arka plandaki yedek işi yazamaz.
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            pendingFolder?.invoke(uri)
            pendingFolder = null
        }

    override suspend fun chooseFolder(): Uri? =
        suspendCancellableCoroutine { continuation ->
            pendingFolder = { uri -> continuation.resume(uri) }
            openTreeLauncher.launch(null)
            continuation.invokeOnCancellation { pendingFolder = null }
        }

    override suspend fun openDocument(): Uri? =
        suspendCancellableCoroutine { continuation ->
            pendingUri = { uri -> continuation.resume(uri) }
            // JSON'u "text/plain" veya "application/octet-stream" olarak
            // etiketleyen sağlayıcılar da var; hepsi listelensin.
            openDocumentLauncher.launch(arrayOf(BACKUP_MIME, "text/plain", "*/*"))
            continuation.invokeOnCancellation { pendingUri = null }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate'ten önce çağrılmalı.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        // Yalnızca ilk açılışta: ekran döndürme veya süreç ölümünden sonra
        // Activity yeniden kurulurken aynı intent hâlâ duruyor. Yeniden
        // işlenirse paylaşılan metinle ikinci bir not açılıp kaydedilebiliyordu.
        if (savedInstanceState == null) {
            startAction = intent?.toStartAction()
        }
        AndroidFilePicker.host = this
        AndroidFolderPicker.host = this

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

            App(startAction = startAction)
        }
    }

    override fun onDestroy() {
        // Sızıntı olmasın: köprü Activity'ye referans tutuyor.
        if (AndroidFilePicker.host === this) AndroidFilePicker.host = null
        if (AndroidFolderPicker.host === this) AndroidFolderPicker.host = null
        super.onDestroy()
    }

    // Uygulama arka plandayken kısayola dokunulduğunda ya da metin
    // paylaşıldığında Activity yeniden oluşturulmaz; intent buradan gelir.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startAction = intent.toStartAction()
    }

    /**
     * Intent'i ortak modüldeki [StartAction]'a çevirir.
     * Tanımadığı intent'lerde null döner; uygulama normal açılır.
     */
    private fun Intent.toStartAction(): StartAction? = when {
        action == ACTION_NEW_NOTE -> StartAction.NewNote
        action == ACTION_NEW_CHECKLIST -> StartAction.NewChecklist
        action == Intent.ACTION_SEND && type == "text/plain" -> {
            val text = getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            // Bazı uygulamalar (tarayıcılar) sayfa başlığını SUBJECT'te yollar.
            val subject = getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
            if (text.isBlank() && subject.isBlank()) {
                null
            } else {
                StartAction.ShareText(title = subject, text = text)
            }
        }
        else -> null
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
