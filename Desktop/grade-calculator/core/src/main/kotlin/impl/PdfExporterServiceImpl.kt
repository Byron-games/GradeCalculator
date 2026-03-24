package impl

import model.GradeLevel
import model.ProcessingReport
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import service.PdfExporterService
import java.awt.Color
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Apache PDFBox–based implementation of [PdfExporterService].
 *
 * Renders a landscape A4 PDF with:
 * - A branded header
 * - Full results table with alternating row colours and grade colour coding
 * - A class statistics footer section
 */
class PdfExporterServiceImpl : PdfExporterService {

    // ── Fonts (lazy so they are created once per export call) ─────────────────
    private val regular  by lazy { PDType1Font(Standard14Fonts.FontName.HELVETICA) }
    private val bold     by lazy { PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD) }
    private val oblique  by lazy { PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE) }

    // ── Layout constants ─────────────────────────────────────────────────────
    private val PAGE         = PDRectangle.A4.let { PDRectangle(it.height, it.width) } // landscape
    private val MARGIN       = 40f
    private val ROW_HEIGHT   = 20f
    private val HEADER_H     = 36f
    private val COL_WIDTHS   = floatArrayOf(200f, 80f, 60f, 80f, 140f)   // Name|Marks|Grade|GPA|Remarks
    private val COL_HEADERS  = listOf("Student Name", "Marks (%)", "Grade", "GPA (4.0)", "Remarks")

    override fun export(report: ProcessingReport, destination: File) {
        PDDocument().use { doc ->
            val pages = paginateResults(report)
            pages.forEachIndexed { pageIdx, pageResults ->
                val page = PDPage(PAGE).also { doc.addPage(it) }
                PDPageContentStream(doc, page).use { cs ->
                    val top = PAGE.height - MARGIN
                    drawHeader(cs, top)
                    val tableTop = top - HEADER_H - 8f
                    drawTableHeader(cs, tableTop)
                    var y = tableTop - ROW_HEIGHT
                    pageResults.forEachIndexed { i, result ->
                        drawDataRow(cs, y, result.name, result.marks, result.grade,
                                    result.gpa, result.description, result.gradeLevel, i % 2 == 0)
                        y -= ROW_HEIGHT
                    }
                    if (pageIdx == pages.lastIndex) {
                        y -= 10f
                        drawSummary(cs, y, report)
                    }
                    drawFooter(cs, pageIdx + 1, pages.size)
                }
            }
            doc.save(destination)
        }
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private fun drawHeader(cs: PDPageContentStream, top: Float) {
        val width = PAGE.width - 2 * MARGIN
        cs.setNonStrokingColor(Color(30, 58, 138))   // dark-blue banner
        cs.addRect(MARGIN, top - HEADER_H, width, HEADER_H)
        cs.fill()

        cs.setNonStrokingColor(Color.WHITE)
        cs.beginText()
        cs.setFont(bold, 16f)
        cs.newLineAtOffset(MARGIN + 8f, top - HEADER_H + 11f)
        cs.showText("Student Grade Report")
        cs.endText()

        cs.setNonStrokingColor(Color.WHITE)
        cs.beginText()
        cs.setFont(oblique, 9f)
        val ts = "Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}"
        val tsW = oblique.getStringWidth(ts) / 1000 * 9f
        cs.newLineAtOffset(PAGE.width - MARGIN - tsW - 8f, top - HEADER_H + 11f)
        cs.showText(ts)
        cs.endText()
    }

    private fun drawTableHeader(cs: PDPageContentStream, y: Float) {
        cs.setNonStrokingColor(Color(55, 65, 81))   // dark-gray row
        cs.addRect(MARGIN, y, tableWidth(), ROW_HEIGHT)
        cs.fill()

        cs.setNonStrokingColor(Color.WHITE)
        cs.setFont(bold, 9f)
        var x = MARGIN
        COL_HEADERS.forEachIndexed { i, label ->
            cs.beginText()
            cs.newLineAtOffset(x + 4f, y + 6f)
            cs.showText(label)
            cs.endText()
            x += COL_WIDTHS[i]
        }
    }

    private fun drawDataRow(
        cs: PDPageContentStream,
        y: Float, name: String, marks: Double, grade: String,
        gpa: Double, description: String, level: GradeLevel, evenRow: Boolean
    ) {
        // Row background
        cs.setNonStrokingColor(if (evenRow) Color.WHITE else Color(243, 244, 246))
        cs.addRect(MARGIN, y, tableWidth(), ROW_HEIGHT)
        cs.fill()

        // Grade colour badge
        val badgeColor = gradeColor(level)
        val gradeX = MARGIN + COL_WIDTHS[0] + COL_WIDTHS[1]
        cs.setNonStrokingColor(badgeColor)
        cs.addRect(gradeX + 2f, y + 2f, COL_WIDTHS[2] - 4f, ROW_HEIGHT - 4f)
        cs.fill()

        // Row border (bottom line)
        cs.setStrokingColor(Color(229, 231, 235))
        cs.moveTo(MARGIN, y)
        cs.lineTo(MARGIN + tableWidth(), y)
        cs.stroke()

        // Text
        cs.setNonStrokingColor(Color(17, 24, 39))
        cs.setFont(regular, 9f)

        val cells = listOf(
            name, "%.1f".format(marks), grade, "%.2f".format(gpa), description
        )
        var x = MARGIN
        cells.forEachIndexed { i, text ->
            val textColor = if (i == 2) Color.WHITE else Color(17, 24, 39)
            cs.setNonStrokingColor(textColor)
            cs.setFont(if (i == 2) bold else regular, 9f)
            cs.beginText()
            cs.newLineAtOffset(x + 4f, y + 6f)
            cs.showText(text.take(30))   // truncate very long names
            cs.endText()
            x += COL_WIDTHS[i]
        }
    }

    private fun drawSummary(cs: PDPageContentStream, startY: Float, report: ProcessingReport) {
        var y = startY
        cs.setNonStrokingColor(Color(30, 58, 138))
        cs.setFont(bold, 11f)
        cs.beginText()
        cs.newLineAtOffset(MARGIN, y)
        cs.showText("Class Summary")
        cs.endText()

        y -= 16f
        cs.setFont(regular, 9f)
        val summaryLines = listOf(
            "Total Students: ${report.totalStudents}",
            "Passed: ${report.passCount}   |   Failed: ${report.failCount}",
            "Class Average GPA: ${"%.2f".format(report.classAvgGpa)} / 4.0",
            "Highest Mark: ${"%.1f".format(report.highestMark)}%   |   Lowest Mark: ${"%.1f".format(report.lowestMark)}%"
        )
        summaryLines.forEach { line ->
            cs.setNonStrokingColor(Color(55, 65, 81))
            cs.beginText()
            cs.newLineAtOffset(MARGIN, y)
            cs.showText(line)
            cs.endText()
            y -= 14f
        }
    }

    private fun drawFooter(cs: PDPageContentStream, current: Int, total: Int) {
        cs.setNonStrokingColor(Color(156, 163, 175))
        cs.setFont(regular, 8f)
        val text = "Page $current of $total"
        val tw = regular.getStringWidth(text) / 1000 * 8f
        cs.beginText()
        cs.newLineAtOffset((PAGE.width - tw) / 2f, 22f)
        cs.showText(text)
        cs.endText()
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun tableWidth(): Float = COL_WIDTHS.sum()

    /** Splits results across pages so rows never overflow the page height. */
    private fun paginateResults(report: ProcessingReport): List<List<model.GradeResult>> {
        val usableHeight = PAGE.height - 2 * MARGIN - HEADER_H - 8f - ROW_HEIGHT - 80f // summary space
        val rowsPerPage  = (usableHeight / ROW_HEIGHT).toInt()
        return report.results.chunked(rowsPerPage)
    }

    private fun gradeColor(level: GradeLevel): Color = when (level) {
        GradeLevel.A_PLUS, GradeLevel.A, GradeLevel.A_MINUS -> Color(22, 163, 74)    // green
        GradeLevel.B_PLUS, GradeLevel.B, GradeLevel.B_MINUS -> Color(37, 99, 235)    // blue
        GradeLevel.C_PLUS, GradeLevel.C, GradeLevel.C_MINUS -> Color(202, 138, 4)    // amber
        GradeLevel.D_PLUS, GradeLevel.D                     -> Color(234, 88, 12)    // orange
        GradeLevel.F                                         -> Color(220, 38, 38)   // red
    }
}
