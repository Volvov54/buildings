package com.vva.buildings

import com.vva.buildings.Balans.getBalansCsv
import com.vva.buildings.Balans.getTabBalans
import com.vva.buildings.FreeSpace.createFreeSpaceTabs
import com.vva.buildings.FreeSpace.getBuildingsCsv
import com.vva.buildings.FreeSpace.getProzorroCsv
import com.vva.buildings.ServiceXlsx.getWorkbook
import com.vva.buildings.ui.MainFrame
import org.apache.poi.xssf.usermodel.XSSFSheet
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
        val sheetBalans = loadSheet(pathInputBalans)
        val tabBuildings = getTabBalans(sheetBalans)
        logger.info("Всього будівель у таблиці: ${tabBuildings.size}")
        saveCsvFile(getBalansCsv(tabBuildings), pathOutputBuildings)

        val sheetFreeSpace = loadSheet(pathInputFreeSpace)
        val freeSpaceData = createFreeSpaceTabs(tabBuildings, sheetFreeSpace)
        saveCsvFile(getProzorroCsv(freeSpaceData), pathOutputProzorro)
        saveCsvFile(getBuildingsCsv(freeSpaceData), pathOutputBuildings2)

        val sheetOrenda = loadSheet(pathInputOrenda)
        val orendaData = Orenda.createOrendaTabs(tabBuildings, sheetOrenda)
        logger.info("Кількість записів оренди: ${orendaData.list.size}")
        saveCsvFile(Orenda.getListCsv(orendaData), pathOutputList)
        saveCsvFile(Orenda.getListProzorroSalesCsv(orendaData), pathOutputListProzorroSales)

        logger.info("CSV-файли успішно згенеровано!")
    }

    private fun loadSheet(path: String): XSSFSheet {
        logger.info("Завантаження $path...")
        return getWorkbook(path).getSheetAt(0)
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
