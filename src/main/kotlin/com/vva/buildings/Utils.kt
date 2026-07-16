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

    fun getCurrencyValue(cell: Cell): String =
        if (cell.cellType == CellType.NUMERIC)
            DecimalFormat("#,##0.00").format(cell.numericCellValue).replace(",", ".")
        else
            "0.00"

    fun getStatus(cell: Cell): String =
        if (cell.cellType == CellType.BLANK) "Невідомо" else "Зареєстровано"

    fun getNotPrivate(contractUserId: String): String =
        if (contractUserId.length == 10) "XXXXXXXXXX" else contractUserId

    fun getDt8601(cell: Cell): String = when (cell.cellType) {
        CellType.BLANK -> "null"
        CellType.STRING -> cell.toString()
        else -> SimpleDateFormat("yyyy-MM-dd").format(cell.dateCellValue)
    }

    fun formatToId(cell: Cell): String = DecimalFormat("#").format(cell.numericCellValue)

    /** Повертає formatToId(cell), якщо клітинка існує і має числовий тип; інакше null. */
    fun numericIdOrNull(cell: Cell?): String? =
        if (cell != null && cell.cellType == CellType.NUMERIC) formatToId(cell) else null

    fun getCsvString(header: Array<String>, tab: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append("\n")
        for (data in tab) {
            sb.append(data.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    fun getEtcCode(code: String): String {
        if (code.startsWith("http") || code.startsWith("hhttp")) {
            val parts = code.split("/")
            return if (parts.last().length < 2) parts[parts.size - 2] else parts.last()
        }
        return code
    }

    fun getUrl(etcCode: String): String = when (etcCode.slice(0..1)) {
        "LL", "UA" -> "https://prozorro.sale/auction/$etcCode"
        "RG" -> "https://prozorro.sale/planning/$etcCode"
        else -> etcCode
    }

    fun getTitleBuilding(
        tabBuildings: Map<String, List<String>>,
        idBuilding: String
    ): String = tabBuildings[idBuilding]?.get(BuildingIndex.title.index) ?: "Невідомо"

    fun is634(destinationGroup: String): Boolean {
        return destinationGroup.contains("634")
    }

    fun is634m(
        tabBuildings: Map<String, List<String>>,
        idBuildings: List<String>
    ): Boolean = idBuildings.any { id ->
        tabBuildings[id]?.get(BuildingIndex.destinationGroup.index)?.let { is634(it) } == true
    }

    fun isNotKyiv(address: String): Boolean {
        val normalized = address.lowercase().trim()
        return normalized == "києво-святошинський" || kyivDistricts.none { it in normalized }
    }

    val kyivDistricts = listOf(
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

    fun isBalanceHolderClosed(balanceHolderId: String) = balanceHolderId in clBalansHolder

    val clBalansHolder: Set<String> = setOf(
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