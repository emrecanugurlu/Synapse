# Synapse — Gizlilik Politikası

**Son güncelleme:** 8 Ağustos 2026
**Geliştirici:** CN Software
**İletişim:** emrecnugurlu@gmail.com

---

## Kısaca

Synapse notlarınızı yalnızca cihazınızda saklar. Hesap açmanız gerekmez, geliştiriciye ait bir sunucu yoktur, reklam ve analiz aracı kullanılmaz.

## Topladığımız veriler

**Hiçbiri.** Synapse uygulamasının sunucusu yoktur. Uygulama size ait hiçbir veriyi geliştiriciye veya üçüncü taraflara aktarmaz.

Oluşturduğunuz içerik — notlar, liste maddeleri, etiketler, renkler, simgeler ve hatırlatıcı zamanları — cihazınızdaki uygulama alanında, yerel bir veritabanında tutulur. Uygulamayı kaldırdığınızda bu veriler cihazdan silinir.

## Verilerinizin cihaz dışına çıkabileceği durumlar

Bu durumların hepsini siz başlatırsınız; hiçbiri kendiliğinden olmaz.

**Yedekleme.** Ayarlar bölümünden bir yedek dosyası oluşturabilirsiniz. Bu dosya notlarınızın tamamını okunabilir metin (JSON) olarak içerir ve **sizin seçtiğiniz konuma** yazılır. Konumu siz belirlediğiniz için dosyanın nereye gittiğinden ve kimlerin erişebileceğinden siz sorumlusunuz. Örneğin bulut senkronizasyonu yapan bir klasör seçerseniz, yedek o bulut hizmetine de yüklenir.

**Otomatik yedekleme.** Açarsanız, uygulama seçtiğiniz klasöre belirlediğiniz sıklıkta (günlük veya haftalık) aynı biçimde yedek yazar. Bu özellik varsayılan olarak kapalıdır ve klasör seçmeden çalışmaz. İstediğiniz an Ayarlar'dan kapatabilirsiniz.

**Paylaşma.** Bir notu paylaştığınızda, notun metni sistemin paylaşım menüsü aracılığıyla seçtiğiniz uygulamaya iletilir. O noktadan sonra veriniz hedef uygulamanın gizlilik politikasına tabidir.

**Cihaz yedeği.** Android'in kendi yedekleme özelliği (Google Yedekleme) etkinse, işletim sistemi uygulama verilerini Google hesabınıza yedekleyebilir. Bu işlem Android tarafından yürütülür; geliştiricinin bu verilere erişimi yoktur. Cihazınızın ayarlarından kapatabilirsiniz.

## Dosya erişimi

Uygulama cihazınızdaki dosyalara genel erişim istemez. Yedek alırken veya geri yüklerken sistemin dosya seçicisi açılır ve **yalnızca sizin seçtiğiniz dosya ya da klasöre** erişilir. Otomatik yedekleme için seçtiğiniz klasörün izni cihazınızda saklanır; bu izni Android ayarlarından istediğiniz an geri alabilirsiniz.

## İzinler

| İzin | Neden gerekli |
|---|---|
| Bildirimler (`POST_NOTIFICATIONS`) | Kurduğunuz hatırlatıcıları gösterebilmek için. Vermezseniz uygulamanın geri kalanı çalışmaya devam eder, yalnızca hatırlatıcı bildirimi görünmez. |
| Tam zamanlı alarm (`SCHEDULE_EXACT_ALARM`) | Hatırlatıcının belirlediğiniz dakikada çalması için. Bu izin olmadan bildirim gecikmeli gelebilir. |
| Yeniden başlatmada çalıştırma (`RECEIVE_BOOT_COMPLETED`) | Cihaz kapanıp açıldığında kurulu hatırlatıcıların yeniden zamanlanması için. |

Uygulama internet izni istemez ve ağ bağlantısı kurmaz.

## Çocukların gizliliği

Synapse çocuklara yönelik değildir ve yaş, kimlik veya konum gibi kişisel bilgi toplamaz.

## Üçüncü taraf hizmetler

Uygulama reklam ağı, analiz veya çökme raporlama hizmeti içermez. Yalnızca açık kaynak yazılım kütüphaneleri kullanılır ve bunların hiçbiri veri toplamaz.

## Verilerinizi silme

Notlarınız yalnızca cihazınızda olduğu için uygulamayı kaldırmak verilerinizi siler. Uygulama içinden tek tek not silebilir, çöp kutusunu boşaltabilir veya cihaz ayarlarından uygulama verilerini temizleyebilirsiniz. Geliştiricinin silinecek bir kopyası yoktur.

## Bu politikadaki değişiklikler

Politika güncellenirse bu sayfadaki "Son güncelleme" tarihi değişir. Veri işleme biçimini etkileyen önemli bir değişiklik olursa uygulama içinde bilgilendirme yapılır.

## İletişim

Soru veya talepleriniz için: **emrecnugurlu@gmail.com**

---

# Synapse — Privacy Policy

**Last updated:** 8 August 2026
**Developer:** CN Software
**Contact:** emrecnugurlu@gmail.com

## In short

Synapse keeps your notes on your device only. No account is required, the developer runs no server, and the app contains no ads or analytics.

## Data we collect

**None.** Synapse has no server. The app sends none of your data to the developer or to any third party.

The content you create — notes, list items, labels, colours, icons and reminder times — is stored in a local database inside the app's own storage on your device. Uninstalling the app removes it.

## When your data can leave the device

You start every one of these; none happens on its own.

**Backup.** From Settings you can create a backup file. It contains all of your notes as readable text (JSON) and is written to **a location you choose**. Because you choose the location, you control where the file goes and who can reach it. If you pick a folder that syncs to a cloud service, the backup is uploaded there too.

**Automatic backup.** If you turn it on, the app writes the same kind of file to your chosen folder at the interval you set (daily or weekly). This is off by default and does nothing until you pick a folder. You can turn it off in Settings at any time.

**Sharing.** When you share a note, its text is passed through the system share sheet to the app you select. From that point your data is governed by that app's privacy policy.

**Device backup.** If Android's own backup feature is enabled, the operating system may back up app data to your Google account. This is handled by Android; the developer has no access to it. You can disable it in your device settings.

## File access

The app does not request broad access to your files. When you back up or restore, the system file picker opens and only the file or folder **you select** is accessed. The permission for a folder chosen for automatic backup is stored on your device, and you can revoke it from Android settings at any time.

## Permissions

| Permission | Why it is needed |
|---|---|
| Notifications (`POST_NOTIFICATIONS`) | To show the reminders you set. Without it the rest of the app still works; only reminder notifications are missing. |
| Exact alarms (`SCHEDULE_EXACT_ALARM`) | So a reminder fires at the minute you chose. Without it notifications may be delayed. |
| Run on boot (`RECEIVE_BOOT_COMPLETED`) | To re-schedule your reminders after the device restarts. |

The app does not request internet access and makes no network connections.

## Children's privacy

Synapse is not directed at children and collects no personal information such as age, identity or location.

## Third-party services

The app contains no advertising network, analytics or crash-reporting service. It uses open-source libraries only, none of which collect data.

## Deleting your data

Because your notes live only on your device, uninstalling the app deletes them. You can also delete individual notes, empty the trash, or clear the app's data from your device settings. The developer holds no copy to delete.

## Changes to this policy

If this policy is updated, the "Last updated" date on this page changes. If a change materially affects how data is handled, you will be told inside the app.

## Contact

Questions or requests: **emrecnugurlu@gmail.com**
