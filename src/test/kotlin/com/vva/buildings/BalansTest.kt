package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BalansTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Рядок з коректними даними для файлу Баланс.xlsx, індексований за BalansIndex. */
    private fun balansRow(vararg overrides: Pair<BalansIndex, Any?>): List<Any?> {
        val row = mutableListOf<Any?>(
            12345.0,        // id
            "Будівля 1",     // title
            "Нежитлова",     // kind
            "Тип 1",         // type
            "Офіс",          // description
            "КП Житлобуд",   // balanceHolderName
            "12345678",      // balanceHolderId
            "111",           // dk018classId
            "Будівлі",       // dk018classDescription
            100.5,           // area
            "01001",         // addressPostCode
            "Печерський",    // addressPostDistrict
            "Хрещатик",      // addressThoroughfare
            "REG-1",         // registrationId
            "Задовільний",   // condition
            "2021-01-01",    // validityDate
            "100",           // destinationGroup
            "5",             // addressLocatorDesignator
            "Управління",    // fieldOfActivity
        )
        overrides.forEach { (field, value) -> row[field.index] = value }
        return row
    }

    /** Створює аркуш, де рядки з даними розміщені за вказаними номерами (rowNum). */
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

    // --- getTabBalans ---

    @Test
    fun `getTabBalans - додає коректний рядок у мапу за id`() {
        val sheet = sheetWithRows(2 to balansRow())
        val result = Balans.getTabBalans(sheet)

        assertEquals(setOf("12345"), result.keys)
        val building = result.getValue("12345")
        assertEquals("12345", building[BuildingIndex.id.index])
        assertEquals("\"Будівля 1\"", building[BuildingIndex.title.index])
        assertEquals("Київська міська рада", building[BuildingIndex.ownerName.index])
        assertEquals("22883141", building[BuildingIndex.ownerId.index])
        assertEquals("\"12345678\"", building[BuildingIndex.balanceHolderId.index])
        assertEquals("кв. м.", building[BuildingIndex.unitName.index])
    }

    @Test
    fun `getTabBalans - для київського району заповнює адресні поля Києва`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.addressPostDistrict to "Печерський"))
        val building = Balans.getTabBalans(sheet).getValue("12345")

        assertEquals("UA80000000000093317", building[BuildingIndex.CATUTTC.index])
        assertEquals("м. Київ", building[BuildingIndex.addressAdminUnitL2.index])
        assertEquals("null", building[BuildingIndex.addressAdminUnitL4.index])
        assertEquals("Київ", building[BuildingIndex.addressPostName.index])
        assertEquals("\"Печерський\"", building[BuildingIndex.addressPostDistrict.index])
    }

    @Test
    fun `getTabBalans - для не київського району заповнює district замість Києва`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.addressPostDistrict to "Бориспільський"))
        val building = Balans.getTabBalans(sheet).getValue("12345")

        assertEquals("null", building[BuildingIndex.CATUTTC.index])
        assertEquals("null", building[BuildingIndex.addressAdminUnitL2.index])
        assertEquals("\"Бориспільський\"", building[BuildingIndex.addressAdminUnitL4.index])
        assertEquals("null", building[BuildingIndex.addressPostName.index])
        assertEquals("null", building[BuildingIndex.addressPostDistrict.index])
    }

    @Test
    fun `getTabBalans - виключає балансоутримувача 22991617`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.balanceHolderId to "22991617"))
        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - виключає сферу діяльності Невизначені`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.fieldOfActivity to "Невизначені"))
        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - пропускає рядок з нечисловим id`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.id to "не число"))
        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - пропускає рядок з порожнім id`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.id to null))
        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - ігнорує перші два рядки як заголовок`() {
        val sheet = sheetWithRows(0 to balansRow(), 1 to balansRow())
        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - registrationStatus Невідомо при порожньому registrationId`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.registrationId to null))
        val building = Balans.getTabBalans(sheet).getValue("12345")
        assertEquals("Невідомо", building[BuildingIndex.registrationStatus.index])
    }

    @Test
    fun `getTabBalans - registrationStatus Зареєстровано при заповненому registrationId`() {
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.registrationId to "REG-1"))
        val building = Balans.getTabBalans(sheet).getValue("12345")
        assertEquals("Зареєстровано", building[BuildingIndex.registrationStatus.index])
    }

    // --- getBalansCsv ---

    private fun fullBuildingRow(vararg overrides: Pair<BuildingIndex, String>): List<String> {
        val row = MutableList(BuildingIndex.entries.size) { "" }
        overrides.forEach { (field, value) -> row[field.index] = value }
        return row
    }

    @Test
    fun `getBalansCsv - виключає будівлі з групою призначення 634`() {
        val tab = mapOf(
            "1" to fullBuildingRow(BuildingIndex.id to "1", BuildingIndex.destinationGroup to "634"),
            "2" to fullBuildingRow(BuildingIndex.id to "2", BuildingIndex.destinationGroup to "100"),
        )
        val csv = Balans.getBalansCsv(tab)

        assertFalse(csv.contains("\n1,"))
        assertTrue(csv.contains("\n2,"))
    }

    @Test
    fun `getBalansCsv - не включає колонку destinationGroup у вивід`() {
        val tab = mapOf(
            "1" to fullBuildingRow(BuildingIndex.id to "1", BuildingIndex.destinationGroup to "100"),
        )
        val csv = Balans.getBalansCsv(tab)
        val dataLine = csv.lines()[1]

        assertEquals(Balans.header.size, dataLine.split(",").size)
    }

    @Test
    fun `getBalansCsv - перший рядок містить заголовок`() {
        val csv = Balans.getBalansCsv(emptyMap())
        assertEquals(Balans.header.joinToString(","), csv.lines().first())
    }
}
