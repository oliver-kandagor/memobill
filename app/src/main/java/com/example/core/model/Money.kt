package com.example.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface KeypadKey {
    data class Digit(val char: Char) : KeypadKey
    data object DecimalPoint : KeypadKey
    data object Backspace : KeypadKey
    data object Clear : KeypadKey
}

object KesMoney {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    /**
     * Formats integer minor units (cents, where 100 minor units = 1.00 KES)
     * into "KES 1,250.00".
     */
    fun formatMinor(amountMinor: Long): String {
        val major = BigDecimal.valueOf(amountMinor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        return "KES ${decimalFormat.format(major)}"
    }

    fun formatMinorOrNull(amountMinor: Long?): String? {
        return amountMinor?.let { formatMinor(it) }
    }

    /**
     * Parses a user-entered KES string into minor units (cents).
     * Rejects negative values, non-KES prefixes, >2 decimals, or malformed text.
     */
    fun parseToMinor(rawInput: String, allowZero: Boolean = false): Long? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val withoutPrefix = if (trimmed.startsWith("KES", ignoreCase = true)) {
            trimmed.substring(3).trim()
        } else {
            trimmed
        }

        val cleaned = withoutPrefix.replace(",", "").trim()
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (!cleaned.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) return null

        return try {
            val bd = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            val minor = bd.multiply(BigDecimal.valueOf(100)).longValueExact()
            if (minor < 0L) null
            else if (!allowZero && minor == 0L) null
            else minor
        } catch (_: Exception) {
            null
        }
    }

    fun minorToRawInput(amountMinor: Long?): String {
        if (amountMinor == null || amountMinor <= 0L) return ""
        val whole = amountMinor / 100L
        val cents = amountMinor % 100L
        return if (cents == 0L) {
            whole.toString()
        } else {
            String.format(Locale.US, "%d.%02d", whole, cents)
        }
    }

    fun formatCompletedInput(rawBuffer: String): String {
        val minor = parseToMinor(rawBuffer, allowZero = true) ?: return ""
        val major = BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        return decimalFormat.format(major)
    }

    fun formatLiveKeypadDisplay(rawBuffer: String): String {
        val cleaned = rawBuffer.replace(",", "").trim()
        if (cleaned.isEmpty()) return "KES 0.00"
        val parts = cleaned.split(".")
        val intPartStr = parts.firstOrNull().orEmpty().ifEmpty { "0" }
        val intFormatted = try {
            val intVal = intPartStr.toLong()
            DecimalFormat("#,###", symbols).format(intVal)
        } catch (_: Exception) {
            intPartStr
        }
        return if (parts.size > 1) {
            "KES $intFormatted.${parts[1]}"
        } else {
            "KES $intFormatted"
        }
    }

    /**
     * Pure reducer for the custom numeric keypad buffer.
     * Enforces:
     * - Single decimal point
     * - At most 2 digits after decimal point
     * - Clean leading zero replacement ("0" + "5" -> "5", "" + "." -> "0.")
     * - Maximum 9 whole digits to prevent overflow
     */
    fun applyKey(currentBuffer: String, key: KeypadKey): String {
        val clean = currentBuffer.replace(",", "").trim()
        return when (key) {
            is KeypadKey.Clear -> ""
            is KeypadKey.Backspace -> {
                if (clean.isEmpty()) "" else clean.dropLast(1)
            }
            is KeypadKey.DecimalPoint -> {
                when {
                    clean.contains('.') -> clean
                    clean.isEmpty() -> "0."
                    else -> "$clean."
                }
            }
            is KeypadKey.Digit -> {
                val d = key.char
                if (d !in '0'..'9') return clean
                if (clean.contains('.')) {
                    val decimals = clean.substringAfter('.')
                    if (decimals.length >= 2) clean else "$clean$d"
                } else {
                    when {
                        clean == "0" && d == '0' -> "0"
                        clean == "0" && d != '0' -> d.toString()
                        clean.length >= 9 -> clean
                        else -> "$clean$d"
                    }
                }
            }
        }
    }
}

object DateUtils {
    fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dayPeriodKey(timeMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(startOfDayMillis(timeMillis)))
    }

    fun formatShortDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return sdf.format(Date(timeMillis))
    }

    fun formatRelativeDueLabel(dueDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val todayStart = startOfDayMillis(nowMillis)
        val dueStart = startOfDayMillis(dueDateMillis)
        val diffDays = ((dueStart - todayStart) / 86_400_000L).toInt()
        return when {
            diffDays < -1 -> "Overdue by ${-diffDays} days · ${formatShortDate(dueDateMillis)}"
            diffDays == -1 -> "Overdue since yesterday · ${formatShortDate(dueDateMillis)}"
            diffDays == 0 -> "Due today · ${formatShortDate(dueDateMillis)}"
            diffDays == 1 -> "Due tomorrow · ${formatShortDate(dueDateMillis)}"
            diffDays in 2..7 -> "Due in $diffDays days · ${formatShortDate(dueDateMillis)}"
            else -> "Due ${formatShortDate(dueDateMillis)}"
        }
    }

    fun advanceDueDate(dueDateMillis: Long, recurrence: BillRecurrence): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDayMillis(dueDateMillis)
        when (recurrence) {
            BillRecurrence.NONE -> Unit
            BillRecurrence.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            BillRecurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillRecurrence.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            BillRecurrence.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
