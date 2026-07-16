package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrendaIndexTest {

    /**
     * index кожного поля - це номер фізичної колонки у Оренда.xlsx (перевіряється в
     * InputValidator.validateOrenda). Дублікат означало б, що два поля мовчки читають
     * одну й ту саму колонку файлу.
     */
    @Test
    fun `значення index унікальні для всіх полів`() {
        val indices = OrendaIndex.entries.map { it.index }
        assertEquals(indices.size, indices.toSet().size)
    }

    @Test
    fun `значення index утворюють суцільний діапазон від 0 - 19 колонок як в InputValidator`() {
        val indices = OrendaIndex.entries.map { it.index }.sorted()
        assertEquals((0 until OrendaIndex.entries.size).toList(), indices)
    }

    @Test
    fun `index дорівнює ordinal - порядок оголошення відповідає порядку колонок`() {
        OrendaIndex.entries.forEach { field ->
            assertEquals(field.ordinal, field.index, "${field.name}: index має дорівнювати ordinal")
        }
    }

    @Test
    fun `id - перша колонка, contractFactPeriodEndDate - остання`() {
        assertEquals(0, OrendaIndex.id.index)
        assertEquals(OrendaIndex.entries.size - 1, OrendaIndex.contractFactPeriodEndDate.index)
    }
}
