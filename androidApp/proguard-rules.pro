# Synapse — release (R8) kuralları

# --- Çökme izlerini okunabilir tut ---
# Satır numaraları korunur, orijinal dosya adı gizlenir.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata + generic tip bilgisi.
# Koin, tip tokenları üzerinden çözümleme yaptığı için Signature şart.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- Enum'lar (KRİTİK) ---
# NoteColor ve SyncStatus veritabanına AD olarak yazılıyor ve valueOf ile okunuyor.
# Sabit adları karıştırılırsa kayıtlı notların rengi/sync durumu bozulur.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.cnsoftware.synapse.domain.** { *; }

# --- Koin ---
# Modüllerde tip bazlı çözümleme (get<T>(), single<T>) yapıldığı için
# DI ile üretilen sınıfların yapıcıları korunmalı.
-keep class com.cnsoftware.synapse.di.** { *; }
-keep class com.cnsoftware.synapse.data.NoteRepositoryImpl { <init>(...); }
-keep class com.cnsoftware.synapse.ui.**ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# --- SQLDelight / sürücüler ---
-keep class com.cnsoftware.synapse.db.** { *; }
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# --- Platform servisleri ---
# Manifest'ten adla örneklenen bileşenler.
-keep class com.cnsoftware.synapse.reminder.ReminderReceiver { *; }
-keep class com.cnsoftware.synapse.reminder.BootReceiver { *; }

# --- kotlinx.serialization (KRİTİK) ---
# Yedek dosyası @Serializable sınıflarla okunup yazılıyor. Serializer'lar
# derleyici tarafından üretilip ADLA (Companion.serializer / $$serializer)
# bulunuyor; R8 bunları "kullanılmıyor" sanıp atarsa yedekleme
# SerializationException ile çöker.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.cnsoftware.synapse.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.cnsoftware.synapse.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cnsoftware.synapse.backup.**$$serializer { *; }
-dontwarn kotlinx.serialization.**

# --- WorkManager ---
# Worker'lar sınıf ADIYLA, reflection ile örnekleniyor.
-keep class com.cnsoftware.synapse.backup.AutoBackupWorker { <init>(...); }
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# --- Room (KRİTİK: WorkManager iş kuyruğunu Room'da tutuyor) ---
# Room, derleme zamanında ürettiği `*_Impl` sınıflarını reflection ile,
# PARAMETRESİZ yapıcıdan örnekliyor. R8 bu yapıcıyı "çağıran yok" sanıp
# atınca WorkDatabase_Impl.<init> bulunamıyor ve uygulama
# androidx.startup.InitializationProvider içinde, daha Application.onCreate
# çalışmadan çöküyor.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-keep class androidx.room.RoomOpenHelper$Delegate { *; }
-dontwarn androidx.room.paging.**

# androidx.startup: sağlayıcı ve Initializer'lar adla bulunuyor.
-keep class * implements androidx.startup.Initializer { <init>(); }

# --- Coroutines ---
# Kütüphane kendi consumer kurallarını getiriyor; ek güvence:
-dontwarn kotlinx.coroutines.**

# --- Compose ---
# Compose kendi kurallarını sağlıyor, ek kural gerekmiyor.
