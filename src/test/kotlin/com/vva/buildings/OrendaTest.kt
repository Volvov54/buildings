package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrendaTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Рядок з даними для Оренда.xlsx у порядку оголошення OrendaIndex. Валідний активний договір, гілка tabList. */
    private fun orendaRow(vararg overrides: Pair<OrendaIndex, Any?>): List<Any?> {
        val row = MutableList<Any?>(OrendaIndex.entries.size) { null }
        val defaults = mapOf(
            OrendaIndex.id to 1.0,
            OrendaIndex.idBuilding to "100",
            OrendaIndex.quantity to 45.5,
            OrendaIndex.valueAmount to 500.75,
            OrendaIndex.valuationDate to "2021-01-01",
            OrendaIndex.contractNumber to "K-1",
            OrendaIndex.contractDateSigned to "2021-01-02",
            OrendaIndex.contractStatus to "Договір діє",
            OrendaIndex.contractCustodianName to "КП Житлобуд",
            OrendaIndex.contractCustodianId to "12345678",
            OrendaIndex.contractPeriodStartDate to "2021-02-01",
            OrendaIndex.contractPeriodEndDate to "2022-02-01",
            OrendaIndex.contractValueAmount to 250.25,
            OrendaIndex.contractUserName to "ФОП Іванов",
            OrendaIndex.contractUserId to "1234567890",
            OrendaIndex.addressLocatorDesignator to "15",
            OrendaIndex.validityDate to "2021-06-01",
        )
        defaults.forEach { (field, value) -> row[field.ordinal] = value }
        overrides.forEach { (field, value) -> row[field.ordinal] = value }
        return row
    }

    /** Записує рядок заголовків Оренда.xlsx (індекс 1), як його очікує ColumnResolver. */
    private fun XSSFSheet.writeOrendaHeader() {
        val header = createRow(1)
        OrendaIndex.entries.forEach { header.createCell(it.ordinal).setCellValue(it.header) }
    }

    private fun sheetWithRows(vararg rows: Pair<Int, List<Any?>>): XSSFSheet {
        val sheet = workbook.createSheet()
        if (rows.none { it.first == 1 }) sheet.writeOrendaHeader()
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

    private fun defaultTabBuildings(destinationGroup: String = "100") = mapOf(
        "100" to buildingRow(
            BuildingIndex.title to "\"Будівля 100\"",
            BuildingIndex.description to "\"Офіс\"",
            BuildingIndex.CATUTTC to "UA8000000000",
            BuildingIndex.addressPostCode to "\"01001\"",
            BuildingIndex.addressAdminUnitL1 to "Україна",
            BuildingIndex.addressAdminUnitL2 to "м. Київ",
            BuildingIndex.addressAdminUnitL3 to "null",
            BuildingIndex.addressAdminUnitL4 to "null",
            BuildingIndex.addressPostName to "Київ",
            BuildingIndex.addressPostDistrict to "\"Печерський\"",
            BuildingIndex.addressPostStreet to "\"Хрещатик\"",
            BuildingIndex.addressLocatorBuilding to "null",
            BuildingIndex.addressLocatorName to "null",
            BuildingIndex.unitName to "кв. м.",
            BuildingIndex.destinationGroup to destinationGroup,
        )
    )

    private val tabBuildings = defaultTabBuildings()

    // --- фільтрація рядків ---

    @Test
    fun `createOrendaTabs - ігнорує декоративний рядок 0 та рядок заголовків`() {
        val sheet = sheetWithRows(0 to orendaRow())
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertTrue(result.list.isEmpty())
        assertTrue(result.prozorro.isEmpty())
    }

    @Test
    fun `createOrendaTabs - працює незалежно від порядку колонок у файлі`() {
        val last = OrendaIndex.entries.size - 1
        val sheet = workbook.createSheet()
        val header = sheet.createRow(1)
        val data = sheet.createRow(2)
        val values = orendaRow()
        OrendaIndex.entries.forEach { field ->
            val destCol = last - field.ordinal
            header.createCell(destCol).setCellValue(field.header)
            val cell = data.createCell(destCol)
            when (val v = values[field.ordinal]) {
                is Double -> cell.setCellValue(v)
                is String -> cell.setCellValue(v)
                else -> {} // залишаємо BLANK
            }
        }
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertEquals("1-100", result.list.single()[Orenda.headerList.indexOf("id")])
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з нечисловим id`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.id to "не число"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з фізично відсутньою клітинкою id`() {
        val sheet = workbook.createSheet()
        sheet.writeOrendaHeader()
        val row = sheet.createRow(2)
        row.createCell(OrendaIndex.idBuilding.ordinal).setCellValue("100")
        // OrendaIndex.id (колонка 0) навмисно не створюється

        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з порожньою датою актуальності`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.validityDate to null))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з датою актуальності раніше 2020-01-01`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.validityDate to "2019-12-31"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - включає рядок з датою актуальності рівно 2020-01-01`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.validityDate to "2020-01-01"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertEquals(1, result.list.size)
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з неактивним договором`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.contractStatus to "Договір розірвано"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - обрізає пробіли навколо статусу договору перед порівнянням`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.contractStatus to "  Договір діє  "))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertEquals(1, result.list.size)
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з порожнім ID обєкту`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.idBuilding to null))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок якщо будівля не знайдена`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.idBuilding to "999"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з групою призначення 634`() {
        val sheet = sheetWithRows(2 to orendaRow())
        val result = Orenda.createOrendaTabs(defaultTabBuildings(destinationGroup = "634"), sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - бере перший ID обєкту з переліку через крапку з комою`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.idBuilding to "100;200"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertEquals(1, result.list.size)
        assertEquals("1-100", result.list.single()[Orenda.headerList.indexOf("id")])
    }

    @Test
    fun `createOrendaTabs - друга будівля з переліку відсутня в tabBuildings не заважає обробці`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.idBuilding to "100;999"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertEquals(1, result.list.size)
        assertEquals("1-100", result.list.single()[Orenda.headerList.indexOf("id")])
    }

    @Test
    fun `createOrendaTabs - обрізає пробіли навколо ID обєктів у переліку`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.idBuilding to "  100 ; 200 "))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertEquals(1, result.list.size)
        assertEquals("1-100", result.list.single()[Orenda.headerList.indexOf("id")])
    }

    @Test
    fun `createOrendaTabs - обробляє кілька рядків незалежно одне від одного`() {
        val sheet = sheetWithRows(
            2 to orendaRow(OrendaIndex.id to 1.0),
            3 to orendaRow(OrendaIndex.id to 2.0, OrendaIndex.contractStatus to "Договір розірвано"), // виключений
            4 to orendaRow(OrendaIndex.id to 3.0),
        )
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertEquals(2, result.list.size)
        val ids = result.list.map { it[Orenda.headerList.indexOf("id")] }
        assertEquals(listOf("1-100", "3-100"), ids)
    }

    @Test
    fun `createOrendaTabs - кидає виняток якщо у файлі бракує обов'язкової колонки`() {
        val sheet = workbook.createSheet()
        val header = sheet.createRow(1)
        OrendaIndex.entries
            .filter { it != OrendaIndex.contractStatus }
            .forEach { header.createCell(it.ordinal).setCellValue(it.header) }

        assertThrows(IllegalStateException::class.java) { Orenda.createOrendaTabs(tabBuildings, sheet) }
    }

    // --- гілка Prozorro (etcCode рядкового типу) ---

    @Test
    fun `createOrendaTabs - рядок з рядковим etcCode потрапляє в prozorro`() {
        val sheet = sheetWithRows(
            2 to orendaRow(OrendaIndex.etcCode to "https://prozorro.sale/auction/UA-1234-567")
        )
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertTrue(result.list.isEmpty())
        assertEquals(
            listOf("UA-1234-567", "\"Будівля 100\"", "https://prozorro.sale/auction/UA-1234-567"),
            result.prozorro.single()
        )
    }

    // --- гілка tabList (etcCode порожній) ---

    @Test
    fun `createOrendaTabs - формує повний рядок оренди з коректними полями`() {
        val sheet = sheetWithRows(2 to orendaRow())
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertTrue(result.prozorro.isEmpty())
        val data = result.list.single()
        val header = Orenda.headerList

        assertEquals("1-100", data[header.indexOf("id")])
        assertEquals("\"Будівля 100\"", data[header.indexOf("title")])
        assertEquals("\"Офіс\"", data[header.indexOf("description")])
        assertEquals("\"15\"", data[header.indexOf("addressLocatorDesignator")])
        assertEquals("45.5", data[header.indexOf("quantity")])
        assertEquals("500.75", data[header.indexOf("valueAmount")])
        assertEquals("UAH", data[header.indexOf("currencyCode")])
        assertEquals("2021-01-01", data[header.indexOf("valuationDate")])
        assertEquals("\"K-1\"", data[header.indexOf("contractNumber")])
        assertEquals("2021-01-02", data[header.indexOf("contractDateSigned")])
        assertEquals("null", data[header.indexOf("contractUrl")])
        assertEquals("\"Договір діє\"", data[header.indexOf("contractStatus")])
        assertEquals("null", data[header.indexOf("contractPurpose")])
        assertEquals("null", data[header.indexOf("contractRentalRate")])
        assertEquals("\"КП Житлобуд\"", data[header.indexOf("contractCustodianName")])
        assertEquals("\"12345678\"", data[header.indexOf("contractCustodianId")])
        assertEquals("\"ФОП Іванов\"", data[header.indexOf("contractUserName")])
        assertEquals("2021-02-01", data[header.indexOf("contractPeriodStartDate")])
        assertEquals("2022-02-01", data[header.indexOf("contractPeriodEndDate")])
        assertEquals("null", data[header.indexOf("contractPeriodMaxExtentDate")])
        assertEquals("null", data[header.indexOf("contractSchedule")])
        assertEquals("Місяць", data[header.indexOf("contractValuePeriod")])
        assertEquals("250.25", data[header.indexOf("contractValueAmount")])
        assertEquals("null", data[header.indexOf("contractValueDescription")])
        assertEquals("2021-06-01", data[header.indexOf("validityDate")])
    }

    @Test
    fun `createOrendaTabs - маскує 10-значний код орендаря`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.contractUserId to "1234567890"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        val data = result.list.single()

        assertEquals("\"XXXXXXXXXX\"", data[Orenda.headerList.indexOf("contractUserId")])
    }

    @Test
    fun `createOrendaTabs - не маскує код орендаря юрособи`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.contractUserId to "12345678"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        val data = result.list.single()

        assertEquals("\"12345678\"", data[Orenda.headerList.indexOf("contractUserId")])
    }

    // --- getListCsv / getListProzorroSalesCsv ---

    @Test
    fun `getListCsv - перший рядок містить заголовок`() {
        val csv = Orenda.getListCsv(OrendaData(list = emptyList(), prozorro = emptyList()))
        assertEquals(Orenda.headerList.joinToString(","), csv.lines().first())
    }

    @Test
    fun `getListProzorroSalesCsv - формує заголовок та рядки`() {
        val data = OrendaData(list = emptyList(), prozorro = listOf(listOf("UA-1", "\"Т\"", "url")))
        val csv = Orenda.getListProzorroSalesCsv(data)

        assertEquals("ocid,title,url\nUA-1,\"Т\",url\n", csv)
    }
}
