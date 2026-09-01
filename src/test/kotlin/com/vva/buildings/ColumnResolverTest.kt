package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColumnResolverTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() = workbook.close()

    /** Створює аркуш і розкладає заголовки: (номер рядка, номер колонки) -> текст. */
    private fun sheetWithHeaders(vararg cells: Triple<Int, Int, String>): XSSFSheet {
        val sheet = workbook.createSheet()
        cells.forEach { (rowIndex, colIndex, text) ->
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            row.createCell(colIndex).setCellValue(text)
        }
        return sheet
    }

    /** Заголовки Balans на рядку 1, кожне поле у своїй колонці за заданим відображенням. */
    private fun balansSheet(columnOf: (BalansIndex) -> Int): XSSFSheet =
        sheetWithHeaders(*BalansIndex.entries.map { Triple(1, columnOf(it), it.header) }.toTypedArray())

    // --- Balans: незалежність від порядку колонок ---

    @Test
    fun `resolveBalans - канонічний порядок колонок`() {
        val map = ColumnResolver.resolveBalans(balansSheet { it.ordinal })

        assertEquals(BalansIndex.entries.size, map.size)
        BalansIndex.entries.forEach { assertEquals(it.ordinal, map.getValue(it), it.name) }
    }

    @Test
    fun `resolveBalans - зворотний порядок колонок`() {
        val last = BalansIndex.entries.size - 1
        val map = ColumnResolver.resolveBalans(balansSheet { last - it.ordinal })

        BalansIndex.entries.forEach { assertEquals(last - it.ordinal, map.getValue(it), it.name) }
    }

    @Test
    fun `resolveBalans - зайві невідомі колонки між очікуваними ігноруються`() {
        val sheet = balansSheet { it.ordinal * 2 } // очікувані - на парних колонках
        val header = sheet.getRow(1)
        for (odd in 1..BalansIndex.entries.size * 2 step 2) {
            header.createCell(odd).setCellValue("Службова колонка $odd")
        }
        val map = ColumnResolver.resolveBalans(sheet)

        BalansIndex.entries.forEach { assertEquals(it.ordinal * 2, map.getValue(it), it.name) }
    }

    // --- Balans: перевірка повноти ---

    @Test
    fun `resolveBalans - відсутня обов'язкова колонка кидає виняток з її назвою`() {
        val sheet = balansSheet { it.ordinal }
        // прибираємо заголовок area
        sheet.getRow(1).getCell(BalansIndex.area.ordinal).setCellValue("")
        val ex = assertThrows(IllegalStateException::class.java) { ColumnResolver.resolveBalans(sheet) }

        assertTrue(ex.message!!.contains(BalansIndex.area.header))
        assertTrue(ex.message!!.contains("не знайдено"))
    }

    @Test
    fun `resolveBalans - порожній аркуш без заголовків кидає виняток`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            ColumnResolver.resolveBalans(workbook.createSheet())
        }
        assertTrue(ex.message!!.contains("рядок заголовків"))
    }

    // --- Orenda: суворе зіставлення апострофа ---

    @Test
    fun `resolveOrenda - коректні заголовки розв'язуються`() {
        val map = ColumnResolver.resolveOrenda(
            sheetWithHeaders(*OrendaIndex.entries.map { Triple(1, it.ordinal, it.header) }.toTypedArray())
        )
        assertEquals(OrendaIndex.entries.size, map.size)
    }

    @Test
    fun `resolveOrenda - звичайний апостроф U+0027 замість U+2019 у idBuilding кидає виняток`() {
        val cells = OrendaIndex.entries.map { field ->
            val text = if (field == OrendaIndex.idBuilding) "ID об'єктів за договором" else field.header
            Triple(1, field.ordinal, text)
        }
        val ex = assertThrows(IllegalStateException::class.java) {
            ColumnResolver.resolveOrenda(sheetWithHeaders(*cells.toTypedArray()))
        }
        assertTrue(ex.message!!.contains(OrendaIndex.idBuilding.header))
    }

    // --- FreeSpace: дворядковий заголовок ---

    @Test
    fun `resolveFreeSpace - під-заголовок рядка 3 перемагає над-заголовок рядка 2`() {
        val sheet = sheetWithHeaders(
            Triple(1, 0, FreeSpaceIndex.idSpace.header),
            Triple(1, 1, FreeSpaceIndex.buildingId.header),
            Triple(1, 2, FreeSpaceIndex.etcCode.header),
            Triple(1, 3, "Вільні приміщення"), // над-заголовок
            Triple(2, 3, FreeSpaceIndex.area.header), // під-заголовок тієї ж колонки
            Triple(1, 4, "Наявність комунікацій"), // над-заголовок
            Triple(2, 4, FreeSpaceIndex.utilitiesAvailableWaterSupply.header),
            Triple(2, 5, FreeSpaceIndex.utilitiesAvailableHeatingSupply.header),
            Triple(2, 6, FreeSpaceIndex.utilitiesAvailableElectricNetwork.header),
            Triple(2, 7, FreeSpaceIndex.utilitiesAvailableGasSupply.header),
            Triple(1, 8, FreeSpaceIndex.addressLocatorDesignator.header),
        )
        val map = ColumnResolver.resolveFreeSpace(sheet)

        assertEquals(0, map.getValue(FreeSpaceIndex.idSpace))
        assertEquals(3, map.getValue(FreeSpaceIndex.area))
        assertEquals(4, map.getValue(FreeSpaceIndex.utilitiesAvailableWaterSupply))
        assertEquals(7, map.getValue(FreeSpaceIndex.utilitiesAvailableGasSupply))
        assertEquals(8, map.getValue(FreeSpaceIndex.addressLocatorDesignator))
    }
}
