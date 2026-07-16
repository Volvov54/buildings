package com.vva.buildings

import com.vva.buildings.Utils.getCsvString
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getQuotationString
import com.vva.buildings.Utils.getStatus
import com.vva.buildings.Utils.is634
import com.vva.buildings.Utils.isNotKyiv
import com.vva.buildings.Utils.numericIdOrNull
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.LoggerFactory

object Balans {
    private val logger = LoggerFactory.getLogger(Balans::class.java)

    private fun Row.quoted(index: BalansIndex): String = setQuotation(getCell(index.index))

    fun getTabBalans(sheet: XSSFSheet): Map<String, List<String>> {
        val tabBuildings = mutableMapOf<String, MutableList<String>>()
        val rowIterator = sheet.iterator()

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val balanceHolderId = row.getCell(BalansIndex.balanceHolderId.index).toString().trim()
            if (balanceHolderId == "22991617") continue

            val fieldOfActivity = row.getCell(BalansIndex.fieldOfActivity.index).toString().trim()
            if (fieldOfActivity == "Невизначені") continue

            val buildingId = numericIdOrNull(row.getCell(BalansIndex.id.index)) ?: continue
            val isNotKievDistrict = isNotKyiv(
                row.getCell(BalansIndex.addressPostDistrict.index).toString().lowercase().trim()
            )

            val buildingData = mutableListOf<String>()
            buildingData.add(buildingId)                                                  // id - ID об'єкту
            buildingData.add("null")                                                       // isPartOf
            buildingData.add(row.quoted(BalansIndex.title))                                // title - Назва об'єкту
            buildingData.add(row.quoted(BalansIndex.kind))                                 // kind - Вид Об'єкту відповідно Класифікатора майна
            buildingData.add(row.quoted(BalansIndex.type))                                 // type - Тип Об'єкту
            buildingData.add(row.quoted(BalansIndex.description))                          // description - Призначення
            buildingData.add("Київська міська рада")                                       // ownerName
            buildingData.add("22883141")                                                   // ownerId
            buildingData.add(row.quoted(BalansIndex.balanceHolderName))                    // balanceHolderName
            buildingData.add(getQuotationString(balanceHolderId))                          // balanceHolderId
            buildingData.add("null")                                                       // userName
            buildingData.add("null")                                                       // userId
            buildingData.add(row.quoted(BalansIndex.dk018classId))                         // dk018classId
            buildingData.add(row.quoted(BalansIndex.dk018classDescription))                // dk018classDescription
            buildingData.add("кв. м.")                                                     // unitName
            buildingData.add(row.quoted(BalansIndex.area))                                 // area - Загальна Площа будинку (кв.м.)
            buildingData.add(if (isNotKievDistrict) "null" else "UA80000000000093317")     // CATUTTC
            buildingData.add(row.quoted(BalansIndex.addressPostCode))                      // addressPostCode - Поштовий індекс
            buildingData.add("Україна")                                                    // addressAdminUnitL1
            buildingData.add(if (isNotKievDistrict) "null" else "м. Київ")                 // addressAdminUnitL2
            buildingData.add("null")                                                       // addressAdminUnitL3
            buildingData.add(
                if (isNotKievDistrict) row.quoted(BalansIndex.addressPostDistrict) else "null"
            )                                                                              // addressAdminUnitL4
            buildingData.add(if (isNotKievDistrict) "null" else "Київ")                    // addressPostName
            buildingData.add(
                if (isNotKievDistrict) "null" else row.quoted(BalansIndex.addressPostDistrict)
            )                                                                              // addressPostDistrict - Район
            buildingData.add(row.quoted(BalansIndex.addressThoroughfare))                  // addressThoroughfare - Назва Вулиці
            buildingData.add("XXX")                                                        // addressLocatorDesignator - Номер Будинку
            buildingData.add("null")                                                       // addressLocatorBuilding
            buildingData.add("null")                                                       // addressLocatorName
            buildingData.add(getStatus(row.getCell(BalansIndex.registrationId.index)))     // registrationStatus
            buildingData.add(row.quoted(BalansIndex.registrationId))                       // registrationId
            buildingData.add("null")                                                       // registrationDate
            buildingData.add("null")                                                       // constructionReadiness
            buildingData.add(row.quoted(BalansIndex.condition))                            // condition - Стан об'єкту
            buildingData.add("null")                                                       // utilitiesAvailable
            buildingData.add(getDt8601(row.getCell(BalansIndex.validityDate.index)))       // validityDate - Дата Актуальності
            buildingData.add(row.getCell(BalansIndex.destinationGroup.index).toString())   // destinationGroup - Група призначення

            tabBuildings[buildingId] = buildingData
        }
        return tabBuildings
    }

    fun getBalansCsv(tabBuildings: Map<String, List<String>>): String {
        logger.info("tabBuildings.size = ${tabBuildings.size}")
        val listNot634 = tabBuildings.values
            .filterNot { is634(it[BuildingIndex.destinationGroup.index]) }
            .map { it.slice(0..BuildingIndex.validityDate.index) }
        logger.info("listNot634.size = ${listNot634.size}")
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
        "addressThoroughfare",
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
