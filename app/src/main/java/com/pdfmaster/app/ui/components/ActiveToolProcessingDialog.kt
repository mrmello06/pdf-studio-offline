package com.pdfmaster.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfmaster.app.data.PdfOperationsHelper
import com.pdfmaster.app.data.PdfTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ActiveToolProcessingDialog(
    tool: PdfTool,
    selectedUris: List<Uri>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var summaryMessage by remember { mutableStateOf<String?>(null) }

    // Live Thumbnail for PDF operations
    var pdfThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var totalPdfPages by remember { mutableStateOf(1) }

    // Option states
    var rotateDegrees by remember { mutableStateOf(90) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var startPage by remember { mutableStateOf("1") }
    var endPage by remember { mutableStateOf("2") }
    var annotationText by remember { mutableStateOf("APPROVED") }
    var reversePages by remember { mutableStateOf(false) }

    // PDF to JPG Options
    var jpgConvertAllPages by remember { mutableStateOf(true) }
    var jpgSelectedPageNum by remember { mutableStateOf("1") }

    val firstUri = selectedUris.firstOrNull()

    // Pre-calculate page count and thumbnail off the main thread
    LaunchedEffect(firstUri) {
        if (firstUri != null) {
            withContext(Dispatchers.IO) {
                totalPdfPages = PdfOperationsHelper.getPdfPageCount(context, firstUri)
                if (tool.id.startsWith("pdf_") || tool.id == "rotate_pdf" || tool.id == "crop_pdf" || tool.id == "page_numbers") {
                    pdfThumbnail = PdfOperationsHelper.renderFirstPageThumbnail(context, firstUri)
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header: Emoji & Tool Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tool.iconEmoji, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% Offline Engine • $totalPdfPages page${if (totalPdfPages > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }

                    IconButton(onClick = onDismiss, enabled = !isProcessing) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selected File Info Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Selected File (${selectedUris.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        selectedUris.take(2).forEach { uri ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = PdfOperationsHelper.getFileName(context, uri),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${PdfOperationsHelper.getFileSize(context, uri)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Live Preview for Rotate PDF
                if (tool.id == "rotate_pdf" && pdfThumbnail != null && resultFile == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Document Preview:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pdfThumbnail!!.asImageBitmap(),
                            contentDescription = "Rotate Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .rotate(rotateDegrees.toFloat())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tool-specific options if not yet completed
                if (resultFile == null && !isProcessing) {
                    when (tool.id) {
                        "pdf_to_jpg" -> {
                            Text("Page Conversion Option:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = jpgConvertAllPages,
                                    onClick = { jpgConvertAllPages = true },
                                    label = { Text("All $totalPdfPages Pages") }
                                )
                                FilterChip(
                                    selected = !jpgConvertAllPages,
                                    onClick = { jpgConvertAllPages = false },
                                    label = { Text("Select Single Page") }
                                )
                            }
                            if (!jpgConvertAllPages) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = jpgSelectedPageNum,
                                    onValueChange = { jpgSelectedPageNum = it },
                                    label = { Text("Page Number (1 to $totalPdfPages)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "rotate_pdf" -> {
                            Text("Select Rotation Angle:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(90, 180, 270).forEach { deg ->
                                    FilterChip(
                                        selected = rotateDegrees == deg,
                                        onClick = { rotateDegrees = deg },
                                        label = { Text("+$deg°") }
                                    )
                                }
                            }
                        }
                        "watermark" -> {
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Watermark Text") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "split_pdf" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = startPage,
                                    onValueChange = { startPage = it },
                                    label = { Text("From Page (1..$totalPdfPages)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endPage,
                                    onValueChange = { endPage = it },
                                    label = { Text("To Page (1..$totalPdfPages)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        "edit_pdf" -> {
                            OutlinedTextField(
                                value = annotationText,
                                onValueChange = { annotationText = it },
                                label = { Text("Text Note / Approval Stamp") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "organize_pdf" -> {
                            // organize_pdf is now handled by OrganizePdfDialog — this branch is not reached
                            Text(
                                "Use the full Organize PDF dialog to reorder pages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Processing Spinner State
                if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Processing on device...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Zero internet data used",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Result Ready State
                resultFile?.let { file ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summaryMessage ?: "Successfully Processed!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Open & Share Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { PdfOperationsHelper.shareFile(context, file) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }

                            Button(
                                onClick = { PdfOperationsHelper.openFile(context, file) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open")
                            }
                        }
                    }
                }

                // Error Message if any
                errorMessage?.let { err ->
                    Text(
                        text = "Error: $err",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                // Process Button (Shown if not yet completed)
                if (resultFile == null && !isProcessing && firstUri != null) {
                    Button(
                        onClick = {
                            isProcessing = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    when (tool.id) {
                                        "pdf_to_word" -> {
                                            resultFile = PdfOperationsHelper.pdfToWord(context, firstUri)
                                            summaryMessage = "Converted to Microsoft Word!"
                                        }
                                        "pdf_to_powerpoint" -> {
                                            resultFile = PdfOperationsHelper.pdfToPowerpoint(context, firstUri)
                                            summaryMessage = "Converted to PowerPoint!"
                                        }
                                        "pdf_to_excel" -> {
                                            resultFile = PdfOperationsHelper.pdfToExcel(context, firstUri)
                                            summaryMessage = "Converted to Excel Spreadsheet!"
                                        }
                                        "pdf_to_jpg" -> {
                                            val pageNum = jpgSelectedPageNum.toIntOrNull() ?: 1
                                            val (outResult, msg) = PdfOperationsHelper.pdfToJpgWithOptions(
                                                context,
                                                firstUri,
                                                convertAllPages = jpgConvertAllPages,
                                                selectedPageNum = pageNum
                                            )
                                            resultFile = outResult
                                            summaryMessage = msg
                                        }
                                        "word_to_pdf" -> {
                                            resultFile = PdfOperationsHelper.wordToPdf(context, firstUri)
                                            summaryMessage = "Word converted to PDF!"
                                        }
                                        "powerpoint_to_pdf" -> {
                                            resultFile = PdfOperationsHelper.powerpointToPdf(context, firstUri)
                                            summaryMessage = "PowerPoint converted to PDF!"
                                        }
                                        "excel_to_pdf" -> {
                                            resultFile = PdfOperationsHelper.excelToPdf(context, firstUri)
                                            summaryMessage = "Excel converted to PDF!"
                                        }
                                        "html_to_pdf" -> {
                                            resultFile = PdfOperationsHelper.htmlToPdf(context, firstUri)
                                            summaryMessage = "HTML converted to PDF!"
                                        }
                                        "rotate_pdf" -> {
                                            resultFile = PdfOperationsHelper.rotatePdf(context, firstUri, rotateDegrees)
                                            summaryMessage = "Rotated PDF saved!"
                                        }
                                        "watermark" -> {
                                            resultFile = PdfOperationsHelper.watermarkPdf(context, firstUri, watermarkText)
                                            summaryMessage = "Watermark stamped!"
                                        }
                                        "page_numbers" -> {
                                            resultFile = PdfOperationsHelper.addPageNumbers(context, firstUri)
                                            summaryMessage = "Page numbers added!"
                                        }
                                        "crop_pdf" -> {
                                            resultFile = PdfOperationsHelper.cropPdf(context, firstUri, 5f)
                                            summaryMessage = "Trimmed margins saved!"
                                        }
                                        "edit_pdf" -> {
                                            resultFile = PdfOperationsHelper.annotatePdf(context, firstUri, annotationText)
                                            summaryMessage = "Annotated PDF saved!"
                                        }
                                        "organize_pdf" -> {
                                            resultFile = PdfOperationsHelper.organizePdf(context, firstUri, reversePages)
                                            summaryMessage = "Reorganized PDF saved!"
                                        }
                                        "merge_pdf" -> {
                                            resultFile = PdfOperationsHelper.mergePdfs(context, selectedUris)
                                            summaryMessage = "Merged ${selectedUris.size} documents into 1!"
                                        }
                                        "split_pdf" -> {
                                            val s = startPage.toIntOrNull() ?: 1
                                            val e = endPage.toIntOrNull() ?: 2
                                            resultFile = PdfOperationsHelper.splitPdfRange(context, firstUri, s, e)
                                            summaryMessage = "Extracted Pages $s to $e!"
                                        }
                                        "compress_pdf" -> {
                                            resultFile = PdfOperationsHelper.compressPdf(context, firstUri)
                                            summaryMessage = "Compressed PDF saved!"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Processing error occurred"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process Offline Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
