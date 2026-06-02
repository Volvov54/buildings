package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream

object ServiceXlsx {
    fun getWorkbook(pathInput: String): XSSFWorkbook {
        try {
            return XSSFWorkbook(FileInputStream(pathInput))
        } catch (e: Exception) {
            throw RuntimeException("Error reading the Excel file at $pathInput: ${e.message}", e)
        }
    }
}