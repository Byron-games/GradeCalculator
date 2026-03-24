package impl

import model.Student
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import service.ExcelReaderService
import java.io.File

/**
 * Apache POI–based implementation of [ExcelReaderService].
 *
 * Reads the first worksheet of an .xlsx or .xls file.
 * Row 0 is treated as a header and skipped automatically.
 * Blank rows are silently skipped.
 */
class ExcelReaderServiceImpl : ExcelReaderService {

    override fun readStudents(file: File): List<Student> {
        require(isSupportedFile(file)) {
            "Unsupported file type '${file.extension}'. Only .xlsx and .xls are accepted."
        }

        return file.inputStream().use { stream ->
            WorkbookFactory.create(stream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                sheet.drop(1)                        // skip header row
                    .filter { row -> !row.getCell(0).isBlankOrNull() }
                    .map { row ->
                        val name  = row.getCell(0).stringValue()
                        val marks = row.getCell(1).numericValue()
                        Student(name = name, marks = marks)
                    }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun Cell?.isBlankOrNull(): Boolean =
        this == null || cellType == CellType.BLANK || stringCellValue.isBlank()

    private fun Cell.stringValue(): String =
        when (cellType) {
            CellType.STRING  -> stringCellValue.trim()
            CellType.NUMERIC -> numericCellValue.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            }
            CellType.FORMULA -> cachedFormulaResultType.let { _ -> stringCellValue.trim() }
            else -> throw IllegalArgumentException(
                "Expected a text value in column A, but found cell type: $cellType"
            )
        }

    private fun Cell.numericValue(): Double =
        when (cellType) {
            CellType.NUMERIC -> numericCellValue
            CellType.STRING  -> stringCellValue.trim().toDoubleOrNull()
                ?: throw IllegalArgumentException(
                    "Cannot parse '${stringCellValue}' as a number in column B."
                )
            CellType.FORMULA -> numericCellValue
            else -> throw IllegalArgumentException(
                "Expected a numeric value in column B, but found cell type: $cellType"
            )
        }
}
