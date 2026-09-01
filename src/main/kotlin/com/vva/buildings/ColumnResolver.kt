package com.vva.buildings

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.slf4j.LoggerFactory

/**
 * Зіставляє логічні поля (`*Index`) з фізичними номерами колонок Excel за назвою заголовка,
 * а не за фіксованою позицією. Порядок колонок у файлі та зайві (невідомі) колонки значення
 * не мають; натомість перевіряється повнота — усі обов'язкові колонки мають бути присутні,
 * інакше кидається [IllegalStateException] (у UI — "Помилка!").
 *
 * Зіставлення суворе: назва заголовка у файлі має точно (після `trim`) збігатися з `header`
 * відповідного поля enum. Якщо портал змінить формулювання заголовка — це навмисно призведе
 * до помилки, а не до мовчазного пропуску даних.
 */
object ColumnResolver {
    private val logger = LoggerFactory.getLogger(ColumnResolver::class.java)

    fun resolveBalans(sheet: XSSFSheet): Map<BalansIndex, Int> =
        resolve(sheet, BalansIndex.entries, headerRows = 1..1, fileName = "Баланс.xlsx") { it.header }

    fun resolveFreeSpace(sheet: XSSFSheet): Map<FreeSpaceIndex, Int> =
        resolve(sheet, FreeSpaceIndex.entries, headerRows = 1..2, fileName = "ВільніПлощі.xlsx") { it.header }

    fun resolveOrenda(sheet: XSSFSheet): Map<OrendaIndex, Int> =
        resolve(sheet, OrendaIndex.entries, headerRows = 1..1, fileName = "Оренда.xlsx") { it.header }

    /**
     * @param headerRows діапазон рядків (0-based), що разом утворюють заголовок. Якщо колонка
     * має значення в кількох рядках (над-/під-заголовок), перемагає рядок з більшим індексом.
     */
    private fun <E : Enum<E>> resolve(
        sheet: XSSFSheet,
        fields: List<E>,
        headerRows: IntRange,
        fileName: String,
        headerOf: (E) -> String,
    ): Map<E, Int> {
        val headerByColumn = sortedMapOf<Int, String>()
        for (rowIndex in headerRows) {
            val row = sheet.getRow(rowIndex) ?: continue
            for (cell in row) {
                val text = cell.toString().trim()
                if (text.isNotEmpty()) headerByColumn[cell.columnIndex] = text
            }
        }
        if (headerByColumn.isEmpty()) {
            val message = "Файл $fileName: не знайдено рядок заголовків (рядки ${headerRows.first + 1}–${headerRows.last + 1})"
            logger.error(message)
            throw IllegalStateException(message)
        }

        // Перше входження (найменший номер колонки) виграє за однакових назв заголовків.
        val columnByHeader = HashMap<String, Int>()
        headerByColumn.forEach { (column, text) -> columnByHeader.putIfAbsent(text, column) }

        val resolved = LinkedHashMap<E, Int>()
        val missing = mutableListOf<String>()
        for (field in fields) {
            val column = columnByHeader[headerOf(field)]
            if (column == null) missing += "\"${headerOf(field)}\"" else resolved[field] = column
        }

        if (missing.isNotEmpty()) {
            val message = buildString {
                append("Файл $fileName: у заголовках не знайдено обов'язкові колонки:\n")
                append(missing.joinToString("\n") { "  $it" })
                append("\nЗнайдені заголовки: ")
                append(headerByColumn.values.joinToString(" | "))
            }
            logger.error(message)
            throw IllegalStateException(message)
        }

        logger.info("Файл $fileName: усі ${fields.size} колонок знайдено за назвою заголовка")
        return resolved
    }
}
