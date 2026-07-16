package com.vva.buildings

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class UtilsTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    private fun cell(setup: Cell.() -> Unit = {}): Cell {
        val sheet = workbook.createSheet()
        val row = sheet.createRow(0)
        val c = row.createCell(0)
        c.setup()
        return c
    }

    private fun blankCell(): Cell = cell()

    private fun stringCell(value: String): Cell = cell { setCellValue(value) }

    private fun numericCell(value: Double): Cell = cell { setCellValue(value) }

    private fun dateCell(year: Int, month: Int, day: Int): Cell = cell {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        setCellValue(calendar)
    }

    private fun buildingRow(vararg overrides: Pair<BuildingIndex, String>): List<String> {
        val row = MutableList(BuildingIndex.entries.size) { "" }
        overrides.forEach { (field, value) -> row[field.index] = value }
        return row
    }

    // --- setQuotation / getQuotationString ---

    @Test
    fun `setQuotation - блок повертає null`() {
        assertEquals("null", Utils.setQuotation(blankCell()))
    }

    @Test
    fun `setQuotation - рядок обгортається в лапки`() {
        assertEquals("\"текст\"", Utils.setQuotation(stringCell("текст")))
    }

    @Test
    fun `setQuotation - внутрішні лапки екрануються подвоєнням`() {
        assertEquals("\"аб\"\"вг\"", Utils.setQuotation(stringCell("аб\"вг")))
    }

    @Test
    fun `getQuotationString - null повертає null-рядок`() {
        assertEquals("null", Utils.getQuotationString(null))
    }

    @Test
    fun `getQuotationString - порожній рядок повертає null-рядок`() {
        assertEquals("null", Utils.getQuotationString("   "))
    }

    @Test
    fun `getQuotationString - звичайний рядок обгортається в лапки`() {
        assertEquals("\"abc\"", Utils.getQuotationString("abc"))
    }

    // --- getCurrencyValue ---

    @Test
    fun `getCurrencyValue - числова клітинка форматується з крапкою як десятковим роздільником`() {
        assertEquals("234.50", Utils.getCurrencyValue(numericCell(234.5)))
    }

    @Test
    fun `getCurrencyValue - нечислова клітинка повертає нуль`() {
        assertEquals("0.00", Utils.getCurrencyValue(stringCell("текст")))
    }

    // --- getStatus ---

    @Test
    fun `getStatus - блок означає Невідомо`() {
        assertEquals("Невідомо", Utils.getStatus(blankCell()))
    }

    @Test
    fun `getStatus - непорожня клітинка означає Зареєстровано`() {
        assertEquals("Зареєстровано", Utils.getStatus(stringCell("будь-що")))
    }

    // --- getNotPrivate ---

    @Test
    fun `getNotPrivate - 10-значний ідентифікатор маскується`() {
        assertEquals("XXXXXXXXXX", Utils.getNotPrivate("1234567890"))
    }

    @Test
    fun `getNotPrivate - ідентифікатор юрособи не маскується`() {
        assertEquals("12345678", Utils.getNotPrivate("12345678"))
    }

    // --- getDt8601 ---

    @Test
    fun `getDt8601 - блок повертає null-рядок`() {
        assertEquals("null", Utils.getDt8601(blankCell()))
    }

    @Test
    fun `getDt8601 - строкова клітинка повертається як є`() {
        assertEquals("2020-01-01", Utils.getDt8601(stringCell("2020-01-01")))
    }

    @Test
    fun `getDt8601 - дата форматується як ISO 8601`() {
        assertEquals("2021-03-15", Utils.getDt8601(dateCell(2021, 3, 15)))
    }

    // --- formatToId ---

    @Test
    fun `formatToId - число форматується без десяткових знаків`() {
        assertEquals("22991617", Utils.formatToId(numericCell(22991617.0)))
    }

    // --- getCsvString ---

    @Test
    fun `getCsvString - формує заголовок та рядки через кому`() {
        val header = arrayOf("a", "b")
        val data = listOf(listOf("1", "2"), listOf("3", "4"))
        assertEquals("a,b\n1,2\n3,4\n", Utils.getCsvString(header, data))
    }

    @Test
    fun `getCsvString - порожні дані дають лише заголовок`() {
        assertEquals("a,b\n", Utils.getCsvString(arrayOf("a", "b"), emptyList()))
    }

    // --- getEtcCode ---

    @Test
    fun `getEtcCode - звичайний код повертається без змін`() {
        assertEquals("UA-XXXX-123", Utils.getEtcCode("UA-XXXX-123"))
    }

    @Test
    fun `getEtcCode - url з довгим останнім сегментом бере останній сегмент`() {
        assertEquals("UA-1234-567", Utils.getEtcCode("https://prozorro.sale/auction/UA-1234-567"))
    }

    @Test
    fun `getEtcCode - url з коротким останнім сегментом бере передостанній`() {
        assertEquals("UA-1234-567", Utils.getEtcCode("https://prozorro.sale/auction/UA-1234-567/1"))
    }

    // --- getUrl ---

    @Test
    fun `getUrl - LL-код формує посилання на аукціон`() {
        assertEquals("https://prozorro.sale/auction/LL-123", Utils.getUrl("LL-123"))
    }

    @Test
    fun `getUrl - UA-код формує посилання на аукціон`() {
        assertEquals("https://prozorro.sale/auction/UA-123", Utils.getUrl("UA-123"))
    }

    @Test
    fun `getUrl - RG-код формує посилання на планування`() {
        assertEquals("https://prozorro.sale/planning/RG-123", Utils.getUrl("RG-123"))
    }

    @Test
    fun `getUrl - інші коди повертаються без змін`() {
        assertEquals("XX-123", Utils.getUrl("XX-123"))
    }

    // --- getTitleBuilding ---

    @Test
    fun `getTitleBuilding - повертає назву за наявним ідентифікатором`() {
        val tab = mapOf("1" to buildingRow(BuildingIndex.title to "Будівля 1"))
        assertEquals("Будівля 1", Utils.getTitleBuilding(tab, "1"))
    }

    @Test
    fun `getTitleBuilding - невідомий ідентифікатор повертає Невідомо`() {
        assertEquals("Невідомо", Utils.getTitleBuilding(emptyMap(), "999"))
    }

    // --- is634 / is634m ---

    @Test
    fun `is634 - група містить 634`() {
        assertTrue(Utils.is634("634"))
        assertTrue(Utils.is634("1634"))
    }

    @Test
    fun `is634 - група не містить 634`() {
        assertFalse(Utils.is634("635"))
    }

    @Test
    fun `is634m - true якщо хоча б одна будівля має групу 634`() {
        val tab = mapOf(
            "1" to buildingRow(BuildingIndex.destinationGroup to "100"),
            "2" to buildingRow(BuildingIndex.destinationGroup to "634"),
        )
        assertTrue(Utils.is634m(tab, listOf("1", "2")))
    }

    @Test
    fun `is634m - false якщо жодна будівля не має групи 634`() {
        val tab = mapOf("1" to buildingRow(BuildingIndex.destinationGroup to "100"))
        assertFalse(Utils.is634m(tab, listOf("1")))
    }

    // --- isNotKyiv ---

    @Test
    fun `isNotKyiv - київський район повертає false`() {
        assertFalse(Utils.isNotKyiv("Печерський"))
        assertFalse(Utils.isNotKyiv("солом'янський"))
    }

    @Test
    fun `isNotKyiv - Києво-Святошинський вважається не київським`() {
        assertTrue(Utils.isNotKyiv("Києво-Святошинський"))
    }

    @Test
    fun `isNotKyiv - інший район повертає true`() {
        assertTrue(Utils.isNotKyiv("Бориспільський"))
    }

    // --- isBalanceHolderClosed ---

    @Test
    fun `isBalanceHolderClosed - відомий код повертає true`() {
        assertTrue(Utils.isBalanceHolderClosed("03328913"))
    }

    @Test
    fun `isBalanceHolderClosed - невідомий код повертає false`() {
        assertFalse(Utils.isBalanceHolderClosed("00000000"))
    }
}
