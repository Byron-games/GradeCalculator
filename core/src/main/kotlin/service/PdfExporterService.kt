package service

import model.ProcessingReport
import java.io.File

/**
 * API for exporting a [ProcessingReport] to a formatted PDF document.
 */
interface PdfExporterService {

    /**
     * Renders the full [report] as a styled PDF and writes it to [destination].
     *
     * The PDF will include:
     * - A title header with generation date.
     * - A results table: Name | Marks | Grade | GPA | Description.
     * - A summary section with class statistics.
     *
     * @param report      The processed class data to export.
     * @param destination Target `.pdf` file path (created or overwritten).
     */
    fun export(report: ProcessingReport, destination: File)
}
