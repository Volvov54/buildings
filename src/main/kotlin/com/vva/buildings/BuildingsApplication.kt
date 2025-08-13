package com.vva.buildings

import com.vva.buildings.Balans.getBalansCsv
import com.vva.buildings.Balans.getTabBalans
import com.vva.buildings.FreeSpace.createFreeSpaceTabs
import com.vva.buildings.FreeSpace.getBuldins2Csv
import com.vva.buildings.FreeSpace.getProzorroCsv
import com.vva.buildings.ServiceXlsx.getWorkbook
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

@SpringBootApplication
class BuildingsApplication : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(BuildingsApplication::class.java)

    val pathInputBalans = "data/input/Баланс.xlsx"
    val pathInputFreeSpace = "data/input/ВільніПлощі.xlsx"
    val pathInputOrenda = "data/input/Оренда.xlsx"
    val pathOutputBuildings =
        FileSystems.getDefault().getPath("data/output/buildings.csv")
    val pathOutputBuildings2 =
        FileSystems.getDefault().getPath(
            "data/output/buildingsRentable.csv")
    val pathOutputProzorro =
        FileSystems.getDefault().getPath(
            "data/output/listProzorroSales_buildingsRentable.csv")
    val pathOutputList =
        FileSystems.getDefault().getPath("data/output/list.csv")
    val pathOutputListProzorroSales =
        FileSystems.getDefault().getPath(
            "data/output/listProzorroSales_rented.csv")

    override fun run(vararg args: String?) {
        // Initialization logic can be added here if needed
        println("Buildings application started successfully!")
        val workbookBalans = getWorkbook(pathInputBalans)
        val sheetBalans = workbookBalans.getSheetAt(0)
        val tabBuildings = getTabBalans(sheetBalans)
        logger.info("Total buildings in tab: ${tabBuildings.size}")

//        val list634 = tabBuildings.values.filter { b ->
//            is634(b[BuildingIndex.destinationGroup.index].toString())
//        }
//        println("Total buildings with 634 code: ${list634.size}")

        val balansCsv = getBalansCsv(tabBuildings)
        saveCsvFile(balansCsv, pathOutputBuildings)

        val workbookFreeSpace = getWorkbook(pathInputFreeSpace)
        val sheetFreeSpace = workbookFreeSpace.getSheetAt(0)
        createFreeSpaceTabs(tabBuildings, sheetFreeSpace)

        val prozorroCsv = getProzorroCsv()
        saveCsvFile(prozorroCsv, pathOutputProzorro)

        val buildings2Csv = getBuldins2Csv()
        saveCsvFile(buildings2Csv, pathOutputBuildings2)

        val workbookOrenda = getWorkbook(pathInputOrenda)
        val sheetOrenda = workbookOrenda.getSheetAt(0)
        Orenda.createOrendaTabs(tabBuildings, sheetOrenda)

        val listCsv = Orenda.getListCsv()
        saveCsvFile(listCsv, pathOutputList)

        val listProzorroSalesCsv = Orenda.getListProzorroSalesCsv()
        saveCsvFile(listProzorroSalesCsv, pathOutputListProzorroSales)

        println("CSV files generated successfully!")
    }

    private fun saveCsvFile(csv: String, path: Path) {
        Files.newBufferedWriter(path).use { out ->
            out.write(csv)
        }
        println("CSV file saved at: ${path.fileName}")
    }
}

fun main(args: Array<String>) {
    runApplication<BuildingsApplication>(*args)
}
