package com.vva.buildings

import com.vva.buildings.Balans.getBalansCsv
import com.vva.buildings.Balans.getTabBalans
import com.vva.buildings.ServiceXlsx.getWorkbook
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.FileSystems
import java.nio.file.Files

@SpringBootApplication
class BuildingsApplication: CommandLineRunner {
    val pathInputBalans = "Balans.xlsx"
    val pathOutputBuildings = FileSystems.getDefault().getPath("buildings.csv")

    override fun run(vararg args: String?) {
        // Initialization logic can be added here if needed
        println("Buildings application started successfully!")
        val workbook = getWorkbook(pathInputBalans)
        val sheet = workbook.getSheetAt(0)
        val tabBuildings = getTabBalans(sheet)
        val balansCsv = getBalansCsv(tabBuildings)
        Files.newBufferedWriter(pathOutputBuildings).use { out ->
            out.write(balansCsv)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BuildingsApplication>(*args)
}
