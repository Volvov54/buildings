package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrendaIndexTest {

    /**
     * `header` кожного поля - назва колонки у Оренда.xlsx, за якою ColumnResolver знаходить
     * фізичний номер колонки. Дублікат означав би, що два поля мовчки читають одну колонку файлу.
     */
    @Test
    fun `заголовки унікальні для всіх полів`() {
        val headers = OrendaIndex.entries.map { it.header }
        assertEquals(headers.size, headers.toSet().size)
    }

    @Test
    fun `жоден заголовок не порожній`() {
        assertTrue(OrendaIndex.entries.all { it.header.isNotBlank() })
    }

    @Test
    fun `idBuilding використовує апостроф U+2019 як у файлі`() {
        assertEquals("ID об’єктів за договором", OrendaIndex.idBuilding.header)
    }
}
