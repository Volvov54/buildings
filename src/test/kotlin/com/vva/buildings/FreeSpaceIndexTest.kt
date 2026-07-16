package com.vva.buildings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreeSpaceIndexTest {

    /**
     * На відміну від BalansIndex/BuildingIndex, тут index НЕ дорівнює ordinal: колонки 3, 5, 6
     * у ВільніПлощі.xlsx навмисно пропущені (невикористовувані/злиті клітинки, див. CLAUDE.md).
     * Тому кожне значення закріплюємо явно - помилковий зсув номера колонки має зловитись тут.
     */
    @Test
    fun `index кожного поля відповідає задокументованій колонці ВільніПлощі xlsx`() {
        assertEquals(0, FreeSpaceIndex.idSpace.index)
        assertEquals(1, FreeSpaceIndex.buildingId.index)
        assertEquals(2, FreeSpaceIndex.etcCode.index)
        assertEquals(4, FreeSpaceIndex.area.index)
        assertEquals(7, FreeSpaceIndex.utilitiesAvailableWaterSupply.index)
        assertEquals(8, FreeSpaceIndex.utilitiesAvailableHeatingSupply.index)
        assertEquals(9, FreeSpaceIndex.utilitiesAvailableElectricNetwork.index)
        assertEquals(10, FreeSpaceIndex.utilitiesAvailableGasSupply.index)
        assertEquals(11, FreeSpaceIndex.addressLocatorDesignator.index)
    }

    @Test
    fun `значення index унікальні для всіх полів`() {
        val indices = FreeSpaceIndex.entries.map { it.index }
        assertEquals(indices.size, indices.toSet().size)
    }

    @Test
    fun `значення index зростають у порядку оголошення полів`() {
        val indices = FreeSpaceIndex.entries.map { it.index }
        assertTrue(indices.zipWithNext().all { (a, b) -> a < b }, "index мають монотонно зростати: $indices")
    }
}
