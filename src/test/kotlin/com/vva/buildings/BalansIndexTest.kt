package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BalansIndexTest {

    /**
     * `header` кожного поля - назва колонки у Баланс.xlsx, за якою ColumnResolver знаходить
     * фізичний номер колонки. Дублікат означав би, що два поля мовчки читають одну колонку файлу.
     */
    @Test
    fun `заголовки унікальні для всіх полів`() {
        val headers = BalansIndex.entries.map { it.header }
        assertEquals(headers.size, headers.toSet().size)
    }

    @Test
    fun `жоден заголовок не порожній`() {
        assertTrue(BalansIndex.entries.all { it.header.isNotBlank() })
    }

    @Test
    fun `заголовки не мають зайвих пробілів по краях`() {
        assertTrue(BalansIndex.entries.all { it.header == it.header.trim() })
    }
}
