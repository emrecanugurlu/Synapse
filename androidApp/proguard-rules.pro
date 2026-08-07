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

# --- Coroutines ---
# Kütüphane kendi consumer kurallarını getiriyor; ek güvence:
-dontwarn kotlinx.coroutines.**

# --- Compose ---
# Compose kendi kurallarını sağlıyor, ek kural gerekmiyor.
