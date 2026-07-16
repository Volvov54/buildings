package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

class ServiceXlsxTest {

    @Test
    fun `getWorkbook - зчитує коректний xlsx файл`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("test.xlsx")
        XSSFWorkbook().use { workbook ->
            workbook.createSheet("Аркуш1")
            FileOutputStream(file.toFile()).use { out -> workbook.write(out) }
        }

        ServiceXlsx.getWorkbook(file.toString()).use { result ->
            assertEquals(1, result.numberOfSheets)
            assertEquals("Аркуш1", result.getSheetAt(0).sheetName)
        }
    }

    @Test
    fun `getWorkbook - неіснуючий файл кидає RuntimeException з шляхом у повідомленні`() {
        val path = "не/існуючий/шлях.xlsx"
        val ex = assertThrows(RuntimeException::class.java) { ServiceXlsx.getWorkbook(path) }

        assertTrue(ex.message!!.contains(path))
        assertTrue(ex.cause is FileNotFoundException)
    }

    @Test
    fun `getWorkbook - файл з некоректним вмістом кидає RuntimeException`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("invalid.xlsx")
        Files.writeString(file, "це не xlsx файл")

        val ex = assertThrows(RuntimeException::class.java) { ServiceXlsx.getWorkbook(file.toString()) }
        assertTrue(ex.message!!.contains(file.toString()))
    }
}
