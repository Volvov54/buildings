package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreeSpaceIndexTest {

    /**
     * `header` кожного поля - назва колонки у ВільніПлощі.xlsx. Для колонок площі та комунікацій
     * це саме під-заголовок (рядок 3), а не над-заголовок (рядок 2), бо ColumnResolver сплющує
     * два рядки заголовка з перевагою нижнього.
     */
    @Test
    fun `заголовки унікальні для всіх полів`() {
        val headers = FreeSpaceIndex.entries.map { it.header }
        assertEquals(headers.size, headers.toSet().size)
    }

    @Test
    fun `жоден заголовок не порожній`() {
        assertTrue(FreeSpaceIndex.entries.all { it.header.isNotBlank() })
    }

    @Test
    fun `колонки комунікацій закріплені на під-заголовках`() {
        assertEquals("Водопостачання", FreeSpaceIndex.utilitiesAvailableWaterSupply.header)
        assertEquals("Теплопостачання", FreeSpaceIndex.utilitiesAvailableHeatingSupply.header)
        assertEquals("Потужність електромережі", FreeSpaceIndex.utilitiesAvailableElectricNetwork.header)
        assertEquals("Газопостачання", FreeSpaceIndex.utilitiesAvailableGasSupply.header)
    }
}
