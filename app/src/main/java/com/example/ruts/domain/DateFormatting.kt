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

fun formatCompactRouteDate(millis: Long): String {
    val day = SimpleDateFormat("d", SpanishLocale).format(Date(millis))
    val month = SimpleDateFormat("MMM", SpanishLocale).format(Date(millis)).lowercase(SpanishLocale)
    return "$day-$month."
}

fun formatWeekdayLowercase(millis: Long): String {
    return formatWeekday(millis).lowercase(SpanishLocale)
}

fun formatDrawerRoutePrefix(millis: Long): String {
    return "${formatCompactRouteDate(millis)} ${formatWeekdayLowercase(millis)}"
}

fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

fun todayMillis(): Long = System.currentTimeMillis()

fun tomorrowMillis(referenceMillis: Long = todayMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

fun isTomorrow(millis: Long, referenceMillis: Long = todayMillis()): Boolean {
    return isSameDay(millis, tomorrowMillis(referenceMillis))
}

fun formatAbbreviatedWeekday(millis: Long): String {
    val abbreviated = SimpleDateFormat("EEE", SpanishLocale).format(Date(millis)).lowercase(SpanishLocale)
    return if (abbreviated.endsWith(".")) abbreviated else "$abbreviated."
}

fun formatShortMonthDate(millis: Long): String {
    val day = SimpleDateFormat("d", SpanishLocale).format(Date(millis))
    val month = SimpleDateFormat("MMM", SpanishLocale).format(Date(millis)).lowercase(SpanishLocale)
    return "$day de $month."
}

fun formatRelativeDateOptionLabel(millis: Long, referenceMillis: Long = todayMillis()): String {
    val dayPrefix = when {
        isSameDay(millis, referenceMillis) -> "Hoy"
        isTomorrow(millis, referenceMillis) -> "Mañana"
        else -> formatWeekday(millis)
    }
    return "$dayPrefix ${formatAbbreviatedWeekday(millis)} ${formatShortMonthDate(millis)}"
}

fun formatElapsedDuration(startMillis: Long, endMillis: Long): String {
    val totalMinutes = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(0)
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
}
