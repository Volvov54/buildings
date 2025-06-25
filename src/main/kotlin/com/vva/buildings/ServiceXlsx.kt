package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream

object ServiceXlsx {
    fun getWorkbook(pathInput: String): XSSFWorkbook {
        try {
            val inputStream = FileInputStream(pathInput)
            val workbook = XSSFWorkbook(inputStream)
            if (workbook == null) {
                throw IllegalArgumentException("Workbook is null. Please check the file path or format.")
            }
            return workbook
        } catch (e: Exception) {
            throw RuntimeException("Error reading the Excel file at $pathInput: ${e.message}", e)
        }
    }
}