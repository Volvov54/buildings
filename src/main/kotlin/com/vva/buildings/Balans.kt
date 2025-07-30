package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getCsvString
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getStatus
import com.vva.buildings.Utils.is634
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

            buildingData.add(buildingId)                           // id - ID об'єкту
            buildingData.add("null")                               // isPartOf
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.title.index)))        // title - Назва об'єкту
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.kind.index)))         // kind - Вид Об'єкту відповідно Класифікатора майна
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.type.index)))         // type - Тип Об'єкту
            buildingData.add(setQuotation(
                row.getCell(BalansIndex.description.index)))  // description - Призначення
            buildingData.add("Київська міська рада")               // ownerName
            buildingData.add("22883141")                           // ownerId
            buildingData.add(setQuotation(row.getCell(
                    BalansIndex.balanceHolderName.index)))    // balanceHolderName - Балансоутримувач - Повна Назва
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.balanceHolderId.index)))          // balanceHolderId - Балансоутримувач - Код ЄДРПОУ
            buildingData.add("null")                               // userName
            buildingData.add("null")                               // userId
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.dk018classId.index)))             // dk018classId
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.dk018classDescription.index)))    // dk018classDescription
            buildingData.add("кв. м.")                             // unitName
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.area.index)))                     // area - Загальна Площа будинку (кв.м.)
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add("UA80000000000093317")       // CATUTTC
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostCode.ordinal)))        // addressPostCode - Поштовий індекс
            buildingData.add("Україна")                            // addressAdminUnitL1
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add("м. Київ")                   // addressAdminUnitL2
            buildingData.add("null")                               // addressAdminUnitL3
            if (isNotKievDistrict) buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostDistrict.index)))
                else buildingData.add("null")                      // addressAdminUnitL4
            if (isNotKievDistrict) buildingData.add("null")
            else buildingData.add("Київ")                          // addressPostName
            if (isNotKievDistrict) buildingData.add("null")
                else buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostDistrict.index)))      // addressPostDistrict - Район
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.addressPostStreet.index)))        // addressPostStreet - Назва Вулиці
            buildingData.add("xxx")                                // addressLocatorDesignator - Номер Будинку
            buildingData.add("null")                               // addressLocatorBuilding
            buildingData.add("null")                               // addressLocatorName
            buildingData.add(getStatus(row.getCell(
                BalansIndex.registrationId.index)))           // registrationStatus - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.registrationId.index)))           // registrationId - Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
            buildingData.add("null")                               // registrationDate
            buildingData.add("null")                               // constructionReadiness
            buildingData.add(setQuotation(row.getCell(
                BalansIndex.condition.index)))                // condition - Стан об'єкту
            buildingData.add("null")                               // utilitiesAvailable
            buildingData.add(getDt8601(row.getCell(
                BalansIndex.validityDate.index)))             // validityDate - Дата Актуальності
            buildingData.add(row.getCell(
                BalansIndex.destinationGroup.index)
                .toString())                                       // destinationGroup - Група призначення

            tabBuildings.put(buildingId, buildingData)
        }
        return tabBuildings
    }

    fun getBalansCsv(tabBuildings: Map<String, List<String>>): String {
        println("tabBuildings.size = ${tabBuildings.size}")
        val list = tabBuildings.values.toList().filter {
            !is634(it[BuildingIndex.destinationGroup.index]) }
        println("list.size = ${list.size}")
        val listNot634 = mutableListOf<List<String>>()
        for (item in list) {
                listNot634.add(item.slice(0..BuildingIndex.validityDate.index))
        }
        return getCsvString(header, listNot634)
    }

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
