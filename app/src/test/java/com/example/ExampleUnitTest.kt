package com.example

import com.example.core.model.KesMoney
import com.example.core.model.KeypadKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `KES formatting uses comma grouping and two decimal places`() {
        assertEquals("KES 1,250.00", KesMoney.formatMinor(125_000L))
        assertEquals("KES 15,000.00", KesMoney.formatMinor(1_500_000L))
        assertEquals("KES 250.50", KesMoney.formatMinor(25_050L))
        assertEquals("KES 0.00", KesMoney.formatMinor(0L))
    }

    @Test
    fun `monetary input parser accepts valid KES amounts and rejects invalid values`() {
        assertEquals(125_000L, KesMoney.parseToMinor("1250"))
        assertEquals(125_000L, KesMoney.parseToMinor("1,250.00"))
        assertEquals(25_050L, KesMoney.parseToMinor("250.5"))
        assertEquals(25_050L, KesMoney.parseToMinor("KES 250.50"))

        assertNull(KesMoney.parseToMinor(""))
        assertNull(KesMoney.parseToMinor("."))
        assertNull(KesMoney.parseToMinor("0", allowZero = false))
        assertEquals(0L, KesMoney.parseToMinor("0", allowZero = true))
        assertNull(KesMoney.parseToMinor("-100"))
        assertNull(KesMoney.parseToMinor("12.345"))
        assertNull(KesMoney.parseToMinor("12..5"))
        assertNull(KesMoney.parseToMinor("USD 100"))
        assertNull(KesMoney.parseToMinor("abc"))
    }

    @Test
    fun `custom numeric keypad reducer enforces single decimal, two decimal places, and leading zero rules`() {
        var buffer = ""
        // Leading zero handling: "0" then "5" becomes "5"
        buffer = KesMoney.applyKey(buffer, KeypadKey.Digit('0'))
        assertEquals("0", buffer)
        buffer = KesMoney.applyKey(buffer, KeypadKey.Digit('5'))
        assertEquals("5", buffer)

        // Clear and test decimal on empty -> "0."
        buffer = KesMoney.applyKey(buffer, KeypadKey.Clear)
        assertEquals("", buffer)
        buffer = KesMoney.applyKey(buffer, KeypadKey.DecimalPoint)
        assertEquals("0.", buffer)

        // Multiple decimal points prevented
        buffer = KesMoney.applyKey(buffer, KeypadKey.DecimalPoint)
        assertEquals("0.", buffer)

        // Max 2 decimal places
        buffer = KesMoney.applyKey(buffer, KeypadKey.Digit('5'))
        buffer = KesMoney.applyKey(buffer, KeypadKey.Digit('0'))
        buffer = KesMoney.applyKey(buffer, KeypadKey.Digit('9')) // ignored
        assertEquals("0.50", buffer)

        // Backspace removes last character
        buffer = KesMoney.applyKey(buffer, KeypadKey.Backspace)
        assertEquals("0.5", buffer)

        // Formatting on completion
        assertEquals("0.50", KesMoney.formatCompletedInput(buffer))
    }
}
