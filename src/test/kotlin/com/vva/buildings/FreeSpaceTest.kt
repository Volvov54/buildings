package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreeSpaceTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Рядок з даними для ВільніПлощі.xlsx у порядку оголошення FreeSpaceIndex. Гілка tabBuildings2 (etcCode порожній). */
    private fun freeSpaceRow(vararg overrides: Pair<FreeSpaceIndex, Any?>): List<Any?> {
        val row = MutableList<Any?>(FreeSpaceIndex.entries.size) { null }
        val defaults = mapOf(
            FreeSpaceIndex.idSpace to 1.0,
            FreeSpaceIndex.buildingId to 100.0,
            FreeSpaceIndex.area to 25.5,
            FreeSpaceIndex.utilitiesAvailableWaterSupply to "Так",
            FreeSpaceIndex.utilitiesAvailableHeatingSupply to "Так",
            FreeSpaceIndex.utilitiesAvailableElectricNetwork to "Мережа 380В",
            FreeSpaceIndex.utilitiesAvailableGasSupply to "Ні",
            FreeSpaceIndex.addressLocatorDesignator to "10А",
        )
        defaults.forEach { (field, value) -> row[field.ordinal] = value }
        overrides.forEach { (field, value) -> row[field.ordinal] = value }
        return row
    }

    /** Записує рядок заголовків ВільніПлощі.xlsx (індекс 2), як його очікує ColumnResolver. */
    private fun XSSFSheet.writeFreeSpaceHeader() {
        val header = createRow(2)
        FreeSpaceIndex.entries.forEach { header.createCell(it.ordinal).setCellValue(it.header) }
    }

    private fun sheetWithRows(vararg rows: Pair<Int, List<Any?>>): XSSFSheet {
        val sheet = workbook.createSheet()
        if (rows.none { it.first == 2 }) sheet.writeFreeSpaceHeader()
        rows.forEach { (rowNum, data) ->
            val row = sheet.createRow(rowNum)
            data.forEachIndexed { colIndex, value ->
                val cell = row.createCell(colIndex)
                when (value) {
                    is Double -> cell.setCellValue(value)
                    is String -> cell.setCellValue(value)
                    null -> Unit // залишаємо BLANK
                }
            }
        }
        return sheet
    }

    private fun buildingRow(vararg overrides: Pair<BuildingIndex, String>): List<String> {
        val row = MutableList(BuildingIndex.entries.size) { "" }
        overrides.forEach { (field, value) -> row[field.index] = value }
        return row
    }

    private val tabBuildings = mapOf(
        "100" to buildingRow(
            BuildingIndex.title to "\"Будівля 100\"",
            BuildingIndex.isPartOf to "null",
            BuildingIndex.kind to "\"Нежитлова\"",
            BuildingIndex.ownerName to "Київська міська рада",
        )
    )

    // --- createFreeSpaceTabs ---

    @Test
    fun `createFreeSpaceTabs - ігнорує рядки 0-1 та рядок заголовків`() {
        val sheet = sheetWithRows(0 to freeSpaceRow(), 1 to freeSpaceRow())
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
    }

    @Test
    fun `createFreeSpaceTabs - працює незалежно від порядку колонок у файлі`() {
        val last = FreeSpaceIndex.entries.size - 1
        val sheet = workbook.createSheet()
        val header = sheet.createRow(2)
        val data = sheet.createRow(3)
        val values = freeSpaceRow()
        FreeSpaceIndex.entries.forEach { field ->
            val destCol = last - field.ordinal
            header.createCell(destCol).setCellValue(field.header)
            val cell = data.createCell(destCol)
            when (val v = values[field.ordinal]) {
                is Double -> cell.setCellValue(v)
                is String -> cell.setCellValue(v)
                else -> {} // залишаємо BLANK
            }
        }
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)
        assertEquals("1-100", result.buildings.single()[0])
    }

    @Test
    fun `createFreeSpaceTabs - пропускає рядок з нечисловим idSpace`() {
        val sheet = sheetWithRows(3 to freeSpaceRow(FreeSpaceIndex.idSpace to "не число"))
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
    }

    @Test
    fun `createFreeSpaceTabs - пропускає рядок з нечисловим buildingId замість падіння`() {
        val sheet = sheetWithRows(3 to freeSpaceRow(FreeSpaceIndex.buildingId to "не число"))
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
    }

    @Test
    fun `createFreeSpaceTabs - пропускає рядок з фізично відсутньою клітинкою idSpace`() {
        val sheet = workbook.createSheet()
        sheet.writeFreeSpaceHeader()
        val row = sheet.createRow(3)
        row.createCell(FreeSpaceIndex.buildingId.ordinal).setCellValue(100.0)
        // FreeSpaceIndex.idSpace (колонка 0) навмисно не створюється

        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
    }

    @Test
    fun `createFreeSpaceTabs - пропускає рядок якщо будівля не знайдена`() {
        val sheet = sheetWithRows(3 to freeSpaceRow(FreeSpaceIndex.buildingId to 999.0))
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
    }

    @Test
    fun `createFreeSpaceTabs - рядок з рядковим etcCode потрапляє в prozorro`() {
        val sheet = sheetWithRows(
            3 to freeSpaceRow(FreeSpaceIndex.etcCode to "https://prozorro.sale/auction/UA-1234-567")
        )
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.buildings.isEmpty())
        assertEquals(
            listOf("1", "\"Будівля 100\"", "https://prozorro.sale/auction/UA-1234-567"),
            result.prozorro.single()
        )
    }

    @Test
    fun `createFreeSpaceTabs - рядок без etcCode потрапляє в buildings з полями будівлі`() {
        val sheet = sheetWithRows(3 to freeSpaceRow())
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        val data = result.buildings.single()

        assertEquals("1-100", data[0]) // buildingId = "$idSpace-$idBuilding"
        assertEquals("null", data[1]) // isPartOf
        assertEquals("\"Будівля 100\"", data[2]) // buildingTitle
        assertEquals("\"Нежитлова\"", data[3]) // kind
        assertEquals("\"25.5\"", data[FreeSpace.headerBuildings2.indexOf("buildingArea")]) // area з рядка
        assertEquals("\"10А\"", data[FreeSpace.headerBuildings2.indexOf("addressLocatorDesignator")])
        assertEquals("true", data[FreeSpace.headerBuildings2.indexOf("utilitiesAvailableWaterSupply")])
        assertEquals("true", data[FreeSpace.headerBuildings2.indexOf("utilitiesAvailableHeatingSupply")])
        assertEquals(
            "\"Мережа 380В\"",
            data[FreeSpace.headerBuildings2.indexOf("utilitiesAvailableElectricNetwork")]
        )
        assertEquals("false", data[FreeSpace.headerBuildings2.indexOf("utilitiesAvailableGasSupply")])
    }

    @Test
    fun `createFreeSpaceTabs - обробляє кілька рядків незалежно одне від одного`() {
        val sheet = sheetWithRows(
            3 to freeSpaceRow(FreeSpaceIndex.idSpace to 1.0),
            4 to freeSpaceRow(FreeSpaceIndex.idSpace to 2.0, FreeSpaceIndex.buildingId to 999.0), // будівля не знайдена
            5 to freeSpaceRow(FreeSpaceIndex.idSpace to 3.0),
        )
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertEquals(2, result.buildings.size)
        assertEquals(listOf("1-100", "3-100"), result.buildings.map { it[0] })
    }

    @Test
    fun `createFreeSpaceTabs - порожнє значення комунікації означає false`() {
        val sheet = sheetWithRows(
            3 to freeSpaceRow(FreeSpaceIndex.utilitiesAvailableWaterSupply to null)
        )
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)
        val data = result.buildings.single()

        assertEquals("false", data[FreeSpace.headerBuildings2.indexOf("utilitiesAvailableWaterSupply")])
    }

    @Test
    fun `createFreeSpaceTabs - кидає виняток якщо у файлі бракує обов'язкової колонки`() {
        val sheet = workbook.createSheet()
        val header = sheet.createRow(2)
        FreeSpaceIndex.entries
            .filter { it != FreeSpaceIndex.area }
            .forEach { header.createCell(it.ordinal).setCellValue(it.header) }

        assertThrows(IllegalStateException::class.java) {
            FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)
        }
    }

    // --- getProzorroCsv / getBuildingsCsv ---

    @Test
    fun `getProzorroCsv - формує заголовок та рядки`() {
        val data = FreeSpaceData(prozorro = listOf(listOf("1", "\"Т\"", "url")), buildings = emptyList())
        val csv = FreeSpace.getProzorroCsv(data)

        assertEquals("ocid,title,url\n1,\"Т\",url\n", csv)
    }

    @Test
    fun `getBuildingsCsv - перший рядок містить заголовок`() {
        val csv = FreeSpace.getBuildingsCsv(FreeSpaceData(prozorro = emptyList(), buildings = emptyList()))
        assertEquals(FreeSpace.headerBuildings2.joinToString(","), csv.lines().first())
    }
}
