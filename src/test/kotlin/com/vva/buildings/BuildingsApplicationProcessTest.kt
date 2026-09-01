package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
            headerRow = BalansIndex.entries.associate { it.ordinal to it.header },
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
            headerRow = FreeSpaceIndex.entries.associate { it.ordinal to it.header },
            dataRows = mapOf(
                // idSpace, buildingId, etcCode, area, water, heat, electric, gas, designator
                3 to listOf(1.0, 100.0, null, 25.5, "Так", "Так", "Мережа", "Так", "10А")
            )
        )

        writeSheet(
            orendaFile,
            headerRow = OrendaIndex.entries.associate { it.ordinal to it.header },
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

    @Test
    fun `process - прокидає виняток якщо структура Баланс xlsx не відповідає очікуваній`(@TempDir tempDir: Path) {
        val balansFile = tempDir.resolve("Баланс.xlsx")
        writeSheet(
            balansFile,
            headerRow = mapOf(0 to "Неправильний заголовок"),
            dataRows = emptyMap()
        )

        val outputDir = tempDir.resolve("output")
        Files.createDirectories(outputDir)

        val app = BuildingsApplication(
            pathInputBalans = balansFile.toString(),
            pathInputFreeSpace = "не використовується",
            pathInputOrenda = "не використовується",
            pathOutputBuildings = outputDir.resolve("buildings.csv").toString(),
            pathOutputBuildings2 = outputDir.resolve("buildingsRentable.csv").toString(),
            pathOutputProzorro = outputDir.resolve("listProzorroSales_buildingsRentable.csv").toString(),
            pathOutputList = outputDir.resolve("list.csv").toString(),
            pathOutputListProzorroSales = outputDir.resolve("listProzorroSales_rented.csv").toString(),
        )

        assertThrows(IllegalStateException::class.java) { app.process() }
    }

    @Test
    fun `process - кидає виняток якщо каталог виводу не існує`(@TempDir tempDir: Path) {
        // CLAUDE.md документує вимогу: каталог data/output має існувати заздалегідь.
        val balansFile = tempDir.resolve("Баланс.xlsx")
        writeSheet(
            balansFile,
            headerRow = BalansIndex.entries.associate { it.ordinal to it.header },
            dataRows = emptyMap()
        )

        val missingOutputDir = tempDir.resolve("немає-такого-каталогу")

        val app = BuildingsApplication(
            pathInputBalans = balansFile.toString(),
            pathInputFreeSpace = "не використовується",
            pathInputOrenda = "не використовується",
            pathOutputBuildings = missingOutputDir.resolve("buildings.csv").toString(),
            pathOutputBuildings2 = missingOutputDir.resolve("buildingsRentable.csv").toString(),
            pathOutputProzorro = missingOutputDir.resolve("listProzorroSales_buildingsRentable.csv").toString(),
            pathOutputList = missingOutputDir.resolve("list.csv").toString(),
            pathOutputListProzorroSales = missingOutputDir.resolve("listProzorroSales_rented.csv").toString(),
        )

        assertThrows(java.nio.file.NoSuchFileException::class.java) { app.process() }
    }
}
