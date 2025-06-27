package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getStatus
import com.vva.buildings.Utils.isNotKyiv
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet

object Balans {
    fun getTabBalans(sheet: XSSFSheet): Map<String, List<String>> {
        val tabBuildings = mutableMapOf<String, MutableList<String>>()
        val rowIterator = sheet.iterator()

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val cellIndex = row.getCell(BalansIndex.id.ordinal)
            if (cellIndex.cellType != CellType.NUMERIC) continue
            val buildingId = formatToId(cellIndex)
            val isNotKievDistrict = isNotKyiv(
                row.getCell(BalansIndex.addressPostDistrict.ordinal)
                    .toString()
                    .lowercase()
                    .trim()
            )

            val buildingData = mutableListOf<String>()

            buildingData.add(buildingId)                            // id - ID об'єкту
            buildingData.add("null")                                // isPartOf
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.title.ordinal)))       // title - Назва об'єкту
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.kind.ordinal)))        // kind - Вид Об'єкту відповідно Класифікатора майна
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.type.ordinal)))        // type - Тип Об'єкту
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.description.ordinal))) // description - Призначення
            buildingData.add("Київська міська рада")                // ownerName
            buildingData.add("22883141")                            // ownerId
            buildingData.add(setQuotation(row.getCell(
                    BalansIndex.balanceHolderName.ordinal)))  // balanceHolderName - Балансоутримувач - Повна Назва
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.balanceHolderId.ordinal)))         // balanceHolderId - Балансоутримувач - Код ЄДРПОУ
            buildingData.add("null")                                // userName
            buildingData.add("null")                                // userId
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.dk018classId.ordinal)))            // dk018classId
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.dk018classDescription.ordinal)))   // dk018classDescription
            buildingData.add("кв. м.")                              // unitName
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.area.ordinal)))                    // area - Загальна Площа будинку (кв.м.)
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add("UA80000000000093317")        // CATUTTC
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostCode.ordinal)))         // addressPostCode - Поштовий індекс
            buildingData.add("Україна")                             // addressAdminUnitL1
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add("м. Київ")                    // addressAdminUnitL2
            buildingData.add("null")                                // addressAdminUnitL3
            if (isNotKievDistrict) buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostDistrict.ordinal)))
                else buildingData.add("null")                       // addressAdminUnitL4
            buildingData.add("null")                                // addressPostName
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostDistrict.ordinal)))     // addressPostDistrict - Район
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostStreet.ordinal)))       // addressPostStreet - Назва Вулиці
            buildingData.add("xxx")                                 // addressLocatorDesignator - Номер Будинку
            buildingData.add("null")                                // addressLocatorBuilding
            buildingData.add("null")                                // addressLocatorName
            buildingData.add(getStatus(row.getCell(
                BalansIndex.registrationId.ordinal)))          // registrationStatus - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.registrationId.ordinal)))          // registrationId - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add("null")                                // registrationDate
            buildingData.add("null")                                // constructionReadiness
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.condition.ordinal)))               // condition - Стан об'єкту
            buildingData.add("null")                                // utilitiesAvailable
            buildingData.add(getDt8601(row.getCell(
                BalansIndex.validityDate.ordinal)))            // validityDate - Дата Актуальності

            tabBuildings.put(buildingId, buildingData)
        }
        return tabBuildings
    }


    fun getBalansCsv(tabBuildings: Map<String, List<String>>): String =
        Utils.getCsvString(header, tabBuildings.values.toList())

    val header: Array<String> = arrayOf(
        "id",                        // id=0
        "isPartOf",                  // isPartOf=1
        "title",                     // title=2
        "kind",                      // kind=3
        "type",                      // type=4
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
