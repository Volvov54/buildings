package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.LoggerFactory

object InputValidator {
    private val logger = LoggerFactory.getLogger(InputValidator::class.java)

    // Другий рядок (індекс 1) файлу Баланс.xlsx
    private val balansExpected = mapOf(
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
        18 to "Сфера діяльності"
    )

    // Другий рядок (індекс 1) файлу ВільніПлощі.xlsx — перевіряємо лише непусті клітинки
    private val freeSpaceExpected = mapOf(
        0 to "Реєстра-ційний №",
        1 to "ID об'єкту",
        2 to "Унікальний код обєкту у ЕТС Прозорро-продажі",
        3 to "Вільні приміщення",
        7 to "Наявність комунікацій",
        11 to "Додаткові"
    )

    // Другий рядок (індекс 1) файлу Оренда.xlsx
    private val orendaExpected = mapOf(
        0 to "ID договору",
        1 to "ID об’єктів за договором",
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
        18 to "Фактичне Закінчення Оренди"
    )

    fun validateBalans(sheet: XSSFSheet) = validate(sheet, balansExpected, "Баланс.xlsx")
    fun validateFreeSpace(sheet: XSSFSheet) = validate(sheet, freeSpaceExpected, "ВільніПлощі.xlsx")
    fun validateOrenda(sheet: XSSFSheet) = validate(sheet, orendaExpected, "Оренда.xlsx")

    private fun validate(sheet: XSSFSheet, expected: Map<Int, String>, fileName: String) {
        val row = sheet.getRow(1)
            ?: throw IllegalStateException("Файл $fileName: відсутній другий рядок (заголовки)")

        val errors = expected.mapNotNull { (colIndex, expectedText) ->
            val actual = row.getCell(colIndex)?.toString()?.trim() ?: ""
            if (actual != expectedText)
                "  колонка[$colIndex]: очікується \"$expectedText\", отримано \"$actual\""
            else null
        }

        if (errors.isNotEmpty()) {
            val message = "Файл $fileName: невідповідність структури заголовків (рядок 2):\n${errors.joinToString("\n")}"
            logger.error(message)
            throw IllegalStateException(message)
        }
        logger.info("Файл $fileName: структура заголовків коректна")
    }
}
