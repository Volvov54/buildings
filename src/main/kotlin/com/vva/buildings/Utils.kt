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
        } else if (cell.cellType == CellType.STRING) {
            return cell.toString()
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

    fun getEtcCode(code: String): String {
        if (code.startsWith("http")) {
            val parts = code.split("/")
            if (parts.last().length < 2) {
                return parts.get(parts.size - 2)
            }
            return parts.last() // Extract the last part of the URL
        }
        else return code
    }

    fun getUrl(etcCode: String): String {
        when (etcCode.slice(0..1)) {
            "LL" -> return "https://prozorro.sale/auction/$etcCode"
            "UA" -> return "https://prozorro.sale/auction/$etcCode"
            "RG" -> return "https://prozorro.sale/planning/$etcCode"
            else -> return etcCode
        }
    }

    fun getTitleBuilding(
        tabBuildings: Map<String, List<String>>,
        idBuilding: String
    ): String {
        return if (tabBuildings.containsKey(idBuilding)) {
            tabBuildings[idBuilding]?.get(BuildingIndex.title.ordinal) ?: "Невідомо" // Назва об'єкту
        } else {
            "Невідомо"
        }
    }

    fun is634(destinationGroup: String): Boolean {
        return destinationGroup.contains("634")
    }

    fun isNotKyiv(address: String): Boolean {
        val _address = address.lowercase().trim()
        if (_address == "КИЄВО-СВЯТОШИНСЬКИЙ".lowercase()) {
            return true
        }
        return KyivDestrict.find { it in _address }.isNullOrBlank()
    }

    val KyivDestrict = listOf(
        "голосіївський",
        "дарницький",
        "деснянський",
        "дніпровський",
        "оболонський",
        "печерський",
        "подільський",
        "святошинський",
        "шевченківський",
        "солом'янський",
    )
}