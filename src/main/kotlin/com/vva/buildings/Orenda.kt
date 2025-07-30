package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getCurrencyValue
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getEtcCode
import com.vva.buildings.Utils.getTitleBuilding
import com.vva.buildings.Utils.getUrl
import com.vva.buildings.Utils.is634m
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Orenda {
    val logger: Logger = LoggerFactory.getLogger(Orenda::class.java)

    val tabListProzzoro = mutableListOf<List<String>>()
    val tabList = mutableListOf<List<String>>()

    fun createOrendaTabs(
        tabBuildings: Map<String, List<String>>,
        sheet: XSSFSheet
    ) {
        logger.info("Start creating Orenda tabs")
        tabListProzzoro.clear()
        tabList.clear()

        val rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val cellIdOrenda = row.getCell(OrendaIndex.id.index)
            if (cellIdOrenda == null) continue
            if (cellIdOrenda.cellType != CellType.NUMERIC) continue
            val idOrenda = formatToId(cellIdOrenda) // ID договору оренди

            val idBuildingCell = row.getCell(OrendaIndex.idBuilding.index) // cell ID об'єкту
            val idBuildingArray = idBuildingCell.toString()
            if (idBuildingArray.isNullOrBlank()) {
                val msg = "ID об'єкту порожній у рядку ${row.rowNum}, idOrenda: $idOrenda"
                logger.warn(msg)
                continue
            }
            val idBuildings = idBuildingArray.split(";").map { it.trim() }
//            logger.warn("idBuildings: $idBuildings, idOrenda: $idOrenda")
            val idBuilding = idBuildings[0] // Беремо перший ID об'єкту

            val building = tabBuildings[idBuilding]
            if (building == null) {
                val msg =
                    "Building with ID ${idBuildings[0]} not found in tabBuildings, idOrenda: $idOrenda"
                logger.warn(msg)
                continue
            }
            if (is634m(tabBuildings, idBuildings)) {
//                logger.warn("Skipping building with ID $idBuildings as it is 634 code")
                continue
            }

            val cellEtcCode =
                row.getCell(OrendaIndex.etcCode.index) // Унікальний код обєкту у ЕТС Прозорро-продажі
            if (cellEtcCode.cellType == CellType.STRING) {
//                logger.warn("Etc code is STRING: $cellEtcCode")
                val etcCode = getEtcCode(cellEtcCode.toString().trim())
                val url = getUrl(etcCode)
                val title = getTitleBuilding(tabBuildings, idBuilding)

                val data = mutableListOf<String>()
                data.add(etcCode) // ocid - Унікальний код обєкту у ЕТС Прозорро-продажі
                data.add(title)   // title - Назва об'єкту
                data.add(url)     // url - Унікальний код обєкту у ЕТС Прозорро-продажі

                tabListProzzoro.add(data)
            } else {
                val data = mutableListOf<String>()
                data.add("$idOrenda-$idBuilding") // id - ID договору оренди
                data.add(building[BuildingIndex.title.index]) // title - Назва об'єкту
                data.add(building[BuildingIndex.description.index]) // description - Призначення
                data.add(building[BuildingIndex.CATUTTC.index]) // CATUTTC - Код територіальної громади
                data.add(building[BuildingIndex.addressPostCode.index]) // addressPostCode - Поштовий індекс
                data.add(
                    building[BuildingIndex.addressAdminUnitL1.index]
                ) // addressAdminUnitL1 - Адміністративна одиниця 1 рівня
                data.add(
                    building[BuildingIndex.addressAdminUnitL2.index]
                ) // addressAdminUnitL2 - Адміністративна одиниця 2 рівня
                data.add(
                    building[BuildingIndex.addressAdminUnitL3.index]
                ) // addressAdminUnitL3 - Адміністративна одиниця 3 рівня
                data.add(
                    building[BuildingIndex.addressAdminUnitL4.index]
                ) // addressAdminUnitL4 - Адміністративна одиниця 4 рівня
                data.add(
                    building[BuildingIndex.addressPostName.index]
                ) // addressPostName - Назва населеного пункту
                data.add(
                    building[BuildingIndex.addressPostDistrict.index]
                ) // addressPostDistrict - Район населеного пункту
                data.add(
                    building[BuildingIndex.addressPostStreet.index]
                ) // addressPostStreet - Вулиця населеного пункту
                data.add(
                    setQuotation(row.getCell(OrendaIndex.addressLocatorDesignator.index))
                ) // addressLocatorDesignator - Позначення будівлі/споруди
                data.add(
                    building[BuildingIndex.addressLocatorBuilding.index]
                ) // addressLocatorBuilding - Номер будівлі/споруди
                data.add(
                    building[BuildingIndex.addressLocatorName.index]
                ) // addressLocatorName - Назва будівлі/споруди
                data.add(
                    building[BuildingIndex.unitName.index]
                ) // unitName - Одиниця виміру площі
                data.add(
                    row.getCell(OrendaIndex.quantity.index).toString()
                ) // quantity - Площа приміщення, що використовується, кв.м
                data.add(
                    getCurrencyValue(row.getCell(OrendaIndex.valueAmount.index))
                ) // valueAmount - Оціночна вартість приміщень за договором, грн
                data.add("UAH") // currencyCode - Код валюти
                data.add(
                    getDt8601(row.getCell(OrendaIndex.valuationDate.index))
                ) // valuationDate - Дата, на яку проведена оцінка об'єкту
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractNumber.index))
                ) // contractNumber - Номер Договору Оренди
                data.add(
                    getDt8601(row.getCell(OrendaIndex.contractDateSigned.index))
                ) // contractDateSigned - Дата укладання договору
                data.add("null") // contractUrl - URL договору (не вказано в даних)
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractStatus.index))
                ) // contractStatus - Стан договору
                data.add("null") // contractPurpose - Цільове призначення (не вказано в даних)
                data.add("null") // contractRentalRate - Орендна ставка (не вказано в даних)
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractCustodianName.index))
                ) // contractCustodianName - Балансоутримувач - Повна Назва
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractCustodianId.index))
                ) // contractCustodianId - Балансоутримувач - Код ЄДРПОУ
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractUserName.index))
                ) // contractUserName - Орендар - Повна Назва
                data.add(
                    setQuotation(row.getCell(OrendaIndex.contractUserId.index))
                ) // contractUserId - Орендар - Код ЄДРПОУ
                data.add(
                    getDt8601(row.getCell(OrendaIndex.contractPeriodStartDate.index))
                ) // contractPeriodStartDate - Початок оренди
                data.add(
                    getDt8601(row.getCell(OrendaIndex.contractPeriodEndDate.index))
                ) // contractPeriodEndDate - Закінченя оренди
                data.add("null") // contractPeriodMaxExtentDate - Максимальна дата оренди (не вказано в даних)
                data.add("null") // contractSchedule - Графік оренди (не вказано в даних)
                data.add("Місяць") // contractValuePeriod - Період вартості
                data.add(
                    getCurrencyValue(row.getCell(OrendaIndex.contractValueAmount.index))
                ) // contractValueAmount - Сума вартості договору
                data.add("null") // contractValueDescription - Опис вартості договору (не вказано в даних)
                data.add(
                    getDt8601(row.getCell(OrendaIndex.validityDate.index))
                ) // validityDate - Дата Актуальності

                tabList.add(data)
            }
        }
        logger.info("Finished creating Orenda tabs")
    }

    fun getListCsv() =
        Utils.getCsvString(headerList, tabList)

    fun getListProzorroSalesCsv() =
        Utils.getCsvString(headerListProzorroSales, tabListProzzoro)

    val headerListProzorroSales = arrayOf(
        "ocid",
        "title",
        "url",
    )
    val headerList = arrayOf(
        "id",
        "title",
        "description",
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
        "unitName",
        "quantity",
        "valueAmount",
        "currencyCode",
        "valuationDate",
        "contractNumber",
        "contractDateSigned",
        "contractUrl",
        "contractStatus",
        "contractPurpose",
        "contractRentalRate",
        "contractCustodianName",
        "contractCustodianId",
        "contractUserName",
        "contractUserId",
        "contractPeriodStartDate",
        "contractPeriodEndDate",
        "contractPeriodMaxExtentDate",
        "contractSchedule",
        "contractValuePeriod",
        "contractValueAmount",
        "contractValueDescription",
        "validityDate",
    )
}