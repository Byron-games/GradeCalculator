package impl

import model.ProcessingReport
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import service.ExcelExporterService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Apache POI–based implementation of ExcelExporterService.
 *
 * Produces:
 *  - Results sheet
 *  - Summary sheet
 */
class ExcelExporterServiceImpl : ExcelExporterService {

    override fun export(report: ProcessingReport, destination: File) {
        XSSFWorkbook().use { workbook ->
            buildResultsSheet(workbook, report)
            buildSummarySheet(workbook, report)
            destination.outputStream().use { workbook.write(it) }
        }
    }

    // ───────────────── Sheets ─────────────────

    private fun buildResultsSheet(workbook: XSSFWorkbook, report: ProcessingReport) {
        val sheet = workbook.createSheet("Results")
        val styles = StyleBundle(workbook)

        // Title
        sheet.createRow(0).also { row ->
            row.heightInPoints = 28f
            row.createCell(0).apply {
                setCellValue("Student Grade Report – Generated ${timestamp()}")
                cellStyle = styles.title
            }
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 4))
        }

        // Headers
        val headers = listOf("Student Name", "Marks (%)", "Grade", "GPA (4.0)", "Remarks")
        sheet.createRow(1).also { row ->
            headers.forEachIndexed { col, label ->
                row.createCell(col).apply {
                    setCellValue(label)
                    cellStyle = styles.header
                }
            }
        }

        // Data rows
        report.results.forEachIndexed { index, result ->
            sheet.createRow(index + 2).also { row ->
                val even = index % 2 == 0

                listOf(
                    result.name        to styles.dataText(even),
                    result.marks       to styles.dataNumber(even),
                    result.grade       to styles.dataGrade(even, result.grade),
                    result.gpa         to styles.dataNumber(even),
                    result.description to styles.dataText(even)
                ).forEachIndexed { col, (value, style) ->
                    row.createCell(col).apply {
                        when (value) {
                            is String -> setCellValue(value)
                            is Double -> setCellValue(value)
                            else -> setCellValue(value.toString())
                        }
                        cellStyle = style
                    }
                }
            }
        }

        (0..4).forEach { sheet.autoSizeColumn(it) }
        sheet.setColumnWidth(0, maxOf(sheet.getColumnWidth(0), 6000))
    }

    private fun buildSummarySheet(workbook: XSSFWorkbook, report: ProcessingReport) {
        val sheet = workbook.createSheet("Summary")
        val styles = StyleBundle(workbook)

        fun addRow(rowIdx: Int, label: String, value: String) {
            sheet.createRow(rowIdx).also { row ->
                row.createCell(0).apply {
                    setCellValue(label)
                    cellStyle = styles.summaryLabel
                }
                row.createCell(1).apply {
                    setCellValue(value)
                    cellStyle = styles.summaryValue
                }
            }
        }

        sheet.createRow(0).also { row ->
            row.heightInPoints = 24f
            row.createCell(0).apply {
                setCellValue("Class Summary")
                cellStyle = styles.title
            }
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 1))
        }

        addRow(1, "Generated On", timestamp())
        addRow(2, "Total Students", report.totalStudents.toString())
        addRow(3, "Pass Count", report.passCount.toString())
        addRow(4, "Fail Count", report.failCount.toString())
        addRow(5, "Class Average GPA", "%.2f".format(report.classAvgGpa))
        addRow(6, "Highest Mark", "%.1f%%".format(report.highestMark))
        addRow(7, "Lowest Mark", "%.1f%%".format(report.lowestMark))

        sheet.setColumnWidth(0, 6000)
        sheet.setColumnWidth(1, 5000)
    }

    // ───────────────── Helpers ─────────────────

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    // ───────────────── Style Bundle ─────────────────

    private inner class StyleBundle(private val wb: XSSFWorkbook) {

        private fun style(): XSSFCellStyle =
            wb.createCellStyle() as XSSFCellStyle

        private val gradeColors = mapOf(
            "A+" to IndexedColors.LIGHT_GREEN.index,
            "A"  to IndexedColors.LIGHT_GREEN.index,
            "A-" to IndexedColors.LIGHT_GREEN.index,
            "B+" to IndexedColors.LIGHT_TURQUOISE.index,
            "B"  to IndexedColors.LIGHT_TURQUOISE.index,
            "B-" to IndexedColors.LIGHT_TURQUOISE.index,
            "C+" to IndexedColors.YELLOW.index,
            "C"  to IndexedColors.YELLOW.index,
            "C-" to IndexedColors.YELLOW.index,
            "D+" to IndexedColors.LIGHT_ORANGE.index,
            "D"  to IndexedColors.LIGHT_ORANGE.index,
            "F"  to IndexedColors.ROSE.index
        )

        val title: XSSFCellStyle = style().apply {
            setFont(wb.createFont().apply {
                bold = true
                fontHeightInPoints = 13
                color = IndexedColors.WHITE.index
            })
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        val header: XSSFCellStyle = style().apply {
            setFont(wb.createFont().apply {
                bold = true
                fontHeightInPoints = 11
            })
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.MEDIUM
        }

        val summaryLabel: XSSFCellStyle = style().apply {
            setFont(wb.createFont().apply { bold = true })
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val summaryValue: XSSFCellStyle = style().apply {
            alignment = HorizontalAlignment.LEFT
        }

        fun dataText(even: Boolean): XSSFCellStyle = style().apply {
            fillForegroundColor =
                if (even) IndexedColors.WHITE.index
                else IndexedColors.GREY_25_PERCENT.index

            if (!even) fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        fun dataNumber(even: Boolean): XSSFCellStyle =
            dataText(even).apply {
                alignment = HorizontalAlignment.CENTER
                dataFormat = wb.createDataFormat().getFormat("0.0#")
            }

        fun dataGrade(even: Boolean, grade: String): XSSFCellStyle =
            dataText(even).apply {
                alignment = HorizontalAlignment.CENTER
                fillForegroundColor =
                    gradeColors[grade] ?: IndexedColors.WHITE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(wb.createFont().apply { bold = true })
            }
    }
}