package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class InputValidatorTest {

    private val workbook = XSSFWorkbook()

    @AfterEach
    fun tearDown() {
        workbook.close()
    }

    /** Аркуш з заголовком у другому рядку (індекс 1) — так, як його очікує InputValidator. */
    private fun sheetWithHeaderRow(headers: Map<Int, String>): XSSFSheet {
        val sheet = workbook.createSheet()
        sheet.createRow(0) // перший рядок ігнорується валідатором
        val row = sheet.createRow(1)
        headers.forEach { (colIndex, value) -> row.createCell(colIndex).setCellValue(value) }
        return sheet
    }

    private val balansHeaders = mapOf(
        0 to "ID об'єкту",
        1 to "Назва Об'єкту",
        2 to "Вид Об'єкту відповідно Класифікатора майна",
        3 to "Тип Об'єкту",
        4 to "Призначення",
        5 to "Балансоутримувач - Повна Назва",
        6 to "Балансоутримувач - Код ЄДРПОУ",
        7 to "Вид Об'єкту відповідно Класифікатора майна (код)",
        8 to "Вид Об'єкту відповідно Класифікатора майна (назва)",
        9 to "Загальна Площа будинку (кв.м.)",
        10 to "Поштовий індекс",
        11 to "Район",
        12 to "Назва Вулиці",
        13 to "Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)",
        14 to "Стан Об'єкту",
        15 to "Дата Актуальності",
        16 to "Група Призначення",
        17 to "Номер Будинку",
        18 to "Сфера діяльності",
    )

    private val freeSpaceHeaders = mapOf(
        0 to "Реєстра-ційний №",
        1 to "ID об'єкту",
        2 to "Унікальний код обєкту у ЕТС Прозорро-продажі",
        3 to "Вільні приміщення",
        7 to "Наявність комунікацій",
        11 to "Додаткові",
    )

    private val orendaHeaders = mapOf(
        0 to "ID договору",
        1 to "ID об’єктів за договором", // U+2019 apostrophe
        2 to "Унікальний код обєкту у ЕТС Прозорро-продажі",
        3 to "Площа що орендується, кв.м",
        4 to "Оціночна вартість приміщень за договором, грн",
        5 to "Дата, на яку проведена оцінка об'єкту",
        6 to "Номер Договору Оренди",
        7 to "Дата укладання договору",
        8 to "Стан договору",
        9 to "Балансоутримувач - Повна Назва",
        10 to "Балансоутримувач - Код ЄДРПОУ",
        11 to "Дата початку використання приміщення",
        12 to "Закінчення Оренди",
        13 to "Місячна орендна плата, грн.",
        14 to "Орендар - Повна Назва",
        15 to "Орендар - Код ЄДРПОУ",
        16 to "Номер Будинку",
        17 to "Дата Актуальності",
        18 to "Фактичне Закінчення Оренди",
    )

    // --- Баланс ---

    @Test
    fun `validateBalans - коректні заголовки не кидають виняток`() {
        assertDoesNotThrow { InputValidator.validateBalans(sheetWithHeaderRow(balansHeaders)) }
    }

    @Test
    fun `validateBalans - невідповідний заголовок кидає виняток з деталями колонки`() {
        val sheet = sheetWithHeaderRow(balansHeaders + (1 to "Неправильний заголовок"))
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateBalans(sheet) }

        assertTrue(ex.message!!.contains("колонка[1]"))
        assertTrue(ex.message!!.contains("Назва Об'єкту"))
        assertTrue(ex.message!!.contains("Неправильний заголовок"))
    }

    @Test
    fun `validateBalans - усі одночасні невідповідності потрапляють в один виняток`() {
        val sheet = sheetWithHeaderRow(balansHeaders + mapOf(1 to "Помилка 1", 5 to "Помилка 2"))
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateBalans(sheet) }

        assertTrue(ex.message!!.contains("колонка[1]"))
        assertTrue(ex.message!!.contains("колонка[5]"))
    }

    @Test
    fun `validateBalans - відсутній другий рядок кидає виняток`() {
        val sheet = workbook.createSheet()
        sheet.createRow(0)
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateBalans(sheet) }

        assertTrue(ex.message!!.contains("відсутній другий рядок"))
    }

    // --- ВільніПлощі ---

    @Test
    fun `validateFreeSpace - коректні непорожні колонки не кидають виняток`() {
        assertDoesNotThrow { InputValidator.validateFreeSpace(sheetWithHeaderRow(freeSpaceHeaders)) }
    }

    @Test
    fun `validateFreeSpace - ігнорує колонки поза списком очікуваних (злиті клітинки)`() {
        val sheet = sheetWithHeaderRow(
            freeSpaceHeaders + mapOf(4 to "будь-що", 5 to "будь-що інше", 8 to "теж не перевіряється")
        )
        assertDoesNotThrow { InputValidator.validateFreeSpace(sheet) }
    }

    @Test
    fun `validateFreeSpace - невідповідність у перевірюваній колонці кидає виняток`() {
        val sheet = sheetWithHeaderRow(freeSpaceHeaders + (7 to "Неправильно"))
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateFreeSpace(sheet) }

        assertTrue(ex.message!!.contains("колонка[7]"))
    }

    // --- Оренда ---

    @Test
    fun `validateOrenda - коректні заголовки не кидають виняток`() {
        assertDoesNotThrow { InputValidator.validateOrenda(sheetWithHeaderRow(orendaHeaders)) }
    }

    @Test
    fun `validateOrenda - звичайний апостроф замість U+2019 кидає виняток`() {
        val sheet = sheetWithHeaderRow(orendaHeaders + (1 to "ID об'єктів за договором")) // U+0027
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateOrenda(sheet) }

        assertTrue(ex.message!!.contains("колонка[1]"))
    }

    @Test
    fun `validateOrenda - невідповідний заголовок кидає виняток`() {
        val sheet = sheetWithHeaderRow(orendaHeaders + (8 to "Неправильно"))
        val ex = assertThrows(IllegalStateException::class.java) { InputValidator.validateOrenda(sheet) }

        assertTrue(ex.message!!.contains("колонка[8]"))
    }
}
