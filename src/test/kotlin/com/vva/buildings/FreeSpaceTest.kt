package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreeSpaceTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Рядок з даними для файлу ВільніПлощі.xlsx, індексований за FreeSpaceIndex. За замовчуванням etcCode порожній (гілка tabBuildings2). */
    private fun freeSpaceRow(vararg overrides: Pair<FreeSpaceIndex, Any?>): List<Any?> {
        val row = MutableList<Any?>(FreeSpaceIndex.entries.maxOf { it.index } + 1) { null }
        row[FreeSpaceIndex.idSpace.index] = 1.0
        row[FreeSpaceIndex.buildingId.index] = 100.0
        row[FreeSpaceIndex.area.index] = 25.5
        row[FreeSpaceIndex.utilitiesAvailableWaterSupply.index] = "Так"
        row[FreeSpaceIndex.utilitiesAvailableHeatingSupply.index] = "Так"
        row[FreeSpaceIndex.utilitiesAvailableElectricNetwork.index] = "Мережа 380В"
        row[FreeSpaceIndex.utilitiesAvailableGasSupply.index] = "Ні"
        row[FreeSpaceIndex.addressLocatorDesignator.index] = "10А"
        overrides.forEach { (field, value) -> row[field.index] = value }
        return row
    }

    private fun sheetWithRows(vararg rows: Pair<Int, List<Any?>>): XSSFSheet {
        val sheet = workbook.createSheet()
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
    fun `createFreeSpaceTabs - ігнорує перші три рядки як заголовок`() {
        val sheet = sheetWithRows(0 to freeSpaceRow(), 1 to freeSpaceRow(), 2 to freeSpaceRow())
        val result = FreeSpace.createFreeSpaceTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        assertTrue(result.buildings.isEmpty())
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
