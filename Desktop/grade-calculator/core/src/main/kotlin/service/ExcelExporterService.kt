package service

import model.ProcessingReport
import java.io.File

/**
 * API for exporting a [ProcessingReport] to an Excel (.xlsx) workbook.
 */
interface ExcelExporterService {

    /**
     * Writes the full [report] to [destination] as an Excel workbook.
     *
     * The output workbook will contain:
     * - **Sheet 1 – Results**: one row per student (Name | Marks | Grade | GPA | Description)
     * - **Sheet 2 – Summary**: class statistics (total, pass, fail, averages)
     *
     * @param report      The processed class data to export.
     * @param destination Target `.xlsx` file path (created or overwritten).
     */
    fun export(report: ProcessingReport, destination: File)
}
