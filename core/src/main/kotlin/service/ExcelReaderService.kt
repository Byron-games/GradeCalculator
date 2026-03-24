package service

import model.Student
import java.io.File

/**
 * API for reading student data from an Excel (.xlsx / .xls) file.
 *
 * The expected spreadsheet format:
 * - Row 1  : Header row (any text – skipped automatically)
 * - Column A: Student name  (String)
 * - Column B: Student marks (Numeric, 0–100)
 */
interface ExcelReaderService {

    /**
     * Parses all student rows from the first sheet of [file].
     *
     * @param file A valid .xlsx or .xls file.
     * @return An ordered list of [Student] objects parsed from the file.
     * @throws IllegalArgumentException if the file format is invalid.
     * @throws java.io.IOException      if the file cannot be read.
     */
    fun readStudents(file: File): List<Student>

    /**
     * Returns `true` if [file] has a supported Excel extension (.xlsx or .xls).
     */
    fun isSupportedFile(file: File): Boolean =
        file.extension.lowercase() in listOf("xlsx", "xls")
}
