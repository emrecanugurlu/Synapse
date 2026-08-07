package com.cnsoftware.synapse.util

expect fun currentTimeMillis(): Long

/**
 * Material DatePicker'ın verdiği "UTC gece yarısı" tarih değeriyle
 * saat/dakikayı yerel saat diliminde birleştirip epoch millis döndürür.
 */
expect fun combineDateTime(dateUtcMidnightMillis: Long, hour: Int, minute: Int): Long

/** Epoch millis'i "5 Ağu 14:30" gibi kısa, yerel bir gösterime çevirir. */
expect fun formatDateTime(millis: Long): String
