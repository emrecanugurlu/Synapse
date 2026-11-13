package com.cnsoftware.synapse.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun combineDateTime(dateUtcMidnightMillis: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateUtcMidnightMillis
    }
    val local = Calendar.getInstance().apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            hour,
            minute,
            0,
        )
    }
    return local.timeInMillis
}

actual fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(millis))

actual fun formatBackupTimestamp(millis: Long): String =
    // Locale.ROOT: cihaz Arapça rakam kullanan bir yerelde olsa bile dosya adı
    // ASCII kalsın.
    SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date(millis))
