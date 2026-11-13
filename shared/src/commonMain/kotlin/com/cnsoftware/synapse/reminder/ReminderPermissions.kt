package com.cnsoftware.synapse.reminder

/**
 * Hatırlatıcının gerçekten çalabilmesi için gereken sistem izinleri.
 *
 * İzinler uygulama açılır açılmaz değil, kullanıcı ilk hatırlatıcıyı kurduğunda
 * isteniyor: neden istendiği o anda belli, reddedilme olasılığı daha düşük.
 */
interface ReminderPermissions {

    /**
     * Bildirim gösterme izni var mı; yoksa ve sorulabiliyorsa kullanıcıya sorar.
     * Kullanıcı reddettiyse (ya da daha önce kalıcı olarak reddetmişse) false döner.
     */
    suspend fun ensureNotificationsAllowed(): Boolean

    /** Uygulamanın bildirim ayarlarını açar. */
    fun openNotificationSettings()

    /** Alarm tam belirlenen dakikada çalabiliyor mu. */
    fun canScheduleExactAlarms(): Boolean

    /** "Alarmlar ve hatırlatıcılar" izin ekranını açar. */
    fun openExactAlarmSettings()
}

/** Hatırlatıcı kurulduktan sonra kullanıcıya gösterilecek uyarı. */
enum class ReminderNotice {
    /** Bildirim izni yok: hatırlatıcı kuruldu ama görünmeyecek. */
    NOTIFICATIONS_BLOCKED,

    /** Tam zamanlı alarm izni yok: bildirim gecikebilir. */
    EXACT_ALARM_OFF,
}
