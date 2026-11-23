package com.nijobenny.wayfinder.domain.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object ClockFormat {
    /** Local wall-clock time like "8:42 PM" for an epoch-millis timestamp. */
    @OptIn(ExperimentalTime::class)
    fun time(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val hour12 = (t.hour % 12).let { if (it == 0) 12 else it }
        val minute = t.minute.toString().padStart(2, '0')
        return "$hour12:$minute ${if (t.hour < 12) "AM" else "PM"}"
    }

    /** "23 Nov" */
    @OptIn(ExperimentalTime::class)
    fun date(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val day = t.date.toString().substringAfterLast('-').trimStart('0')
        return "$day ${months[t.month.ordinal]}"
    }
}
