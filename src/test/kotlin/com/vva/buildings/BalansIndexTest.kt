package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BalansIndexTest {

    /**
     * index кожного поля - це номер фізичної колонки у Баланс.xlsx (перевіряється в
     * InputValidator.validateBalans). Дублікат означало б, що два поля мовчки читають
     * одну й ту саму колонку файлу.
     */
    @Test
    fun `значення index унікальні для всіх полів`() {
        val indices = BalansIndex.entries.map { it.index }
        assertEquals(indices.size, indices.toSet().size)
    }

    @Test
    fun `значення index утворюють суцільний діапазон від 0 - 19 колонок як в InputValidator`() {
        val indices = BalansIndex.entries.map { it.index }.sorted()
        assertEquals((0 until BalansIndex.entries.size).toList(), indices)
    }

    @Test
    fun `index дорівнює ordinal - порядок оголошення відповідає порядку колонок`() {
        BalansIndex.entries.forEach { field ->
            assertEquals(field.ordinal, field.index, "${field.name}: index має дорівнювати ordinal")
        }
    }

    @Test
    fun `id - перша колонка, fieldOfActivity - остання`() {
        assertEquals(0, BalansIndex.id.index)
        assertEquals(BalansIndex.entries.size - 1, BalansIndex.fieldOfActivity.index)
    }
}
