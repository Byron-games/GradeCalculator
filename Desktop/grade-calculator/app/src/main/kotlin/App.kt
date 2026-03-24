import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import ui.components.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
fun App(viewModel: AppViewModel = remember { AppViewModel() }) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar()
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = viewModel.uiState) {
                        is AppUiState.Idle         -> IdleScreen(viewModel)
                        is AppUiState.Loading      -> LoadingScreen()
                        is AppUiState.Loaded       -> LoadedScreen(state, viewModel)
                        is AppUiState.Processed    -> ProcessedScreen(state, viewModel)
                        is AppUiState.Error        -> ErrorScreen(state.message, viewModel)
                        is AppUiState.ExportSuccess-> ExportSuccessScreen(state, viewModel)
                    }
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.Primary)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("Grade Calculator", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Idle Screen ───────────────────────────────────────────────────────────────
@Composable
private fun IdleScreen(viewModel: AppViewModel) {
    var showFilePicker by remember { mutableStateOf(false) }

    ContentWrapper {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionTitle("Upload Student Data")
            FileDropZone(
                onPickFile = { showFilePicker = true },
                onDropFile = { file -> viewModel.loadFile(file) }   // ← wire up drop
            )
            InstructionsCard()
        }
    }

    if (showFilePicker) {
        FileChooserDialog(
            title  = "Select Excel File",
            filter = { _, name -> name.endsWith(".xlsx", ignoreCase = true)
                    || name.endsWith(".xls",  ignoreCase = true) },
            onResult = { file ->
                showFilePicker = false          // ← always reset, even on cancel
                file?.let { viewModel.loadFile(it) }
            }
        )
    }
}

// ── Loaded Screen ─────────────────────────────────────────────────────────────

@Composable
private fun LoadedScreen(state: AppUiState.Loaded, viewModel: AppViewModel) {
    ContentWrapper {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // File info banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = AppColors.PrimaryLight)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${state.students.size} students found", fontSize = 12.sp, color = AppColors.TextSecondary)
                    }
                    TextButton(onClick = viewModel::reset) { Text("Change File") }
                }
            }

            SectionTitle("Preview (first 10 students)")
            // Preview table
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF374151))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Student Name", color = Color.White, fontWeight = FontWeight.Bold,
                             fontSize = 12.sp, modifier = Modifier.weight(3f))
                        Text("Marks (%)", color = Color.White, fontWeight = FontWeight.Bold,
                             fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    }
                    state.students.take(10).forEachIndexed { i, student ->
                        val bg = if (i % 2 == 0) Color.White else Color(0xFFF9FAFB)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(student.name,  modifier = Modifier.weight(3f),   fontSize = 13.sp)
                            Text("%.1f".format(student.marks), modifier = Modifier.weight(1.5f), fontSize = 13.sp,
                                 color = AppColors.TextSecondary)
                        }
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
                    }
                    if (state.students.size > 10) {
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("… and ${state.students.size - 10} more students",
                                 fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            // Action button
            Button(
                onClick  = viewModel::processGrades,
                modifier = Modifier.align(Alignment.End),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Calculate Grades", fontSize = 14.sp)
            }
        }
    }
}

// ── Processed Screen ──────────────────────────────────────────────────────────

@Composable
private fun ProcessedScreen(state: AppUiState.Processed, viewModel: AppViewModel) {
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingFormat    by remember { mutableStateOf(ExportFormat.EXCEL) }
    var showFileSaver    by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Stats row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement= Arrangement.spacedBy(16.dp),
                verticalAlignment    = Alignment.CenterVertically
            ) {
                StatCard("Total Students", state.report.totalStudents.toString())
                StatCard("Passed",         state.report.passCount.toString(), AppColors.GradeA)
                StatCard("Failed",         state.report.failCount.toString(), AppColors.GradeF)
                StatCard("Avg GPA",        "%.2f".format(state.report.classAvgGpa), AppColors.Primary)
                StatCard("Highest Mark",   "%.1f%%".format(state.report.highestMark), AppColors.GradeA)
                StatCard("Lowest Mark",    "%.1f%%".format(state.report.lowestMark), AppColors.GradeF)

                Spacer(modifier = Modifier.weight(1f))

                // Export buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            pendingFormat = ExportFormat.EXCEL
                            showFileSaver = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Excel", fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            pendingFormat = ExportFormat.PDF
                            showFileSaver = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export PDF", fontSize = 13.sp)
                    }
                }
            }
        }

        // Results table
        Box(modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 12.dp)) {
            Card(
                modifier  = Modifier.fillMaxSize(),
                shape     = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    ResultsTableHeader()
                    LazyColumn {
                        itemsIndexed(state.report.results) { index, result ->
                            ResultsTableRow(result, index)
                        }
                    }
                }
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Source: ${state.sourceFile.name}", fontSize = 12.sp, color = AppColors.TextSecondary)
            TextButton(onClick = viewModel::reset) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start Over")
            }
        }
    }

    // Save file dialog
    if (showFileSaver) {
        val ext         = if (pendingFormat == ExportFormat.EXCEL) "xlsx" else "pdf"
        val defaultName = "grades_${state.sourceFile.nameWithoutExtension}.$ext"
        FileSaverDialog(
            title       = "Save ${if (pendingFormat == ExportFormat.EXCEL) "Excel" else "PDF"} File",
            defaultName = defaultName,
            onResult    = { file ->
                showFileSaver = false
                file?.let { viewModel.exportReport(pendingFormat, it) }
            }
        )
    }
}

// ── Error Screen ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, viewModel: AppViewModel) {
    ContentWrapper {
        ErrorBanner(message = message, onRetry = viewModel::reset)
    }
}

// ── Export Success Screen ─────────────────────────────────────────────────────

@Composable
private fun ExportSuccessScreen(state: AppUiState.ExportSuccess, viewModel: AppViewModel) {
    ContentWrapper {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExportSuccessBanner(path = state.path, onDismiss = viewModel::backToProcessed)
            Button(
                onClick = viewModel::backToProcessed,
                colors  = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to Results")
            }
        }
    }
}

// ── Loading Screen ────────────────────────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
            Text("Processing…", color = AppColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

// ── Instructions Card ─────────────────────────────────────────────────────────

@Composable
private fun InstructionsCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Excel File Requirements", fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E), fontSize = 13.sp)
            }
            listOf(
                "Row 1 must be a header row (any text — it will be skipped)",
                "Column A → Student full name",
                "Column B → Numeric mark between 0 and 100",
                "Both .xlsx and .xls formats are supported"
            ).forEach { hint ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = Color(0xFFB45309), modifier = Modifier.padding(end = 6.dp, top = 1.dp), fontSize = 12.sp)
                    Text(hint, fontSize = 12.sp, color = Color(0xFF78350F))
                }
            }
        }
    }
}

// ── Layout wrapper ────────────────────────────────────────────────────────────

@Composable
private fun ContentWrapper(content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        content = content
    )
}

// ── AWT File Dialogs ──────────────────────────────────────────────────────────
// Fixed: onResult now fires on BOTH confirm AND cancel
@Composable
private fun FileChooserDialog(
    title: String,
    filter: FilenameFilter,
    onResult: (File?) -> Unit
) {
    AwtWindow(
        create = {
            object : FileDialog(null as Frame?, title, LOAD) {
                override fun setVisible(value: Boolean) {
                    super.setVisible(value)
                    if (value) {                           // called after dialog closes
                        val f = if (file != null && directory != null)
                            File(directory, file) else null
                        onResult(f)                        // null = user cancelled → still resets showFilePicker
                    }
                }
            }.also { it.filenameFilter = filter }
        },
        dispose = FileDialog::dispose
    )
}

@Composable
private fun FileSaverDialog(
    title: String,
    defaultName: String,
    onResult: (File?) -> Unit
) {
    AwtWindow(
        create = {
            object : FileDialog(null as Frame?, title, SAVE) {
                override fun setVisible(value: Boolean) {
                    super.setVisible(value)
                    if (value) {
                        val f = if (file != null && directory != null)
                            File(directory, file)
                        else null
                        onResult(f)
                    }
                }
            }.also {
                it.file = defaultName
            }
        },
        dispose = FileDialog::dispose
    )
}
