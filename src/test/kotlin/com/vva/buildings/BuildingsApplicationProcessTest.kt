package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

class BuildingsApplicationProcessTest {

    private fun writeSheet(file: Path, headerRow: Map<Int, String>, dataRows: Map<Int, List<Any?>>) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()
            sheet.createRow(0) // рядок 0 - декоративний, не валідується
            val header = sheet.createRow(1)
            headerRow.forEach { (col, value) -> header.createCell(col).setCellValue(value) }
            dataRows.forEach { (rowNum, values) ->
                val row = sheet.createRow(rowNum)
                values.forEachIndexed { colIndex, value ->
                    val cell = row.createCell(colIndex)
                    when (value) {
                        is Double -> cell.setCellValue(value)
                        is String -> cell.setCellValue(value)
                        null -> Unit
                    }
                }
            }
            FileOutputStream(file.toFile()).use { out -> workbook.write(out) }
        }
    }

    @Test
    fun `process - конструюється напряму з довільними шляхами і формує всі пʼять CSV-файлів`(@TempDir tempDir: Path) {
        val balansFile = tempDir.resolve("Баланс.xlsx")
        val freeSpaceFile = tempDir.resolve("ВільніПлощі.xlsx")
        val orendaFile = tempDir.resolve("Оренда.xlsx")

        writeSheet(
            balansFile,
            headerRow = mapOf(
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
            ),
            dataRows = mapOf(
                2 to listOf(
                    100.0, "Будівля 100", "Нежитлова", "Тип 1", "Офіс",
                    "КП Житлобуд", "12345678", "111", "Будівлі", 100.5,
                    "01001", "Печерський", "Хрещатик", "REG-1", "Задовільний",
                    "2021-01-01", "100", "1", "Управління",
                )
            )
        )

        writeSheet(
            freeSpaceFile,
            headerRow = mapOf(
                0 to "Реєстра-ційний №",
                1 to "ID об'єкту",
                2 to "Унікальний код обєкту у ЕТС Прозорро-продажі",
                3 to "Вільні приміщення",
                7 to "Наявність комунікацій",
                11 to "Додаткові",
            ),
            dataRows = mapOf(
                3 to listOf(1.0, 100.0, null, null, 25.5, null, null, "Так", "Так", "Мережа", "Так", "10А")
            )
        )

        writeSheet(
            orendaFile,
            headerRow = mapOf(
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
                18 to "Фактичне Закінчення Оренди",
            ),
            dataRows = mapOf(
                2 to listOf(
                    1.0, "100", null, 45.5, 500.75, "2021-01-01", "K-1", "2021-01-02",
                    "Договір діє", "КП Житлобуд", "12345678", "2021-02-01", "2022-02-01",
                    250.25, "ФОП Іванов", "1234567890", "15", "2021-06-01", null,
                )
            )
        )

        val outputDir = tempDir.resolve("output")
        Files.createDirectories(outputDir)

        val app = BuildingsApplication(
            pathInputBalans = balansFile.toString(),
            pathInputFreeSpace = freeSpaceFile.toString(),
            pathInputOrenda = orendaFile.toString(),
            pathOutputBuildings = outputDir.resolve("buildings.csv").toString(),
            pathOutputBuildings2 = outputDir.resolve("buildingsRentable.csv").toString(),
            pathOutputProzorro = outputDir.resolve("listProzorroSales_buildingsRentable.csv").toString(),
            pathOutputList = outputDir.resolve("list.csv").toString(),
            pathOutputListProzorroSales = outputDir.resolve("listProzorroSales_rented.csv").toString(),
        )

        app.process()

        val buildingsCsv = Files.readString(outputDir.resolve("buildings.csv"))
        assertEquals(Balans.header.joinToString(","), buildingsCsv.lines().first())
        assertTrue(buildingsCsv.contains("\"Будівля 100\""))

        val buildingsRentableCsv = Files.readString(outputDir.resolve("buildingsRentable.csv"))
        assertTrue(buildingsRentableCsv.contains("1-100"))

        val prozorroRentableCsv = Files.readString(outputDir.resolve("listProzorroSales_buildingsRentable.csv"))
        assertEquals(listOf(FreeSpace.headerProzorro.joinToString(",")), prozorroRentableCsv.lines().filter { it.isNotBlank() })

        val listCsv = Files.readString(outputDir.resolve("list.csv"))
        assertTrue(listCsv.contains("1-100"))

        val prozorroListCsv = Files.readString(outputDir.resolve("listProzorroSales_rented.csv"))
        assertEquals(listOf(Orenda.headerListProzorroSales.joinToString(",")), prozorroListCsv.lines().filter { it.isNotBlank() })
    }
}
