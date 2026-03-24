import androidx.compose.runtime.*
import impl.ExcelExporterServiceImpl
import impl.ExcelReaderServiceImpl
import impl.GradeCalculatorServiceImpl
import impl.PdfExporterServiceImpl
import kotlinx.coroutines.*
import model.ProcessingReport
import model.Student
import java.io.File

sealed class AppUiState {
    object Idle     : AppUiState()
    object Loading  : AppUiState()
    data class Loaded(val students: List<Student>, val file: File) : AppUiState()
    data class Processed(val report: ProcessingReport, val sourceFile: File) : AppUiState()
    data class Error(val message: String) : AppUiState()
    data class ExportSuccess(val path: String, val report: ProcessingReport, val sourceFile: File) : AppUiState()
}

enum class ExportFormat { EXCEL, PDF }

class AppViewModel(
    private val excelReader:   ExcelReaderServiceImpl     = ExcelReaderServiceImpl(),
    private val gradeCalc:     GradeCalculatorServiceImpl = GradeCalculatorServiceImpl(),
    private val excelExporter: ExcelExporterServiceImpl   = ExcelExporterServiceImpl(),
    private val pdfExporter:   PdfExporterServiceImpl     = PdfExporterServiceImpl()
) {
    // Dispatchers.Main now resolves to SwingDispatcher once
    // kotlinx-coroutines-swing is on the classpath — state writes
    // always happen on the Swing EDT, which Compose Desktop requires.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var uiState by mutableStateOf<AppUiState>(AppUiState.Idle)
        private set

    fun loadFile(file: File) {
        uiState = AppUiState.Loading
        scope.launch {
            uiState = withContext(Dispatchers.IO) {
                runCatching { excelReader.readStudents(file) }
                    .fold(
                        onSuccess = { students ->
                            if (students.isEmpty()) AppUiState.Error("The file contains no student data.")
                            else AppUiState.Loaded(students, file)
                        },
                        onFailure = { e -> AppUiState.Error("Failed to read file: ${e.message}") }
                    )
            }
        }
    }

    fun processGrades() {
        val current = uiState as? AppUiState.Loaded ?: return
        uiState = AppUiState.Loading
        scope.launch {
            uiState = withContext(Dispatchers.Default) {
                runCatching { gradeCalc.processClass(current.students) }
                    .fold(
                        onSuccess  = { report -> AppUiState.Processed(report, current.file) },
                        onFailure  = { e -> AppUiState.Error("Grade processing failed: ${e.message}") }
                    )
            }
        }
    }

    fun exportReport(format: ExportFormat, destination: File) {
        val current = uiState as? AppUiState.Processed ?: return
        uiState = AppUiState.Loading
        scope.launch {
            uiState = withContext(Dispatchers.IO) {
                runCatching {
                    when (format) {
                        ExportFormat.EXCEL -> excelExporter.export(current.report, destination)
                        ExportFormat.PDF   -> pdfExporter.export(current.report, destination)
                    }
                }.fold(
                    onSuccess  = { AppUiState.ExportSuccess(destination.absolutePath, current.report, current.sourceFile) },
                    onFailure  = { e -> AppUiState.Error("Export failed: ${e.message}") }
                )
            }
        }
    }

    fun backToProcessed() {
        val current = uiState as? AppUiState.ExportSuccess ?: return
        uiState = AppUiState.Processed(current.report, current.sourceFile)
    }

    fun reset() { uiState = AppUiState.Idle }

    fun onCleared() { scope.cancel() }
}