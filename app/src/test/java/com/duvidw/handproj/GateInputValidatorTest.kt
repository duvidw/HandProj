package com.duvidw.handproj

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GateInputValidatorTest {
    @Test
    fun validateRejectsInvalidValues() {
        val result = GateInputValidator.validate("", "0", "abc")
        assertTrue(result.hasError)
    }

    @Test
    fun validateAcceptsValidValues() {
        val result = GateInputValidator.validate("+1-234-567", "5", "1")
        assertFalse(result.hasError)
    }
}
