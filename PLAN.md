# Synapse — Geliştirme Planı

Marka: **Synapse**, ana renk: `#D99B21` (proje klasörü/paket adı `com.cnsoftware.synapse`).

Google Keep tarzı, Android + iOS'ta çalışan, notların önce cihazda (local) tutulduğu, ileride buluta taşınabilecek şekilde tasarlanmış bir not uygulaması.

## Teknoloji Seçimleri

- **Local veritabanı:** SQLDelight — KMP'de Android ve iOS için aynı SQL şemasından tip-güvenli Kotlin kodu üretir, reactive query (Flow) desteği var. (Alternatif: Room KMP; SQLDelight daha olgun ve iOS tarafında daha az sürtünmeli.)
- **DI:** Koin — KMP'de kurulumu basit, Android/iOS ayrımı gerektirmiyor.
- **State/Mimari:** MVVM — `ViewModel` (zaten `androidx-lifecycle-viewmodelCompose` bağımlılığı projede var) + `StateFlow`, Repository pattern.
- **Navigation:** Compose Multiplatform Navigation ya da basit state-tabanlı ekran geçişi (liste ↔ editör).

## Veri Modeli

**Note**
- `id: String` (UUID)
- `title: String`
- `content: String`
- `color: String` (tema rengi, Keep'teki gibi)
- `isPinned: Boolean`
- `isArchived: Boolean`
- `isDeleted: Boolean` (çöp kutusu, soft delete)
- `isChecklist: Boolean` + checklist item'ları (ayrı tablo: `id, noteId, text, isChecked, position`)
- `createdAt: Long`
- `updatedAt: Long`
- `remoteId: String?` — **şimdiden ekleniyor**, kullanılmayacak ama ileride sync için şema değişikliği/migration gerektirmeyecek
- `syncStatus: String` — `LOCAL_ONLY | SYNCED | PENDING | CONFLICT` (varsayılan `LOCAL_ONLY`)

**Label** (opsiyonel, etiketleme için): `id, name` + `NoteLabelCrossRef(noteId, labelId)`

Bu iki alanı (remoteId, syncStatus) başta boş/varsayılan değerle eklemek, buluta geçişte veritabanı migration'ını basitleştirir.

## Katmanlar

```
shared/
  data/       -> SQLDelight schema, NoteRepositoryImpl
  domain/     -> Note model, NoteRepository interface, use case'ler
  ui/         -> ekranlar, ViewModel'ler
```

`NoteRepository` bir **interface**: bugün `LocalNoteRepository`, yarın `SyncingNoteRepository` (local + remote'u birleştiren decorator) ile değiştirilebilir. UI katmanı bu değişimi hiç bilmeyecek.

## Fazlar

**Faz 0 — Altyapı** ✅
Bağımlılıklar eklendi (SQLDelight 2.3.2, Koin 4.2.2), klasör yapısı kuruldu.

**Faz 1 — Local veri katmanı** ✅
`Note.sq` + `Label.sq` şemaları, `NoteRepository` + SQLDelight implementasyonu, Flow ile reactive CRUD.

**Faz 2 — Not listesi (ana ekran)** ✅
Staggered grid, not kartları, arama çubuğu, FAB, boş state.

**Faz 3 — Not oluşturma/düzenleme** ✅
Başlık + içerik, renk seçici, checklist modu, 600ms debounce'lu otomatik kaydetme, geri tuşunda kaydet-ve-çık.

**Faz 4 — Organizasyon** ✅
Pin/unpin, arşiv + çöp kutusu ekranları, 7 günlük otomatik kalıcı silme, etiketler (oluştur/ata/filtrele, migration: 1.sqm), swipe ile arşivle/sil.

**Faz 5 — Cilalama** ✅
Dark mode (karanlık not paleti dahil), ekran geçiş animasyonları (AnimatedContent), grid item animasyonları (animateItem), sıralama seçenekleri.

**Faz 6 — Buluta taşıma (ileride)**
- Auth: Firebase Auth (Google Sign-In) ya da kendi backend'i
- Sync: Firebase Firestore (offline-first desteği hazır, hızlı entegrasyon) **veya** kendi REST API + arka planda periyodik sync
- Çakışma çözümü: başlangıç için `updatedAt` karşılaştırmalı last-write-wins yeterli
- Faz 0-5'te eklenen `remoteId`/`syncStatus` alanları sayesinde bu faz veri modelinde büyük değişiklik gerektirmeyecek

## Faz 5+ — Eklenen ekstralar

**Etkileşim**
- Undo snackbar'ı (arşivle/sil/toplu işlemler geri alınabilir) ✅
- Hatırlatıcılar: nota tarih/saat ekle, yerel bildirim al (migration: 2.sqm) ✅
  - Android: AlarmManager + BroadcastReceiver, POST_NOTIFICATIONS/SCHEDULE_EXACT_ALARM izinleri
  - iOS: UNUserNotificationCenter
  - Boot sonrası alarmlar BootReceiver ile veritabanından yeniden kurulur ✅
- Çoklu seçim (uzun basma) + toplu sabitle/paylaş/kopyala/arşivle/sil ✅
- Not paylaşma (Android ACTION_SEND, iOS UIActivityViewController) ✅
- Not kopyalama, liste/grid görünüm geçişi ✅
- Checklist: Enter ile araya yeni öğe, boş satırda backspace ile silme, tamamlananlar alta ✅
- Metin ↔ checklist dönüşümü içerik kaybetmiyor ✅

**Organizasyon**
- Etiket yönetimi ekranı: oluştur / yeniden adlandır / birleştir / sil ✅
- Arşiv ve Çöp Kutusu'nda arama + çoklu seçim ✅
- Çöp kutusunu tümünü boşalt ✅
- Sıralama ve görünüm tercihi kalıcı (PreferencesStore: SharedPreferences / NSUserDefaults) ✅

**Görsel kimlik**
- Material 3 Expressive tema (MaterialExpressiveTheme + expressive motion scheme + yuvarlak şekil ölçeği) ✅
- Marka: Synapse, `#D99B21`; özel tipografi ölçeği ve `Spacing` token sistemi ✅
- Navigation drawer (Notlar / Arşiv / Çöp / Etiketler), üst barda arama modu ✅
- Ortak `EmptyState` bileşeni, Sabitlenmiş/Diğerleri bölümleri ✅
- Launcher ikonu (adaptive + monochrome + legacy PNG) ve açılış ekranı (Android core-splashscreen, iOS UILaunchScreen) ✅

**Testler**
- Repository entegrasyon testleri (in-memory SQLite): CRUD, arşiv/çöp geçişleri, arama, purge, hatırlatıcı sorgusu, etiket birleştirme ✅
- ViewModel testleri: arama, toplu işlem + undo, debounce autosave, bayrak koruma, checklist Enter/backspace ✅
- Paylaşım formatlayıcı testleri (commonTest) ✅

## Yayın hazırlığı

- Sistem geri tuşu / jesti tüm ekranlarda doğru davranıyor (`BackHandler`) ✅
- R8 açık, keep kuralları yazıldı (enum adları kritik: renkler DB'ye ad olarak yazılıyor) ✅
- Release imzalama iskeleti (`keystore.properties`, `.gitignore`'da) ✅
- Ayarlar ekranı: tema (sistem/açık/koyu), çöp kutusu saklama süresi ✅
- Tam yerelleştirme: TR (varsayılan) + EN, Compose Resources + Android bildirim metinleri ✅
- Gizlilik politikası taslağı (`PRIVACY.md`) ve mağaza dosyası (`STORE_LISTING.md`) ✅
- iOS gizlilik manifesti (`PrivacyInfo.xcprivacy`) ✅

### Yayından önce yapılması gerekenler (elle)

- [ ] `PRIVACY.md` bir URL'de yayımlanmalı, link mağaza formlarına girilmeli
- [ ] `keystore.properties` + anahtar oluşturulup release imzalanmalı
- [ ] **`assembleRelease` cihazda test edilmeli** — R8 hataları yalnızca release'te görünür
- [ ] `PrivacyInfo.xcprivacy` Xcode'da hedefe ("Copy Bundle Resources") eklenmeli
- [ ] Ekran görüntüleri + Play öne çıkan grafik hazırlanmalı
- [ ] Play "Veri Güvenliği" formu ve içerik derecelendirme anketi doldurulmalı

## Sonraki sürümler için

- **Yedekleme/geri yükleme** (JSON dışa-içe aktarma) — bir sonraki güncellemede
- **Faz 6 — bulut senkronizasyonu** (şemadaki `remoteId`/`syncStatus` hazır)
- Görsel ekleme, ana ekran widget'ı, biyometrik kilit
- Zengin metin / Markdown desteği
- Çökme raporlama (Crashlytics/Sentry), UI testleri, CI
