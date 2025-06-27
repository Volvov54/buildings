package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.isNotKyiv
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet

object FreeSpace {
    val tabProzoro = mutableListOf<List<String>>()
    val tabBuildings2 = mutableListOf<List<String>>()

    fun createFreeSpaceTabs(tabBuildings: Map<String, List<String>>, sheet: XSSFSheet) {
        tabProzoro.clear()
        tabBuildings2.clear()
        val rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 3) continue // Skip header row

            val cellIdSpace = row.getCell(FreeSpaceIndex.idSpace.ordinal) // Реєстра-ційний №
            if (cellIdSpace.cellType != CellType.NUMERIC) continue // Skip empty rows
            val idSpace = formatToId(cellIdSpace) // ID вільного простору
            val idBuilding = formatToId(row.getCell(FreeSpaceIndex.buildingId.ordinal)) // ID об'єкту

            val cellEtcCode = row.getCell(FreeSpaceIndex.etcCode.ordinal) // Унікальний код обєкту у ЕТС Прозорро-продажі

            if (cellEtcCode.cellType == CellType.STRING) {
                val etcCode = getEtcCode(cellEtcCode.toString().trim())
                val url = getUrl(etcCode)
                val title = getTitleBuilding(tabBuildings, idBuilding)

                val data = mutableListOf<String>()
                data.add(idSpace) // ocid - Унікальний код обєкту у ЕТС Прозорро-продажі
                data.add(title)   // title - Назва об'єкту
                data.add(url)     // url - Унікальний код обєкту у ЕТС Прозорро-продажі

                tabProzoro.add(data)
            } else {
                val building = tabBuildings[idBuilding]
                if (building == null) {
                    println("Building with ID $idBuilding not found in tabBuildings")
                    continue
                }

                val data = mutableListOf<String>()
                data.add("$idSpace-$idBuilding") // buildingId
                data.add(building[BuildingIndex.isPartOf.ordinal]) // isPartOf
                data.add(building[BuildingIndex.title.ordinal]) // buildingTitle
                data.add(building[BuildingIndex.kind.ordinal]) // kind
                data.add(building[BuildingIndex.type.ordinal]) // type
                data.add(building[BuildingIndex.description.ordinal]) // description
                data.add(building[BuildingIndex.ownerName.ordinal]) // ownercustodianName
                data.add(building[BuildingIndex.ownerId.ordinal]) // ownercustodianId
                data.add(building[BuildingIndex.balanceHolderName.ordinal]) // balanceHolderName
                data.add(building[BuildingIndex.balanceHolderId.ordinal]) // balanceHolderId
                data.add(building[BuildingIndex.userName.ordinal]) // userName
                data.add(building[BuildingIndex.userId.ordinal]) // userId
                data.add(building[BuildingIndex.dk018classId.ordinal]) // dk018classId
                data.add(building[BuildingIndex.dk018classDescription.ordinal]) // dk018classDescription
                data.add(building[BuildingIndex.unitName.ordinal]) // unitName
                data.add(building[BuildingIndex.area.ordinal]) // buildingArea
                data.add(building[BuildingIndex.CATUTTC.ordinal]) // CATUTTC
                data.add(building[BuildingIndex.addressPostCode.ordinal]) // addressPostCode
                data.add(building[BuildingIndex.addressAdminUnitL1.ordinal]) // addressAdminUnitL1
                data.add(building[BuildingIndex.addressAdminUnitL2.ordinal]) // addressAdminUnitL2
                data.add(building[BuildingIndex.addressAdminUnitL3.ordinal]) // addressAdminUnitL3
                data.add(building[BuildingIndex.addressAdminUnitL4.ordinal]) // addressAdminUnitL4
                data.add(building[BuildingIndex.addressPostName.ordinal]) // addressPostName
                data.add(building[BuildingIndex.addressPostDistrict.ordinal]) // addressPostDistrict
                data.add(building[BuildingIndex.addressPostStreet.ordinal]) // addressPostStreet
                data.add(building[BuildingIndex.addressLocatorDesignator.ordinal]) // addressLocatorDesignator
                data.add(building[BuildingIndex.addressLocatorBuilding.ordinal]) // addressLocatorBuilding
                data.add(building[BuildingIndex.addressLocatorName.ordinal]) // addressLocatorName
                data.add(building[BuildingIndex.registrationStatus.ordinal]) // registrationStatus
                data.add(building[BuildingIndex.registrationId.ordinal]) // registrationId
                data.add(building[BuildingIndex.registrationDate.ordinal]) // registrationDate
                data.add(building[BuildingIndex.constructionReadiness.ordinal]) // constructionReadiness
                data.add(building[BuildingIndex.condition.ordinal]) // condition
                data.add(building[BuildingIndex.utilitiesAvailable.ordinal]) // utilitiesAvailable
                data.add(building[BuildingIndex.validityDate.ordinal]) // validityDate

                tabBuildings2.add(data)
            }
        }
    }

    private fun getTitleBuilding(
        tabBuildings: Map<String, List<String>>,
        idBuilding: String
    ): String {
        return if (tabBuildings.containsKey(idBuilding)) {
            tabBuildings[idBuilding]?.get(BuildingIndex.title.ordinal) ?: "Невідомо" // Назва об'єкту
        } else {
            "Невідомо"
        }
    }

    private fun getEtcCode(code: String): String {
        if (code.startsWith("http")) {
            val parts = code.split("/")
            if (parts.last().length < 2) {
                return parts.get(parts.size - 2)
            }
            return "${parts.last()}" // Extract the last part of the URL
        }
        else return code
    }

    private fun getUrl(etcCode: String): String {
        when (etcCode.slice(0..1)) {
            "LL" -> return "https://prozorro.sale/auction/$etcCode"
            "UA" -> return "https://prozorro.sale/auction/$etcCode"
            "RG" -> return "https://prozorro.sale/planning/$etcCode"
            else -> return etcCode
        }
    }

    fun getProzorroCsv(): String =
        Utils.getCsvString(headerProzorro, tabProzoro)

    fun getBuldins2Csv(): String =
        Utils.getCsvString(headerBuildings2, tabBuildings2)


    val headerProzorro: Array<String> = arrayOf(
        "ocid",
        "title",
        "url"
    )

    val headerBuildings2: Array<String> = arrayOf(
        "buildingId",
        "isPartOf",
        "buildingTitle",
        "kind",
        "type",
        "description",
        "ownercustodianName",
        "ownercustodianId",
        "balanceHolderName",
        "balanceHolderId",
        "userName",
        "userId",
        "dk018classId",
        "dk018classDescription",
        "unitName",
        "buildingArea",
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