package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.GradeLevel
import model.GradeResult
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File

val LocalAwtWindow = compositionLocalOf<Window?> { null }

object AppColors {
    val Primary       = Color(0xFF1E3A8A)
    val PrimaryLight  = Color(0xFF3B82F6)
    val Surface       = Color(0xFFF8FAFC)
    val CardBg        = Color(0xFFFFFFFF)
    val Divider       = Color(0xFFE5E7EB)
    val TextPrimary   = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)

    val GradeA = Color(0xFF16A34A)
    val GradeB = Color(0xFF2563EB)
    val GradeC = Color(0xFFCA8A04)
    val GradeD = Color(0xFFEA580C)
    val GradeF = Color(0xFFDC2626)

    fun forGrade(level: GradeLevel): Color = when (level) {
        GradeLevel.A_PLUS, GradeLevel.A, GradeLevel.A_MINUS -> GradeA
        GradeLevel.B_PLUS, GradeLevel.B, GradeLevel.B_MINUS -> GradeB
        GradeLevel.C_PLUS, GradeLevel.C, GradeLevel.C_MINUS -> GradeC
        GradeLevel.D_PLUS, GradeLevel.D                     -> GradeD
        GradeLevel.F                                         -> GradeF
    }
}

@Composable
fun FileDropZone(
    onPickFile: () -> Unit,
    onDropFile: (File) -> Unit
) {
    var isDraggingOver by remember { mutableStateOf(false) }

    val window = LocalAwtWindow.current

    DisposableEffect(window) {
        val listener = object : DropTargetAdapter() {
            override fun dragEnter(event: DropTargetDragEvent) {
                if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    isDraggingOver = true
                    event.acceptDrag(DnDConstants.ACTION_COPY)
                }
            }
            override fun dragExit(event: DropTargetEvent) {
                isDraggingOver = false
            }
            override fun drop(event: DropTargetDropEvent) {
                isDraggingOver = false
                event.acceptDrop(DnDConstants.ACTION_COPY)
                try {
                    val transferable = event.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        files.filterIsInstance<File>()
                            .firstOrNull { it.extension.lowercase() in listOf("xlsx", "xls") }
                            ?.let { onDropFile(it) }
                    }
                    event.dropComplete(true)
                } catch (ex: Exception) {
                    event.dropComplete(false)
                }
            }
        }
        val dropTarget = window?.let { DropTarget(it, listener) }
        window?.dropTarget = dropTarget

        onDispose {
            window?.dropTarget = null
            dropTarget?.removeDropTargetListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = if (isDraggingOver) AppColors.Primary else AppColors.PrimaryLight,
                shape = RoundedCornerShape(16.dp)
            )
            .background(if (isDraggingOver) Color(0xFFDBEAFE) else Color(0xFFEFF6FF))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Upload,
                contentDescription = null,
                tint               = if (isDraggingOver) AppColors.Primary else AppColors.PrimaryLight,
                modifier           = Modifier.size(48.dp)
            )
            Text(
                text      = if (isDraggingOver) "Release to load file"
                else "Drop an Excel file here or click to browse",
                color     = AppColors.TextSecondary,
                fontSize  = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Supported: .xlsx, .xls  |  Required columns: Name, Marks",
                color    = AppColors.TextSecondary,
                fontSize = 12.sp
            )
            Button(
                onClick = onPickFile,
                colors  = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) { Text("Choose File") }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Card(
        modifier  = Modifier.width(160.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun GradeBadge(grade: String, level: GradeLevel) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.forGrade(level).copy(alpha = 0.15f))
            .border(1.dp, AppColors.forGrade(level), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(grade, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.forGrade(level))
    }
}

@Composable
fun ResultsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF374151))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        listOf("Student Name" to 3f, "Marks (%)" to 1.5f, "Grade" to 1f, "GPA" to 1f, "Remarks" to 2f)
            .forEach { (label, weight) ->
                Text(
                    label,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp,
                    modifier   = Modifier.weight(weight)
                )
            }
    }
}

@Composable
fun ResultsTableRow(result: GradeResult, index: Int) {
    val bg = if (index % 2 == 0) Color.White else Color(0xFFF9FAFB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(result.name, modifier = Modifier.weight(3f), fontSize = 13.sp, color = AppColors.TextPrimary)
        Text("%.1f".format(result.marks), modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = AppColors.TextSecondary)
        Box(modifier = Modifier.weight(1f)) { GradeBadge(result.grade, result.gradeLevel) }
        Text("%.2f".format(result.gpa), modifier = Modifier.weight(1f), fontSize = 13.sp, color = AppColors.TextSecondary)
        Text(result.description, modifier = Modifier.weight(2f), fontSize = 12.sp, color = AppColors.TextSecondary)
    }
    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}

@Composable
fun ExportSuccessBanner(path: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.GradeA)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Export successful!", fontWeight = FontWeight.Bold, color = AppColors.GradeA)
                Text(path, fontSize = 11.sp, color = AppColors.TextSecondary)
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, contentDescription = null, tint = AppColors.GradeF)
            Spacer(Modifier.width(12.dp))
            Text(message, modifier = Modifier.weight(1f), color = Color(0xFF991B1B), fontSize = 13.sp)
            TextButton(onClick = onRetry) { Text("Try Again") }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text       = text,
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = AppColors.TextPrimary,
        modifier   = Modifier.padding(bottom = 8.dp)
    )
}
