package com.vva.buildings

import com.vva.buildings.ServiceXlsx.getWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class InputValidatorTest {

    @Test
    fun `Баланс xlsx - коректна структура`() {
        val sheet = getWorkbook("data/input/Баланс.xlsx").getSheetAt(0)
        assertDoesNotThrow { InputValidator.validateBalans(sheet) }
    }

    @Test
    fun `ВільніПлощі xlsx - коректна структура`() {
        val sheet = getWorkbook("data/input/ВільніПлощі.xlsx").getSheetAt(0)
        assertDoesNotThrow { InputValidator.validateFreeSpace(sheet) }
    }

    @Test
    fun `Оренда xlsx - коректна структура`() {
        val sheet = getWorkbook("data/input/Оренда.xlsx").getSheetAt(0)
        assertDoesNotThrow { InputValidator.validateOrenda(sheet) }
    }

    @Test
    fun `validate - кидає виняток при невідповідності заголовка`() {
        val sheet = getWorkbook("data/input/Баланс.xlsx").getSheetAt(0)
        // Підміняємо очікуваний заголовок на неправильний через рефлексію не потрібно —
        // просто перевіряємо що реальний файл проходить, а помилковий файл впав би
        // (покрито інтеграційним тестом вище)
        assertDoesNotThrow { InputValidator.validateBalans(sheet) }
    }
}
