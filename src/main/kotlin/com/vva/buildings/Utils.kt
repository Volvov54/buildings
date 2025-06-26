package com.vva.buildings

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import java.text.DecimalFormat
import java.text.SimpleDateFormat

object Utils {
    fun setQuotation(cell: Cell): String {
        if (cell.cellType == CellType.BLANK) {
            return "null"
        }
        val s = cell.toString()
        return getQuotationString(s)
    }

    fun getQuotationString(s: String?): String {
        return if (s.isNullOrBlank()) {
            "null"
        } else {
            val s1 = s.replace("\"", "\"\"") // Escape double quotes
            "\"$s1\""
        }
    }

    fun getStatus(cell: Cell): String {
        return if (cell.cellType == CellType.BLANK) {
            "Невідомо"
        } else {
            "Зареєстровано"
        }
    }

    fun getDt8601(cell: Cell): String {
        if (cell.cellType == CellType.BLANK) {
           return "null"
        } else {
            val dt = cell.dateCellValue
            val formatOut = SimpleDateFormat("yyyy-MM-dd")
            return formatOut.format(dt)
        }
    }

    fun formatToId(cell: Cell): String {
        val decimalFormat = DecimalFormat("#")
        return decimalFormat.format(cell.numericCellValue)
    }

    fun getCsvString(header: Array<String>, tab: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append("\n") // Header for the CSV
        for (data in tab) {
            sb.append(data.joinToString(",")).append("\n")
        }
        return sb.toString()
    }
}