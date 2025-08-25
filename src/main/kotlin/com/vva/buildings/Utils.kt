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
        val s = cell.toString().trim()
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

    fun getCurrencyValue(cell: Cell): String {
        return if (cell.cellType == CellType.NUMERIC) {
            val decimalFormat = DecimalFormat("#,##0.00")
            val d = decimalFormat.format(cell.numericCellValue)
            if (d.isNotBlank()) {
                d.replace(",", ".") // Replace comma with dot for decimal
            } else {
                "0.00"
            }
        } else {
            "0.00"
        }
    }

    fun getStatus(cell: Cell): String {
        return if (cell.cellType == CellType.BLANK) {
            "Невідомо"
        } else {
            "Зареєстровано"
        }
    }

    fun getNotPrivate(contractUserId: String): String {
        return if (contractUserId.length  == 10) {
            "XXXXXXXXXX" // Masking private person ID
        } else {
            contractUserId
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
        if (code.startsWith("http") || code.startsWith("hhttp")) {
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

    fun is634m(
        tabBuildings: Map<String, List<String>>,
        idBuildings: List<String>
    ): Boolean {
        idBuildings.forEach { id ->
            if (tabBuildings.containsKey(id)) {
                val destinationGroup = tabBuildings[id]?.get(BuildingIndex.destinationGroup.ordinal)
                if (destinationGroup != null && is634(destinationGroup)) {
                    return true
                }
            }
        }
        return false
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

    fun isBalanceHolderClosed(balanceHolderId: String): Boolean {
        return if (clBalansHolder.contains(balanceHolderId)) true
        else false
    }

    val clBalansHolder: Array<String> = arrayOf(
        "03328913", // КОМУНАЛЬНЕ ПІДПРИЄМСТВО "КИЇВСЬКИЙ МЕТРОПОЛІТЕН"
        "31725604", // КОМУНАЛЬНЕ ПІДПРИЄМСТВО "КИЇВПАСТРАНС"
        "26112340", // ДЕПАРТАМЕНТ ЕКСПЛУАТАЦІЇ ВОДОПРОВІДНОГО ГОСПОДАРСТВА ПрАТ "АК "КИЇВВОДОКАНАЛ"
        "03327664", // ГОЛОВНИЙ ОФІС ПрАТ "АК"КИЇВВОДОКАНАЛ"
        "03358400",  // ПРИВАТНЕ АКЦІОНЕРНЕ ТОВАРИСТВО «АКЦІОНЕРНА КОМПАНІЯ «КИЇВВОДОКАНАЛ»
        // БОРТНИЦЬКА СТАНЦІЯ АЕРАЦІЇ
        "26112475", // ДЕПАРТАМЕНТ ЕКСПЛУАТАЦІЇ КАНАЛІЗАЦІЙНОГО ГОСПОДАРСТВА УПРАВЛІННЯ
        // ЕКСПЛУАТАЦІЇ КАНАЛІЗАЦІЙНИХ МЕРЕЖ І НАСОСНИХ СТАНЦІЙ ПрАТ "АК "КИЇВВОДОКАНАЛ"
        "37292855", // СПЕЦІАЛІЗОВАНЕ ВОДОГОСПОДАРСЬКЕ КОМУНАЛЬНЕ ПІДПРИЄМСТВО ВИКОНАВЧОГО ОРГАНУ
        // КИЇВСЬКОЇ МІСЬКОЇ РАДИ (КИЇВСЬКОЇ МІСЬКОЇ ДЕРЖАВНОЇ АДМІНІСТРАЦІЇ) "КИЇВВОДФОНД"
        "25665166", // ФІЛІАЛ ТЕПЛОВІ РОЗПОДІЛЬЧІ МЕРЕЖІ КИЇВЕНЕРГО ПУБЛІЧНОГО
        // АКЦІОНЕРНОГО ТОВАРИСТВА КИЇВЕНЕРГО
        "40538421", // КОМУНАЛЬНЕ ПІДПРИЄМСТВО ВИКОНАВЧОГО ОРГАНУ КИЇВРАДИ
        // (КИЇВСЬКОЇ МІСЬКОЇ ДЕРЖАВНОЇ АДМІНІСТРАЦІЇ) КИЇВТЕПЛОЕНЕРГО
        "31752994" // ТОВАРИСТВО З ОБМЕЖЕНОЮ ВІДПОВІДАЛЬНІСТЮ "СПЕЦІАЛІЗОВАНЕ ВИРОБНИЧО-НАУКОВЕ
        // ПІДПРИЄМСТВО "КИЇВПРОМЕНЕРГО"
    )
}