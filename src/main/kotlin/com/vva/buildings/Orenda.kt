package com.vva.buildings

import com.vva.buildings.FreeSpace.headerProzorro
import com.vva.buildings.FreeSpace.tabProzoro
import com.vva.buildings.Utils.formatToId
import com.vva.buildings.Utils.getDt8601
import com.vva.buildings.Utils.getEtcCode
import com.vva.buildings.Utils.getTitleBuilding
import com.vva.buildings.Utils.getUrl
import com.vva.buildings.Utils.setQuotation
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet

object Orenda {
    val tabListProzoro = mutableListOf<List<String>>()
    val tabList = mutableListOf<List<String>>()

    fun createOrendaTabs(
        tabBuildings: Map<String, List<String>>,
        sheet: XSSFSheet
    ) {
        tabListProzoro.clear()
        tabList.clear()

        val rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            if (row.rowNum < 2) continue // Skip header row

            val cellIdOrenda = row.getCell(OrendaIndex.id.index)
            if (cellIdOrenda == null) continue
            if (cellIdOrenda.cellType != CellType.NUMERIC) continue
//            val idOrenda = formatToId(cellIdOrenda) // ID договору оренди
//            val idBuilding = formatToId(row.getCell(OrendaIndex.idBuilding.index)) // ID об'єкту
            val idBuilding = "45869" // ID об'єкту
            val cellEtcCode =
                row.getCell(OrendaIndex.etcCode.index) // Унікальний код обєкту у ЕТС Прозорро-продажі
            if (cellEtcCode.cellType == CellType.STRING) {
                val etcCode = getEtcCode(cellEtcCode.toString().trim())
                val url = getUrl(etcCode)
                val title = getTitleBuilding(tabBuildings, idBuilding)

                val data = mutableListOf<String>()
//                data.add(idOrenda) // ocid - Унікальний код обєкту у ЕТС Прозорро-продажі
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
//                data.add(idOrenda) // id - ID договору оренди
                data.add("4444")
                data.add(building[BuildingIndex.title.ordinal]) // title - Назва об'єкту
                data.add(building[BuildingIndex.description.ordinal]) // description - Призначення
                data.add(building[BuildingIndex.CATUTTC.ordinal]) // CATUTTC - Код територіальної громади
                data.add(building[BuildingIndex.addressPostCode.ordinal]) // addressPostCode - Поштовий індекс
                data.add(
                    building[
                        BuildingIndex.addressAdminUnitL1.ordinal]
                ) // addressAdminUnitL1 - Адміністративна одиниця 1 рівня
                data.add(
                    building[
                        BuildingIndex.addressAdminUnitL2.ordinal]
                ) // addressAdminUnitL2 - Адміністративна одиниця 2 рівня
                data.add(
                    building[
                        BuildingIndex.addressAdminUnitL3.ordinal]
                ) // addressAdminUnitL3 - Адміністративна одиниця 3 рівня
                data.add(
                    building[
                        BuildingIndex.addressAdminUnitL4.ordinal]
                ) // addressAdminUnitL4 - Адміністративна одиниця 4 рівня
                data.add(
                    building[
                        BuildingIndex.addressPostName.ordinal]
                ) // addressPostName - Назва населеного пункту
                data.add(
                    building[
                        BuildingIndex.addressPostDistrict.ordinal]
                ) // addressPostDistrict - Район населеного пункту
                data.add(
                    building[
                        BuildingIndex.addressPostStreet.ordinal]
                ) // addressPostStreet - Вулиця населеного пункту
                data.add(
                    building[
                        BuildingIndex.addressLocatorDesignator.ordinal]
                ) // addressLocatorDesignator - Позначення будівлі/споруди
                data.add(
                    building[
                        BuildingIndex.addressLocatorBuilding.ordinal]
                ) // addressLocatorBuilding - Номер будівлі/споруди
                data.add(
                    building[
                        BuildingIndex.addressLocatorName.ordinal]
                ) // addressLocatorName - Назва будівлі/споруди
                data.add(
                    building[
                        BuildingIndex.unitName.ordinal]
                ) // unitName - Одиниця виміру площі
                data.add(
                    row.getCell(
                        OrendaIndex.quantity.index
                    ).toString()
                ) // quantity - Площа приміщення, що використовується, кв.м
                data.add(
                    row.getCell(
                        OrendaIndex.valuationDate.index
                    ).toString()
                ) // valuationDate - Дата, на яку проведена оцінка об'єкту
                data.add(
                    row.getCell(
                        OrendaIndex.valueAmount.index
                    ).toString()
                ) // valueAmount - Оціночна вартість приміщень за договором, грн
                data.add("UAH") // currencyCode - Код валюти
                data.add(
                    getDt8601(
                        row.getCell(
                            OrendaIndex.valuationDate.index
                        )
                    )
                ) // valuationDate - Дата, на яку проведена оцінка об'єкту
                data.add(
                    row.getCell(
                        OrendaIndex.contractNumber.index
                    ).toString()
                ) // contractNumber - Номер Договору Оренди
                data.add(
                    getDt8601(
                        row.getCell(
                            OrendaIndex.contractDateSigned.index
                        )
                    )
                ) // contractDateSigned - Дата укладання договору
                data.add("null") // contractUrl - URL договору (не вказано в даних)
                data.add(
                    row.getCell(
                        OrendaIndex.contractStatus.index
                    ).toString()
                ) // contractStatus - Стан договору
                data.add("null") // contractPurpose - Цільове призначення (не вказано в даних)
                data.add("null") // contractRentalRate - Орендна ставка (не вказано в даних)
                data.add(
                    row.getCell(
                        OrendaIndex.contractCustodianName.index
                    ).toString()
                ) // contractCustodianName - Балансоутримувач - Повна Назва
                data.add(
                    row.getCell(
                        OrendaIndex.contractCustodianId.index
                    ).toString()
                ) // contractCustodianId - Балансоутримувач - Код ЄДРПОУ
//                println("contractUserName: ${row.getCell(OrendaIndex.contractUserName.index)}")
                data.add(
                    row.getCell(
                        OrendaIndex.contractUserName.index
                    ).toString()
                ) // contractUserName - Орендар - Повна Назва
                data.add(
                    row.getCell(
                        OrendaIndex.contractUserId.index
                    ).toString()
                ) // contractUserId - Орендар - Код ЄДРПОУ
                data.add(
                    getDt8601(
                        row.getCell(
                            OrendaIndex.contractPeriodStartDate.index
                        )
                    )
                ) // contractPeriodStartDate - Початок оренди
                data.add(
                    getDt8601(
                        row.getCell(
                            OrendaIndex.contractPeriodEndDate.index
                        )
                    )
                ) // contractPeriodEndDate - Закінченя оренди
                data.add("null") // contractPeriodMaxExtentDate - Максимальна дата оренди (не вказано в даних)
                data.add("null") // contractSchedule - Графік оренди (не вказано в даних)
                data.add("Місяць") // contractValuePeriod - Період вартості
                data.add(
                    row.getCell(
                        OrendaIndex.contractValueAmount.index
                    ).toString()
                ) // contractValueAmount - Сума вартості договору
                data.add("null") // contractValueDescription - Опис вартості договору (не вказано в даних)

                tabList.add(data)
            }
        }
    }

    fun getListCsv() =
        Utils.getCsvString(headerList, tabList)

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
    )
}