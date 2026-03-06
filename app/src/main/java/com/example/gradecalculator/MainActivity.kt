package com.example.gradecalculator // use your actual package name

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

    private var inputFileUri: Uri? = null

    // Request codes for SAF intents
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

        // Pick input file
        buttonPickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
                // Optionally also allow older .xls: add "application/vnd.ms-excel"
            }
            startActivityForResult(intent, PICK_INPUT_FILE_REQUEST)
        }

        // Process button – only enabled after a file is picked
        buttonProcess.setOnClickListener {
            if (inputFileUri == null) {
                Toast.makeText(this, "Please select an input file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Launch SAF to choose where to save the output file
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
                    // Show the file name to the user
                    val fileName = getFileName(inputFileUri!!)
                    textViewFilePath.text = "Selected: $fileName"
                    buttonProcess.isEnabled = true
                    textViewStatus.text = ""
                } else {
                    Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            CREATE_OUTPUT_FILE_REQUEST -> {
                if (resultCode == RESULT_OK && data != null) {
                    val outputUri = data.data
                    // Now we have input and output URIs – run the processing
                    processExcelFiles(inputFileUri!!, outputUri!!)
                } else {
                    Toast.makeText(this, "Save location not chosen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper to get a displayable file name from a URI
    private fun getFileName(uri: Uri): String {
        return try {
            val docFile = DocumentFile.fromSingleUri(this, uri)
            docFile?.name ?: uri.lastPathSegment ?: "Unknown"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Unknown"
        }
    }

    // This function does the actual Excel processing (similar to your original code)
    private fun processExcelFiles(inputUri: Uri, outputUri: Uri) {
        // Show progress bar and disable buttons
        progressBar.visibility = ProgressBar.VISIBLE
        textViewStatus.text = "Processing..."
        buttonPickFile.isEnabled = false
        buttonProcess.isEnabled = false

        // Run in background thread to avoid blocking UI
        Thread {
            try {
                // Read input Excel file
                contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)

                    val results = mutableListOf<GradeResult>()
                    var isFirstRow = true

                    for (row in sheet) {
                        if (isFirstRow) {
                            isFirstRow = false
                            continue
                        }
                        if (row.physicalNumberOfCells >= 2) {
                            val nameCell = row.getCell(0)
                            val marksCell = row.getCell(1)

                            if (nameCell != null && marksCell != null) {
                                val name = nameCell.toString()
                                val marks = when {
                                    marksCell.cellType.name == "NUMERIC" -> marksCell.numericCellValue.toInt()
                                    else -> marksCell.toString().toIntOrNull() ?: 0
                                }
                                val result = GradeCalculator.processStudent(name, marks)
                                results.add(result)
                            }
                        }
                    }
                    workbook.close()

                    // Create output Excel file
                    contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                        val outputWorkbook = XSSFWorkbook()
                        val outputSheet = outputWorkbook.createSheet("Sheet1")

                        // Header
                        val headerRow = outputSheet.createRow(0)
                        headerRow.createCell(0).setCellValue("Name")
                        headerRow.createCell(1).setCellValue("Marks")
                        headerRow.createCell(2).setCellValue("Grade")
                        headerRow.createCell(3).setCellValue("GPA")

                        // Data rows
                        results.forEachIndexed { index, result ->
                            val dataRow = outputSheet.createRow(index + 1)
                            dataRow.createCell(0).setCellValue(result.name)
                            dataRow.createCell(1).setCellValue(result.marks.toDouble())
                            dataRow.createCell(2).setCellValue(result.grade)
                            dataRow.createCell(3).setCellValue(result.gpa)
                        }

                        // Auto-size columns
                        for (i in 0..3) {
                            outputSheet.autoSizeColumn(i)
                        }

                        outputWorkbook.write(outputStream)
                        outputWorkbook.close()
                    }

                    // Update UI on main thread
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        textViewStatus.text = "Success! Processed ${results.size} student(s)."
                        buttonPickFile.isEnabled = true
                        buttonProcess.isEnabled = true
                        // Clear selected file if you want to allow new selection
                        // inputFileUri = null
                        // textViewFilePath.text = "No file selected"
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