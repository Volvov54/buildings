package com.vva.buildings

import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getStatus
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import java.text.DecimalFormat

object Balans {
    fun getTabBalans(sheet: XSSFSheet): Map<String, List<String>> {
        val tabBuildings = mutableMapOf<String, MutableList<String>>()
        val rowIterator = sheet.iterator()

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val decimalFormat = DecimalFormat("#")
            val cell0 = row.getCell(0)
            if (cell0.cellType != CellType.NUMERIC) continue
            val buildingId = decimalFormat.format(cell0.numericCellValue)

            val buildingData = mutableListOf<String>()

            buildingData.add(buildingId)                            // id - ID об'єкту
            buildingData.add("null")                                // isPartOf
            buildingData.add(setQuotation(row.getCell(1)))  // title - Назва об'єкту
            buildingData.add(setQuotation(row.getCell(2)))  // kind - Вид Об'єкту відповідно Класифікатора майна
            buildingData.add(setQuotation(row.getCell(3)))  // type - Тип Об'єкту
            buildingData.add(setQuotation(row.getCell(4)))  // description - Призначення
            buildingData.add("Київська міська рада")                // ownerName
            buildingData.add("22883141")                            // ownerId
            buildingData.add(setQuotation(row.getCell(5)))  // balanceHolderName - Балансоутримувач - Повна Назва
            buildingData.add(setQuotation(row.getCell(6)))  // balanceHolderId - Балансоутримувач - Код ЄДРПОУ
            buildingData.add("null")                                // userName
            buildingData.add("null")                                // userId
            buildingData.add(setQuotation(row.getCell(7)))  // dk018classId
            buildingData.add(setQuotation(row.getCell(8)))  // dk018classDescription
            buildingData.add("кв. м.")                              // unitName
            buildingData.add(setQuotation(row.getCell(9)))  // area - Загальна Площа будинку (кв.м.)
            buildingData.add("UA80000000000093317")                 // CATUTTC
            buildingData.add(setQuotation(row.getCell(10))) // addressPostCode - Поштовий індекс
            buildingData.add("Україна")                             // addressAdminUnitL1
            buildingData.add("м. Київ")                             // addressAdminUnitL2
            buildingData.add("null")                                // addressAdminUnitL3
            buildingData.add("null")                                // addressAdminUnitL4
            buildingData.add("null")                                // addressPostName
            buildingData.add(setQuotation(row.getCell(11))) // addressPostDistrict - Район
            buildingData.add(setQuotation(row.getCell(12))) // addressPostStreet - Назва Вулиці
            buildingData.add("xxx")                                 // addressLocatorDesignator - Номер Будинку
            buildingData.add("null")                                // addressLocatorBuilding
            buildingData.add("null")                                // addressLocatorName
            buildingData.add(getStatus(row.getCell(13))) // registrationStatus - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add(setQuotation(row.getCell(13))) // registrationId - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add("null")                                // registrationDate
            buildingData.add("null")                                // constructionReadiness
            buildingData.add(setQuotation(row.getCell(14))) // condition - Стан об'єкту
            buildingData.add("null")                                // utilitiesAvailable
            buildingData.add(getDt8601(row.getCell(15))) // validityDate - Дата Актуальності

            tabBuildings.put(buildingId, buildingData)
        }
        return tabBuildings
    }


    fun getBalansCsv(tabBuildings: Map<String, List<String>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append("\n")
        for ((_, data) in tabBuildings) {
            sb.append(data.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    val header: Array<String> = arrayOf(
        "id",
        "isPartOf",
        "title",
        "kind",
        "type",
        "description",
        "ownerName",
        "ownerId",
        "balanceHolderName",
        "balanceHolderId",
        "userName",
        "userId",
        "dk018classId",
        "dk018classDescription",
        "unitName",
        "area",
        "CATUTTC",
        "addressPostCode",
        "addressAdminUnitL1",
        "addressAdminUnitL2",
        "addressAdminUnitL3",
        "addressAdminUnitL4",
        "addressPostName",
        "addressPostDistrict",
        "addressPostStreet",
        "addressLocatorDesignator",
        "addressLocatorBuilding",
        "addressLocatorName",
        "registrationStatus",
        "registrationId",
        "registrationDate",
        "constructionReadiness",
        "condition",
        "utilitiesAvailable",
        "validityDate",
    )
}
