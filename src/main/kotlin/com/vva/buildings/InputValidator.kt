package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.LoggerFactory

object InputValidator {
    private val logger = LoggerFactory.getLogger(InputValidator::class.java)

    // Другий рядок (індекс 1) файлу Баланс.xlsx. Ключі - BalansIndex.index, щоб перевірка колонок
    // не розходилась з парсингом у Balans.kt, якщо колонки колись зміщаться.
    private val balansExpected = mapOf(
        BalansIndex.id.index to "ID об'єкту",
        BalansIndex.title.index to "Назва Об'єкту",
        BalansIndex.kind.index to "Вид Об'єкту відповідно Класифікатора майна",
        BalansIndex.type.index to "Тип Об'єкту",
        BalansIndex.description.index to "Призначення",
        BalansIndex.balanceHolderName.index to "Балансоутримувач - Повна Назва",
        BalansIndex.balanceHolderId.index to "Балансоутримувач - Код ЄДРПОУ",
        BalansIndex.dk018classId.index to "Вид Об'єкту відповідно Класифікатора майна (код)",
        BalansIndex.dk018classDescription.index to "Вид Об'єкту відповідно Класифікатора майна (назва)",
        BalansIndex.area.index to "Загальна Площа будинку (кв.м.)",
        BalansIndex.addressPostCode.index to "Поштовий індекс",
        BalansIndex.addressPostDistrict.index to "Район",
        BalansIndex.addressThoroughfare.index to "Назва Вулиці",
        BalansIndex.registrationId.index to "Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)",
        BalansIndex.condition.index to "Стан Об'єкту",
        BalansIndex.validityDate.index to "Дата Актуальності",
        BalansIndex.destinationGroup.index to "Група Призначення",
        BalansIndex.addressLocatorDesignator.index to "Номер Будинку",
        BalansIndex.fieldOfActivity.index to "Сфера діяльності"
    )

    // Другий рядок (індекс 1) файлу ВільніПлощі.xlsx — перевіряємо лише непусті клітинки.
    // На відміну від Balans/Orenda, тут немає чистої відповідності 1:1 з FreeSpaceIndex: через
    // злиті клітинки в заголовку колонка 3 не має відповідного поля в enum, тому ключі - числові
    // літерали (номери фізичних колонок), а не FreeSpaceIndex.index.
    private val freeSpaceExpected = mapOf(
        0 to "Реєстра-ційний №",
        1 to "ID об'єкту",
        2 to "Унікальний код обєкту у ЕТС Прозорро-продажі",
        3 to "Вільні приміщення",
        7 to "Наявність комунікацій",
        11 to "Додаткові"
    )

    // Другий рядок (індекс 1) файлу Оренда.xlsx. Ключі - OrendaIndex.index, щоб перевірка колонок
    // не розходилась з парсингом у Orenda.kt, якщо колонки колись зміщаться.
    private val orendaExpected = mapOf(
        OrendaIndex.id.index to "ID договору",
        OrendaIndex.idBuilding.index to "ID об’єктів за договором",
        OrendaIndex.etcCode.index to "Унікальний код обєкту у ЕТС Прозорро-продажі",
        OrendaIndex.quantity.index to "Площа що орендується, кв.м",
        OrendaIndex.valueAmount.index to "Оціночна вартість приміщень за договором, грн",
        OrendaIndex.valuationDate.index to "Дата, на яку проведена оцінка об'єкту",
        OrendaIndex.contractNumber.index to "Номер Договору Оренди",
        OrendaIndex.contractDateSigned.index to "Дата укладання договору",
        OrendaIndex.contractStatus.index to "Стан договору",
        OrendaIndex.contractCustodianName.index to "Балансоутримувач - Повна Назва",
        OrendaIndex.contractCustodianId.index to "Балансоутримувач - Код ЄДРПОУ",
        OrendaIndex.contractPeriodStartDate.index to "Дата початку використання приміщення",
        OrendaIndex.contractPeriodEndDate.index to "Закінчення Оренди",
        OrendaIndex.contractValueAmount.index to "Місячна орендна плата, грн.",
        OrendaIndex.contractUserName.index to "Орендар - Повна Назва",
        OrendaIndex.contractUserId.index to "Орендар - Код ЄДРПОУ",
        OrendaIndex.addressLocatorDesignator.index to "Номер Будинку",
        OrendaIndex.validityDate.index to "Дата Актуальності",
        OrendaIndex.contractFactPeriodEndDate.index to "Фактичне Закінчення Оренди"
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
