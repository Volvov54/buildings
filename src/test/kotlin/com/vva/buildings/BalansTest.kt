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

    /** Рядок з коректними даними для файлу Баланс.xlsx. Колонки розкладені у порядку оголошення BalansIndex. */
    private fun balansRow(vararg overrides: Pair<BalansIndex, Any?>): List<Any?> {
        val row = MutableList<Any?>(BalansIndex.entries.size) { null }
        val defaults = mapOf(
            BalansIndex.id to 12345.0,
            BalansIndex.title to "Будівля 1",
            BalansIndex.kind to "Нежитлова",
            BalansIndex.type to "Тип 1",
            BalansIndex.description to "Офіс",
            BalansIndex.balanceHolderName to "КП Житлобуд",
            BalansIndex.balanceHolderId to "12345678",
            BalansIndex.dk018classId to "111",
            BalansIndex.dk018classDescription to "Будівлі",
            BalansIndex.area to 100.5,
            BalansIndex.addressPostCode to "01001",
            BalansIndex.addressPostDistrict to "Печерський",
            BalansIndex.addressThoroughfare to "Хрещатик",
            BalansIndex.registrationId to "REG-1",
            BalansIndex.condition to "Задовільний",
            BalansIndex.validityDate to "2021-01-01",
            BalansIndex.destinationGroup to "100",
            BalansIndex.addressLocatorDesignator to "5",
            BalansIndex.fieldOfActivity to "Управління",
        )
        defaults.forEach { (field, value) -> row[field.ordinal] = value }
        overrides.forEach { (field, value) -> row[field.ordinal] = value }
        return row
    }

    /** Записує рядок заголовків Баланс.xlsx (індекс 1), як його очікує ColumnResolver. */
    private fun XSSFSheet.writeBalansHeader() {
        val header = createRow(1)
        BalansIndex.entries.forEach { header.createCell(it.ordinal).setCellValue(it.header) }
    }

    /**
     * Створює аркуш з рядком заголовків (індекс 1) і рядками даних за вказаними номерами (rowNum).
     * Заголовок додається автоматично, якщо номер 1 не заданий явно.
     */
    private fun sheetWithRows(vararg rows: Pair<Int, List<Any?>>): XSSFSheet {
        val sheet = workbook.createSheet()
        if (rows.none { it.first == 1 }) sheet.writeBalansHeader()
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
    fun `getTabBalans - працює незалежно від порядку колонок у файлі`() {
        // Заголовки та дані у зворотному порядку колонок.
        val last = BalansIndex.entries.size - 1
        val sheet = workbook.createSheet()
        val header = sheet.createRow(1)
        val data = sheet.createRow(2)
        val values = balansRow()
        BalansIndex.entries.forEach { field ->
            header.createCell(last - field.ordinal).setCellValue(field.header)
            when (val v = values[field.ordinal]) {
                is Double -> data.createCell(last - field.ordinal).setCellValue(v)
                is String -> data.createCell(last - field.ordinal).setCellValue(v)
            }
        }
        val building = Balans.getTabBalans(sheet).getValue("12345")
        assertEquals("\"Будівля 1\"", building[BuildingIndex.title.index])
        assertEquals("\"Печерський\"", building[BuildingIndex.addressPostDistrict.index])
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
    fun `getTabBalans - фільтр Невизначені чутливий до регістру`() {
        // Документує поточну поведінку: порівняння точне (==), тому інший регістр не відфільтровується.
        val sheet = sheetWithRows(2 to balansRow(BalansIndex.fieldOfActivity to "невизначені"))
        assertFalse(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - обробляє кілька рядків незалежно одне від одного`() {
        val sheet = sheetWithRows(
            2 to balansRow(BalansIndex.id to 1.0),
            3 to balansRow(BalansIndex.id to 2.0, BalansIndex.balanceHolderId to "22991617"), // виключений
            4 to balansRow(BalansIndex.id to 3.0),
        )
        val result = Balans.getTabBalans(sheet)

        assertEquals(setOf("1", "3"), result.keys)
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
    fun `getTabBalans - пропускає рядок з фізично відсутньою клітинкою id`() {
        // Клітинка id взагалі не створюється (row.getCell поверне null), а не просто BLANK.
        val sheet = workbook.createSheet()
        sheet.writeBalansHeader()
        val row = sheet.createRow(2)
        row.createCell(BalansIndex.balanceHolderId.ordinal).setCellValue("12345678")
        row.createCell(BalansIndex.fieldOfActivity.ordinal).setCellValue("Управління")
        // BalansIndex.id (колонка 0) навмисно не створюється

        assertTrue(Balans.getTabBalans(sheet).isEmpty())
    }

    @Test
    fun `getTabBalans - ігнорує декоративний рядок 0 та рядок заголовків`() {
        val sheet = sheetWithRows(0 to balansRow())
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

    @Test
    fun `getTabBalans - кидає виняток якщо у файлі бракує обов'язкової колонки`() {
        val sheet = workbook.createSheet()
        val header = sheet.createRow(1)
        BalansIndex.entries
            .filter { it != BalansIndex.area }
            .forEach { header.createCell(it.ordinal).setCellValue(it.header) }

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            Balans.getTabBalans(sheet)
        }
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
