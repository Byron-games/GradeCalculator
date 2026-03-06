package com.example.gradecalculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var buttonPickFile: Button
    private lateinit var textViewFilePath: TextView
    private lateinit var buttonProcess: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewStatus: TextView
    private lateinit var textViewSummary: TextView   // <-- new

    private var inputFileUri: Uri? = null

    private val PICK_INPUT_FILE_REQUEST = 1
    private val CREATE_OUTPUT_FILE_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonPickFile = findViewById(R.id.buttonPickFile)
        textViewFilePath = findViewById(R.id.textViewFilePath)
        buttonProcess = findViewById(R.id.buttonProcess)
        progressBar = findViewById(R.id.progressBar)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewSummary = findViewById(R.id.textViewSummary)   // <-- new

        buttonPickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            startActivityForResult(intent, PICK_INPUT_FILE_REQUEST)
        }

        buttonProcess.setOnClickListener {
            if (inputFileUri == null) {
                Toast.makeText(this, "Please select an input file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "output.xlsx")
            }
            startActivityForResult(intent, CREATE_OUTPUT_FILE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_INPUT_FILE_REQUEST -> {
                if (resultCode == RESULT_OK && data != null) {
                    inputFileUri = data.data
                    val fileName = getFileName(inputFileUri!!)
                    textViewFilePath.text = "Selected: $fileName"
                    buttonProcess.isEnabled = true
                    textViewStatus.text = ""
                    textViewSummary.text = ""   // clear previous summary
                } else {
                    Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            CREATE_OUTPUT_FILE_REQUEST -> {
                if (resultCode == RESULT_OK && data != null) {
                    val outputUri = data.data
                    processExcelFiles(inputFileUri!!, outputUri!!)
                } else {
                    Toast.makeText(this, "Save location not chosen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String = try {
        DocumentFile.fromSingleUri(this, uri)?.name ?: uri.lastPathSegment ?: "Unknown"
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Unknown"
    }

    // ================== FUNCTIONAL REFACTOR ==================
    private fun processExcelFiles(inputUri: Uri, outputUri: Uri) {
        progressBar.visibility = ProgressBar.VISIBLE
        textViewStatus.text = "Processing..."
        textViewSummary.text = ""
        buttonPickFile.isEnabled = false
        buttonProcess.isEnabled = false

        Thread {
            try {
                contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)

                    // 1. Extract data using functional pipeline (sequence + map/filter)
                    val rawPairs = sheet.asSequence()
                        .drop(1)   // skip header row
                        .filter { row -> row.physicalNumberOfCells >= 2 }
                        .mapNotNull { row ->
                            val nameCell = row.getCell(0)
                            val marksCell = row.getCell(1)
                            if (nameCell == null || marksCell == null) null
                            else {
                                val name = nameCell.toString()
                                val marks = when {
                                    marksCell.cellType.name == "NUMERIC" -> marksCell.numericCellValue.toInt()
                                    else -> marksCell.toString().toIntOrNull() ?: 0
                                }
                                name to marks   // return a Pair
                            }
                        }
                        .toList()   // materialize the sequence

                    // 2. Process all students with the functional helper from GradeCalculator
                    val gradeResults = GradeCalculator.processAllStudents(rawPairs)

                    // 3. Write output Excel (using forEachIndexed, which is functional)
                    contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                        val outputWorkbook = XSSFWorkbook()
                        val outputSheet = outputWorkbook.createSheet("Sheet1")

                        // Header row
                        outputSheet.createRow(0).apply {
                            createCell(0).setCellValue("Name")
                            createCell(1).setCellValue("Marks")
                            createCell(2).setCellValue("Grade")
                            createCell(3).setCellValue("GPA")
                        }

                        // Data rows – functional loop
                        gradeResults.forEachIndexed { index, result ->
                            outputSheet.createRow(index + 1).apply {
                                createCell(0).setCellValue(result.name)
                                createCell(1).setCellValue(result.marks.toDouble())
                                createCell(2).setCellValue(result.grade)
                                createCell(3).setCellValue(result.gpa)
                            }
                        }

                        // Auto‑size columns – still a loop, but we can make it functional too
                        (0..3).forEach { col -> outputSheet.autoSizeColumn(col) }

                        outputWorkbook.write(outputStream)
                        outputWorkbook.close()
                    }
                    workbook.close()

                    // 4. Compute summary using functional operations
                    val avgGpa = gradeResults.map { it.gpa }.average()
                    val maxGpa = gradeResults.maxOfOrNull { it.gpa } ?: 0.0
                    val gradeCounts = gradeResults.groupBy { it.grade }
                        .mapValues { (_, list) -> list.size }

                    val summary = buildString {
                        appendLine("Average GPA: %.2f".format(avgGpa))
                        appendLine("Highest GPA: $maxGpa")
                        append("Grade distribution: ")
                        gradeCounts.forEach { (grade, count) ->
                            append("$grade:$count ")
                        }
                    }

                    // 5. Update UI on main thread
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        textViewStatus.text = "Success! Processed ${gradeResults.size} student(s)."
                        textViewSummary.text = summary
                        buttonPickFile.isEnabled = true
                        buttonProcess.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    textViewStatus.text = "Error: ${e.message}"
                    buttonPickFile.isEnabled = true
                    buttonProcess.isEnabled = true
                }
            }
        }.start()
    }
}