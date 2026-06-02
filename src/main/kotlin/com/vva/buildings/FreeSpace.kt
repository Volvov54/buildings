package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getEtcCode
import com.vva.buildings.Utils.getQuotationString
import com.vva.buildings.Utils.getTitleBuilding
import com.vva.buildings.Utils.getUrl
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FreeSpace {
    val logger: Logger = LoggerFactory.getLogger(FreeSpace::class.java)

    val tabProzorro = mutableListOf<List<String>>()
    val tabBuildings2 = mutableListOf<List<String>>()

    fun createFreeSpaceTabs(tabBuildings: Map<String, List<String>>, sheet: XSSFSheet) {
        tabProzorro.clear()
        tabBuildings2.clear()
        val rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 3) continue // Skip header row

            val cellIdSpace = row.getCell(FreeSpaceIndex.idSpace.index) // Реєстра-ційний №
            if (cellIdSpace.cellType != CellType.NUMERIC) continue // Skip empty rows
            val idSpace = formatToId(cellIdSpace) // ID вільного простору

            val idBuilding = formatToId(
                row.getCell(
                    FreeSpaceIndex.buildingId.index
                )
            ) // ID об'єкту
            val building = tabBuildings[idBuilding]
            if (building == null) {
                logger.info("Building with ID $idBuilding not found in tabBuildings")
                continue
            }

            val cellEtcCode = row.getCell(
                FreeSpaceIndex.etcCode.index
            ) // Унікальний код обєкту у ЕТС Прозорро-продажі

            if (cellEtcCode.cellType == CellType.STRING) {
                val etcCode = getEtcCode(cellEtcCode.toString().trim())
                val url = getUrl(etcCode)
                val title = getTitleBuilding(tabBuildings, idBuilding)

                val data = mutableListOf<String>()
                data.add(idSpace) // ocid - Унікальний код обєкту у ЕТС Прозорро-продажі
                data.add(title)   // title - Назва об'єкту
                data.add(url)     // url - Унікальний код обєкту у ЕТС Прозорро-продажі

                tabProzorro.add(data)
            } else {
                val data = mutableListOf<String>()
                data.add("$idSpace-$idBuilding") // buildingId
                data.add(building[BuildingIndex.isPartOf.index]) // isPartOf
                data.add(building[BuildingIndex.title.index]) // buildingTitle
                data.add(building[BuildingIndex.kind.index]) // kind
                data.add(building[BuildingIndex.type.index]) // type
                data.add(building[BuildingIndex.description.index]) // description
                data.add(building[BuildingIndex.ownerName.index]) // ownercustodianName
                data.add(building[BuildingIndex.ownerId.index]) // ownercustodianId
                data.add(building[BuildingIndex.balanceHolderName.index]) // balanceHolderName
                data.add(building[BuildingIndex.balanceHolderId.index]) // balanceHolderId
                data.add(building[BuildingIndex.userName.index]) // userName
                data.add(building[BuildingIndex.userId.index]) // userId
                data.add(building[BuildingIndex.dk018classId.index]) // dk018classId
                data.add(building[BuildingIndex.dk018classDescription.index]) // dk018classDescription
                data.add(building[BuildingIndex.unitName.index]) // unitName
                data.add(
                    setQuotation(row.getCell(FreeSpaceIndex.area.index))
                ) // buildingArea
                data.add(building[BuildingIndex.CATUTTC.index]) // CATUTTC
                data.add(building[BuildingIndex.addressPostCode.index]) // addressPostCode
                data.add(building[BuildingIndex.addressAdminUnitL1.index]) // addressAdminUnitL1
                data.add(building[BuildingIndex.addressAdminUnitL2.index]) // addressAdminUnitL2
                data.add(building[BuildingIndex.addressAdminUnitL3.index]) // addressAdminUnitL3
                data.add(building[BuildingIndex.addressAdminUnitL4.index]) // addressAdminUnitL4
                data.add(building[BuildingIndex.addressPostName.index]) // addressPostName
                data.add(building[BuildingIndex.addressPostDistrict.index]) // addressPostDistrict
                data.add(building[BuildingIndex.addressPostStreet.index]) // addressPostStreet
                data.add(
                    setQuotation(row.getCell(FreeSpaceIndex.addressLocatorDesignator.index))
                ) // addressLocatorDesignator
                data.add(building[BuildingIndex.addressLocatorBuilding.index]) // addressLocatorBuilding
                data.add(building[BuildingIndex.addressLocatorName.index]) // addressLocatorName
                data.add(building[BuildingIndex.registrationStatus.index]) // registrationStatus
                data.add(building[BuildingIndex.registrationId.index]) // registrationId
                data.add(building[BuildingIndex.registrationDate.index]) // registrationDate
                data.add(building[BuildingIndex.constructionReadiness.index]) // constructionReadiness
                data.add(building[BuildingIndex.condition.index]) // condition
                data.add(building[BuildingIndex.utilitiesAvailable.index]) // utilitiesAvailable
                data.add(building[BuildingIndex.validityDate.index]) // validityDate
                data.add(
                    getValueBool(
                        row,
                        FreeSpaceIndex.utilitiesAvailableWaterSupply.index
                    )
                ) // utilitiesAvailableWaterSupply
                data.add(
                    getValueBool(
                        row,
                        FreeSpaceIndex.utilitiesAvailableHeatingSupply.index
                    )
                ) // utilitiesAvailableHeatingSupply
                data.add(
                    getValueStr(
                        row,
                        FreeSpaceIndex.utilitiesAvailableElectricNetwork.index
                    )
                ) // utilitiesAvailableElectricNetwork
                data.add(
                    getValueBool(
                        row,
                        FreeSpaceIndex.utilitiesAvailableGasSupply.index
                    )
                ) // utilitiesAvailableGasSupply

                tabBuildings2.add(data)
            }
        }
        logger.info("Free spaces prozorro: ${tabProzorro.size} - buildings2: ${tabBuildings2.size}")
        logger.info("Total free spaces buildings: ${tabBuildings2.size+tabProzorro.size}")
    }

    private fun getValueStr(row: Row, index: Int): String {
        val s = row.getCell(index).toString().trim()
        return getQuotationString(s)
    }

    private fun getValueBool(row: Row, index: Int): String {
        val s = row.getCell(index).toString().trim().lowercase()
        return if (s.isBlank() || s == "ні") "false" else "true"
    }

    fun getProzorroCsv(): String =
        Utils.getCsvString(headerProzorro, tabProzorro)

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
        "utilitiesAvailableWaterSupply",
        "utilitiesAvailableHeatingSupply",
        "utilitiesAvailableElectricNetwork",
        "utilitiesAvailableGasSupply"
    )
}