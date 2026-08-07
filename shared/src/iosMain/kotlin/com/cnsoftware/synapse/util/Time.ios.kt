package com.cnsoftware.synapse.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun combineDateTime(dateUtcMidnightMillis: Long, hour: Int, minute: Int): Long {
    val utcCalendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
        timeZone = NSTimeZone.timeZoneWithName("UTC")!!
    }
    val utcDate = NSDate.dateWithTimeIntervalSince1970(dateUtcMidnightMillis / 1000.0)
    val components = utcCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = utcDate,
    )
    val localComponents = NSDateComponents().apply {
        year = components.year
        month = components.month
        day = components.day
        this.hour = hour.toLong()
        this.minute = minute.toLong()
    }
    val localDate = NSCalendar.currentCalendar.dateFromComponents(localComponents) ?: return dateUtcMidnightMillis
    return (localDate.timeIntervalSince1970 * 1000).toLong()
}

actual fun formatDateTime(millis: Long): String {
    val formatter = NSDateFormatter().apply { dateFormat = "d MMM HH:mm" }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}
