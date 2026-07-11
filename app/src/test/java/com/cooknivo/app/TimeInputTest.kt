package com.cooknivo.app

import com.cooknivo.app.util.TimeInput
import com.cooknivo.app.util.TextLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeInputTest {

    @Test
    fun blankIsNull() {
        assertNull(TimeInput.parseMinutes(""))
        assertNull(TimeInput.parseMinutes("   "))
    }

    @Test
    fun nonNumericIsNull() {
        assertNull(TimeInput.parseMinutes("abc"))
    }

    @Test
    fun negativeClampedToZero() {
        assertEquals(0, TimeInput.parseMinutes("-5"))
    }

    @Test
    fun overMaxClampedToMax() {
        assertEquals(TimeInput.MAX_MINUTES, TimeInput.parseMinutes("999999"))
    }

    @Test
    fun validParses() {
        assertEquals(45, TimeInput.parseMinutes("45"))
    }

    @Test
    fun clampTrimsAndLimits() {
        assertEquals("hello", TextLimits.clamp("  hello  ", 120))
        assertEquals("abc", TextLimits.clamp("abcdef", 3))
    }
}
