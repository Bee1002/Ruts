package com.example.ruts.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val SpanishLocale = Locale.forLanguageTag("es-ES")

fun formatWeekday(millis: Long): String {
    val formatted = SimpleDateFormat("EEEE", SpanishLocale).format(Date(millis))
    return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(SpanishLocale) else it.toString() }
}

fun formatTime(millis: Long): String {
    return SimpleDateFormat("H:mm", SpanishLocale).format(Date(millis))
}

fun formatFullDate(millis: Long): String {
    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", SpanishLocale).format(Date(millis))
}

fun formatShortRouteLabel(millis: Long): String {
    return "${formatWeekday(millis)} · ${formatFullDate(millis)}"
}

fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

fun todayMillis(): Long = System.currentTimeMillis()
