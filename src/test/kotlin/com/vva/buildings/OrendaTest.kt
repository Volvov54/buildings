package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrendaTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Рядок з даними для файлу Оренда.xlsx, індексований за OrendaIndex. За замовчуванням - валідний активний договір, etcCode порожній (гілка tabList). */
    private fun orendaRow(vararg overrides: Pair<OrendaIndex, Any?>): List<Any?> {
        val row = MutableList<Any?>(OrendaIndex.entries.maxOf { it.index } + 1) { null }
        row[OrendaIndex.id.index] = 1.0
        row[OrendaIndex.idBuilding.index] = "100"
        row[OrendaIndex.quantity.index] = 45.5
        row[OrendaIndex.valueAmount.index] = 500.75
        row[OrendaIndex.valuationDate.index] = "2021-01-01"
        row[OrendaIndex.contractNumber.index] = "K-1"
        row[OrendaIndex.contractDateSigned.index] = "2021-01-02"
        row[OrendaIndex.contractStatus.index] = "Договір діє"
        row[OrendaIndex.contractCustodianName.index] = "КП Житлобуд"
        row[OrendaIndex.contractCustodianId.index] = "12345678"
        row[OrendaIndex.contractPeriodStartDate.index] = "2021-02-01"
        row[OrendaIndex.contractPeriodEndDate.index] = "2022-02-01"
        row[OrendaIndex.contractValueAmount.index] = 250.25
        row[OrendaIndex.contractUserName.index] = "ФОП Іванов"
        row[OrendaIndex.contractUserId.index] = "1234567890"
        row[OrendaIndex.addressLocatorDesignator.index] = "15"
        row[OrendaIndex.validityDate.index] = "2021-06-01"
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
    fun `createOrendaTabs - ігнорує перші два рядки як заголовок`() {
        val sheet = sheetWithRows(0 to orendaRow(), 1 to orendaRow())
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)

        assertTrue(result.list.isEmpty())
        assertTrue(result.prozorro.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з нечисловим id`() {
        val sheet = sheetWithRows(2 to orendaRow(OrendaIndex.id to "не число"))
        val result = Orenda.createOrendaTabs(tabBuildings, sheet)
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun `createOrendaTabs - пропускає рядок з фізично відсутньою клітинкою id`() {
        // На відміну від orendaRow(), тут клітинка id взагалі не створюється.
        val sheet = workbook.createSheet()
        val row = sheet.createRow(2)
        row.createCell(OrendaIndex.idBuilding.index).setCellValue("100")
        // OrendaIndex.id.index (0) навмисно не створюється

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
        // is634m перевіряє групу призначення для кожного ID зі списку; відсутній у tabBuildings ID
        // просто не враховується (не приймається за 634), а не спричиняє помилку.
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
