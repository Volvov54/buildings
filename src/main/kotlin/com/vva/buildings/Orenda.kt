package com.vva.buildings

import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getCurrencyValue
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getEtcCode
import com.vva.buildings.Utils.getNotPrivate
import com.vva.buildings.Utils.getQuotationString
import com.vva.buildings.Utils.getTitleBuilding
import com.vva.buildings.Utils.getUrl
import com.vva.buildings.Utils.is634m
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class OrendaData(
    val list: List<List<String>>,
    val prozorro: List<List<String>>
)

object Orenda {
    val logger: Logger = LoggerFactory.getLogger(Orenda::class.java)

    fun createOrendaTabs(
        tabBuildings: Map<String, List<String>>,
        sheet: XSSFSheet
    ): OrendaData {
        logger.info("Start creating Orenda tabs")
        val tabList = mutableListOf<List<String>>()
        val tabListProzorro = mutableListOf<List<String>>()
        var o634Count = 0
        var countValidityDateEmpty = 0
        var countValidityDateBefore2020 = 0

        val rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val cellIdOrenda = row.getCell(OrendaIndex.id.index)
            if (cellIdOrenda == null) continue
            if (cellIdOrenda.cellType != CellType.NUMERIC) continue
            val idOrenda = formatToId(cellIdOrenda) // ID договору оренди

            val validityDate = getDt8601(row.getCell(OrendaIndex.validityDate.index))
            if (validityDate == "null") {
                logger.warn("Пропускаємо рядок ${row.rowNum}, idOrenda: $idOrenda - відсутня дата актуальності")
                countValidityDateEmpty++
                continue
            }
            if (validityDate < "2020-01-01") {
                logger.warn("Пропускаємо рядок ${row.rowNum}, idOrenda: $idOrenda - дата актуальності $validityDate раніше 2020-01-01")
                countValidityDateBefore2020++
                continue
            }

            val cellContractStatus = row.getCell(OrendaIndex.contractStatus.index)
            val contractStatus = cellContractStatus.toString().trim()
            if (contractStatus != "Договір діє") continue

            val idBuildingCell = row.getCell(OrendaIndex.idBuilding.index) // cell ID об'єкту
            val idBuildingArray = idBuildingCell.toString()
            if (idBuildingArray.isBlank()) {
                logger.warn("ID об'єкту порожній у рядку ${row.rowNum}, idOrenda: $idOrenda")
                continue
            }

            val idBuildings = idBuildingArray.split(";").map { it.trim() }
            val idBuilding = idBuildings[0] // Беремо перший ID об'єкту

            val building = tabBuildings[idBuilding]
            if (building == null) {
                logger.warn("Building with ID ${idBuildings[0]} not found in tabBuildings, idOrenda: $idOrenda")
                continue
            }
            if (is634m(tabBuildings, idBuildings)) {
                o634Count++
                continue
            }

            val cellEtcCode =
                row.getCell(OrendaIndex.etcCode.index) // Унікальний код обєкту у ЕТС Прозорро-продажі

            if (cellEtcCode.cellType == CellType.STRING) {
                logger.warn("Etc code is STRING: $cellEtcCode")
                val etcCode = getEtcCode(cellEtcCode.toString().trim())
                val url = getUrl(etcCode)
                val title = getTitleBuilding(tabBuildings, idBuilding)

                tabListProzorro.add(listOf(etcCode, title, url))
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
                    getQuotationString(
                        getNotPrivate(
                            row.getCell(
                                OrendaIndex.contractUserId.index
                            ).toString()
                        )
                    )
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
                data.add(validityDate) // validityDate - Дата Актуальності

                tabList.add(data)
            }
        }
        if (countValidityDateEmpty > 0) {
            logger.info("Skipped $countValidityDateEmpty rows with empty validityDate")
        }
        if (countValidityDateBefore2020 > 0) {
            logger.info("Skipped $countValidityDateBefore2020 rows with validityDate before 2020-01-01")
        }
        if (o634Count > 0) {
            logger.info("Skipped $o634Count buildings with 634 code")
        }
        logger.info("Orenda tabList size: ${tabList.size} - tabListProzorro size: ${tabListProzorro.size}")
        logger.info("Total size of Orenda tabs: ${tabList.size + tabListProzorro.size}")
        logger.info("Finished creating Orenda tabs")
        return OrendaData(tabList, tabListProzorro)
    }

    fun getListCsv(data: OrendaData): String =
        Utils.getCsvString(headerList, data.list)

    fun getListProzorroSalesCsv(data: OrendaData): String =
        Utils.getCsvString(headerListProzorroSales, data.prozorro)

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
