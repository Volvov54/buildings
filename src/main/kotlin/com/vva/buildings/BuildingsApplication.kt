package com.vva.buildings

import com.vva.buildings.Balans.getBalansCsv
import com.vva.buildings.Balans.getTabBalans
import com.vva.buildings.FreeSpace.createFreeSpaceTabs
import com.vva.buildings.FreeSpace.getBuildingsCsv
import com.vva.buildings.FreeSpace.getProzorroCsv
import com.vva.buildings.ServiceXlsx.getWorkbook
import com.vva.buildings.ui.MainFrame
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

@SpringBootApplication
class BuildingsApplication(
    @param:Value("\${buildings.input.balans}") private val pathInputBalans: String,
    @param:Value("\${buildings.input.free-space}") private val pathInputFreeSpace: String,
    @param:Value("\${buildings.input.orenda}") private val pathInputOrenda: String,
    @param:Value("\${buildings.output.buildings}") private val pathOutputBuildings: String,
    @param:Value("\${buildings.output.buildings-rentable}") private val pathOutputBuildings2: String,
    @param:Value("\${buildings.output.prozorro-buildings-rentable}") private val pathOutputProzorro: String,
    @param:Value("\${buildings.output.list}") private val pathOutputList: String,
    @param:Value("\${buildings.output.prozorro-list}") private val pathOutputListProzorroSales: String,
) {
    private val logger = LoggerFactory.getLogger(BuildingsApplication::class.java)

    fun process() {
        logger.info("Завантаження ${pathInputBalans}...")
        val workbookBalans = getWorkbook(pathInputBalans)
        val sheetBalans = workbookBalans.getSheetAt(0)
        InputValidator.validateBalans(sheetBalans)
        val tabBuildings = getTabBalans(sheetBalans)
        logger.info("Всього будівель у таблиці: ${tabBuildings.size}")

        val balansCsv = getBalansCsv(tabBuildings)
        saveCsvFile(balansCsv, pathOutputBuildings)

        logger.info("Завантаження ${pathInputFreeSpace}...")
        val workbookFreeSpace = getWorkbook(pathInputFreeSpace)
        val sheetFreeSpace = workbookFreeSpace.getSheetAt(0)
        InputValidator.validateFreeSpace(sheetFreeSpace)
        val freeSpaceData = createFreeSpaceTabs(tabBuildings, sheetFreeSpace)

        saveCsvFile(getProzorroCsv(freeSpaceData), pathOutputProzorro)
        saveCsvFile(getBuildingsCsv(freeSpaceData), pathOutputBuildings2)

        logger.info("Завантаження ${pathInputOrenda}...")
        val workbookOrenda = getWorkbook(pathInputOrenda)
        val sheetOrenda = workbookOrenda.getSheetAt(0)
        InputValidator.validateOrenda(sheetOrenda)
        val orendaData = Orenda.createOrendaTabs(tabBuildings, sheetOrenda)

        val listCsv = Orenda.getListCsv(orendaData)
        logger.info("Кількість записів оренди: ${orendaData.list.size}")
        saveCsvFile(listCsv, pathOutputList)

        saveCsvFile(Orenda.getListProzorroSalesCsv(orendaData), pathOutputListProzorroSales)

        logger.info("CSV-файли успішно згенеровано!")
    }

    private fun saveCsvFile(csv: String, path: String) {
        val target = Path.of(path)
        Files.newBufferedWriter(target).use { out ->
            out.write(csv)
        }
        logger.info("Збережено: ${target.fileName}")
    }
}

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    val context = runApplication<BuildingsApplication>(*args)
    val app = context.getBean(BuildingsApplication::class.java)
    SwingUtilities.invokeLater {
        val frame = MainFrame { app.process() }
        frame.isVisible = true
    }
}
