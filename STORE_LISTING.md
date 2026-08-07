# Synapse — Mağaza Yayın Dosyası

Bu dosya Play Console ve App Store Connect'e girilecek metinleri ve hazırlanması gereken görselleri toplar.

---

## 1. Temel bilgiler

| Alan | Değer |
|---|---|
| Uygulama adı | Synapse |
| Paket adı / Bundle ID | `com.cnsoftware.synapse` |
| Kategori | Verimlilik (Productivity) |
| Ücret | Ücretsiz, uygulama içi satın alma yok |
| Reklam | Yok |
| Hedef kitle yaşı | 13+ (Play: "Herkes", App Store: 4+) |
| Diller | Türkçe (varsayılan), İngilizce |
| Gizlilik politikası URL'i | **(doldurulacak — PRIVACY.md bir web adresinde yayımlanmalı)** |

> Gizlilik politikası bir URL olarak erişilebilir olmalı. GitHub Pages, Notion herkese açık sayfa veya kendi alan adın işe yarar; mağazalar dosya yüklemeyi kabul etmiyor.

---

## 2. Play Store metinleri (Türkçe)

**Kısa açıklama** (en fazla 80 karakter)

```
Notlarını ve listelerini topla; hatırlatıcı kur, etiketle, cihazında sakla.
```

**Tam açıklama** (en fazla 4000 karakter)

```
Synapse, günlük notlarını ve yapılacak listelerini tek yerde toplayan sade bir not defteri. Hesap açman gerekmez, internet bağlantısı istemez — notların yalnızca senin cihazında kalır.

NE YAPABİLİRSİN

• Hızlı not al, ya da doğrudan yapılacaklar listesi oluştur
• Listede Enter'a basarak yeni maddeye geç, maddeleri sürükleyerek sırala
• Tamamladıklarını işaretle; otomatik olarak listenin altına insinler
• Notlarını renklendir, etiketle, sabitle
• Hatırlatıcı kur; zamanı gelince bildirim al
• Aramayla istediğin notu anında bul
• Arşivle ya da çöp kutusuna at — yanlışlıkla sildiysen tek dokunuşla geri al

DÜZENLİ KALSIN

• Sabitlenmiş notlar listenin üstünde ayrı bir bölümde
• Etiketlerle grupla, yan menüden tek dokunuşla filtrele
• Izgara ve liste görünümü arasında geçiş yap
• Son düzenlenme, oluşturma tarihi veya renge göre sırala
• Birden fazla notu seçip topluca arşivle, paylaş, kopyala veya sil

TASARIM

• Material 3 Expressive ile hazırlanmış arayüz
• Açık ve koyu tema; istersen cihaz temasını takip eder
• Türkçe ve İngilizce

GİZLİLİK

Synapse'in sunucusu yok. Notların cihazından çıkmaz, reklam ve analiz aracı içermez. İstediğin izinler yalnızca hatırlatıcıların çalışması içindir.
```

---

## 3. Play Store metinleri (İngilizce)

**Kısa açıklama**

```
Keep your notes and lists together: reminders, labels, colors — all on device.
```

**Tam açıklama**

```
Synapse is a simple notebook that keeps your everyday notes and to-do lists in one place. No account, no internet connection required — your notes stay on your device.

WHAT YOU CAN DO

• Jot down a quick note, or start a checklist right away
• Press Enter to add the next item, drag items to reorder them
• Check things off — completed items move to the bottom automatically
• Color, label and pin your notes
• Set a reminder and get a notification when it's due
• Find any note instantly with search
• Archive or trash notes — and undo with one tap if it was a mistake

STAY ORGANIZED

• Pinned notes get their own section at the top
• Group with labels and filter from the side menu
• Switch between grid and list views
• Sort by last edited, date created or color
• Select multiple notes to archive, share, duplicate or delete at once

DESIGN

• Built with Material 3 Expressive
• Light and dark themes, or follow your device
• English and Turkish

PRIVACY

Synapse has no server. Your notes never leave your device, and the app contains no ads or analytics. The permissions it asks for exist only so reminders can work.
```

---

## 4. Play Console — Veri Güvenliği formu cevapları

| Soru | Cevap |
|---|---|
| Uygulama kullanıcı verisi topluyor mu? | **Hayır** |
| Uygulama kullanıcı verisi paylaşıyor mu? | **Hayır** |
| Veriler aktarım sırasında şifreleniyor mu? | Uygulanamaz (ağ trafiği yok) |
| Kullanıcı verilerin silinmesini isteyebilir mi? | Evet — uygulama içinden silinebilir veya uygulama kaldırılabilir |

> Not: Android sistem yedeklemesi (`allowBackup="true"`) veriyi Google hesabına yedekleyebilir. Bu Play'in "veri toplama" tanımına girmez (geliştirici erişmez) ama gizlilik politikasında açıkça belirtildi.

---

## 5. App Store Connect

- **Gizlilik etiketleri:** "Data Not Collected" seçilir.
- **PrivacyInfo.xcprivacy** dosyası eklenmeli. Kullanılan Required Reason API'leri: `UserDefaults` (tercih saklama, sebep kodu `CA92.1`). Bunun dışında dosya zaman damgası/disk alanı/aktif klavye API'si kullanılmıyor.
- **Yaş sınırı:** 4+
- **Telif hakkı:** © 2026 CN Software

---

## 6. Hazırlanacak görseller

| Öğe | Gereksinim | Durum |
|---|---|---|
| Uygulama ikonu (Play) | 512×512 PNG | Kaynak vektör hazır, dışa aktarılmalı |
| Öne çıkan grafik (Play) | 1024×500 PNG | **Yapılacak** |
| Telefon ekran görüntüsü (Play) | En az 2, en fazla 8; 1080×1920 önerilir | **Yapılacak** |
| Tablet ekran görüntüsü (Play) | 7" ve 10" — isteğe bağlı ama önerilir | **Yapılacak** |
| App Store ikonu | 1024×1024 PNG (saydamlık yok) | Hazır (`AppIcon.appiconset`) |
| iPhone ekran görüntüsü | 6.7" ve 6.5" boyutları zorunlu | **Yapılacak** |

**Ekran görüntüsü için önerilen 5 kare:**
1. Ana ekran — renkli, dolu bir not ızgarası
2. Yapılacaklar listesi — bazı maddeler işaretli
3. Not editörü — renk seçici açık
4. Hatırlatıcı kurulmuş bir not
5. Koyu tema

---

## 7. Yayın öncesi kontrol listesi

- [ ] Gizlilik politikası bir URL'de yayımlandı
- [ ] `keystore.properties` oluşturuldu ve release imzalandı
- [ ] `assembleRelease` ile R8 açık derleme cihazda test edildi (renkler, hatırlatıcı, açılış)
- [ ] `versionCode` / `versionName` gözden geçirildi
- [ ] Ekran görüntüleri ve öne çıkan grafik hazırlandı
- [ ] Play "Veri Güvenliği" formu dolduruldu
- [ ] iOS için `PrivacyInfo.xcprivacy` eklendi
- [ ] İçerik derecelendirme anketi tamamlandı
- [ ] Türkçe ve İngilizce mağaza metinleri girildi
