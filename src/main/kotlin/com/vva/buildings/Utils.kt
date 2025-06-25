package com.vva.buildings

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import java.text.SimpleDateFormat

object Utils {
    fun setQuotation(cell: Cell): String {
        if (cell.cellType == CellType.BLANK) {
            return "null"
        }
        val s = cell.toString()
        val s1 = s.replace("\"", "\"\"") // Escape double quotes
        return "\"$s1\""
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
}