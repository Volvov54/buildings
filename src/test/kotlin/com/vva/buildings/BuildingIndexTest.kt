package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildingIndexTest {

    /**
     * Balans.getTabBalans() будує buildingData послідовними викликами add() (позиція = порядок
     * оголошення в enum), тоді як FreeSpace/Orenda читають поля через building[BuildingIndex.X.index].
     * Якщо index колись розійдеться з ordinal (typo, вставлене чи переставлене значення),
     * читання й запис почнуть мовчки вказувати на різні поля.
     */
    @Test
    fun `index кожного елемента дорівнює його ordinal`() {
        BuildingIndex.entries.forEach { field ->
            assertEquals(field.ordinal, field.index, "${field.name}: index має дорівнювати ordinal")
        }
    }

    @Test
    fun `значення index унікальні для всіх елементів`() {
        val indices = BuildingIndex.entries.map { it.index }
        assertEquals(indices.size, indices.toSet().size)
    }

    @Test
    fun `значення index утворюють суцільний діапазон від 0`() {
        val indices = BuildingIndex.entries.map { it.index }.sorted()
        assertEquals((0 until BuildingIndex.entries.size).toList(), indices)
    }

    @Test
    fun `id є першим полем, destinationGroup - останнім`() {
        assertEquals(0, BuildingIndex.id.index)
        assertEquals(BuildingIndex.entries.size - 1, BuildingIndex.destinationGroup.index)
    }
}
