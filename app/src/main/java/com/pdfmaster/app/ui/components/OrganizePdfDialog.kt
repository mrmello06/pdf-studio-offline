package com.pdfmaster.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pdfmaster.app.data.PdfOperationsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen dialog for Organize PDF with:
 *  - Big, high-definition scrollable page previews
 *  - Fast reorder buttons (⏫ To Top, ▲ Move Up, ▼ Move Down, ⏬ To Bottom)
 *  - Direct "Move to Position" jump dialog (e.g., move page 3 to position 1)
 *  - One-tap "Reverse All" order toggle
 *  - Tap preview to inspect full screen
 *  - Apply button calling organizePdfCustomOrder()
 */
@Composable
fun OrganizePdfDialog(
    inputUri: Uri,
    onDismiss: () -> Unit,
    onSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isLoadingThumbs by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for "Move to position"
    var moveDialogPosition by remember { mutableStateOf<Int?>(null) } // 0-based position in pageOrder
    var moveTargetInput by remember { mutableStateOf("") }

    // Fullscreen zoomed preview dialog
    var zoomedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Load all page thumbnails in high resolution off-thread
    LaunchedEffect(inputUri) {
        withContext(Dispatchers.IO) {
            val pages = PdfOperationsHelper.renderAllPageThumbnails(context, inputUri)
            withContext(Dispatchers.Main) {
                thumbnails = pages
                pageOrder = pages.keys.sorted()
                isLoadingThumbs = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header Bar ───────────────────────────────────────────
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗂️", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Organize PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isLoadingThumbs) "Loading large previews…"
                                else "${pageOrder.size} pages · Scroll to inspect & reorder",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }

                        // Reverse order quick button
                        IconButton(
                            onClick = {
                                if (pageOrder.isNotEmpty()) {
                                    pageOrder = pageOrder.reversed()
                                }
                            },
                            enabled = !isProcessing && !isLoadingThumbs
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Reverse sequence", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Reset order button
                        IconButton(
                            onClick = {
                                pageOrder = thumbnails.keys.sorted()
                            },
                            enabled = !isProcessing && !isLoadingThumbs
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset order")
                        }

                        // Close button
                        IconButton(onClick = { if (!isProcessing) onDismiss() }, enabled = !isProcessing) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider()

                // ── Big Scrollable Page Previews Feed ───────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoadingThumbs) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Rendering big previews...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Loading high-resolution page scans",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(pageOrder, key = { _, origIdx -> origIdx }) { position, origIdx ->
                                val thumb = thumbnails[origIdx]
                                BigPageReorderCard(
                                    position = position + 1,
                                    totalPages = pageOrder.size,
                                    originalPageNum = origIdx + 1,
                                    thumbnail = thumb,
                                    onMoveUp = {
                                        if (position > 0) {
                                            val newOrder = pageOrder.toMutableList()
                                            val tmp = newOrder[position - 1]
                                            newOrder[position - 1] = newOrder[position]
                                            newOrder[position] = tmp
                                            pageOrder = newOrder
                                        }
                                    },
                                    onMoveDown = {
                                        if (position < pageOrder.size - 1) {
                                            val newOrder = pageOrder.toMutableList()
                                            val tmp = newOrder[position + 1]
                                            newOrder[position + 1] = newOrder[position]
                                            newOrder[position] = tmp
                                            pageOrder = newOrder
                                        }
                                    },
                                    onMoveToTop = {
                                        if (position > 0) {
                                            val newOrder = pageOrder.toMutableList()
                                            val item = newOrder.removeAt(position)
                                            newOrder.add(0, item)
                                            pageOrder = newOrder
                                        }
                                    },
                                    onMoveToBottom = {
                                        if (position < pageOrder.size - 1) {
                                            val newOrder = pageOrder.toMutableList()
                                            val item = newOrder.removeAt(position)
                                            newOrder.add(item)
                                            pageOrder = newOrder
                                        }
                                    },
                                    onJumpClick = {
                                        moveDialogPosition = position
                                        moveTargetInput = (position + 1).toString()
                                    },
                                    onPreviewClick = {
                                        zoomedBitmap = thumb
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ── Error notice if any ─────────────────────────────────
                errorMessage?.let {
                    Text(
                        "Error: $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // ── Bottom Action Bar ───────────────────────────────────
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                pageOrder = thumbnails.keys.sorted()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isProcessing && !isLoadingThumbs
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }

                        Button(
                            onClick = {
                                isProcessing = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val resultFile = PdfOperationsHelper.organizePdfCustomOrder(
                                            context, inputUri, pageOrder
                                        )
                                        onSuccess(resultFile)
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to reorganize PDF"
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isProcessing && !isLoadingThumbs && pageOrder.isNotEmpty()
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rebuilding PDF...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Reordered PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Direct Move Page to Position (e.g. 3 to 1) ──────
    moveDialogPosition?.let { fromIdx ->
        val currentNumber = fromIdx + 1
        AlertDialog(
            onDismissRequest = { moveDialogPosition = null },
            icon = { Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Move Page $currentNumber") },
            text = {
                Column {
                    Text(
                        "Enter the new position for this page (1 to ${pageOrder.size}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = moveTargetInput,
                        onValueChange = { moveTargetInput = it },
                        label = { Text("New Position Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetNum = moveTargetInput.toIntOrNull()
                        if (targetNum != null && targetNum in 1..pageOrder.size) {
                            val targetIdx = targetNum - 1
                            if (targetIdx != fromIdx) {
                                val newOrder = pageOrder.toMutableList()
                                val item = newOrder.removeAt(fromIdx)
                                newOrder.add(targetIdx, item)
                                pageOrder = newOrder
                            }
                        }
                        moveDialogPosition = null
                    }
                ) {
                    Text("Move Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { moveDialogPosition = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Dialog: Full Screen Zoom Preview ─────────────────────────
    zoomedBitmap?.let { bmp ->
        Dialog(
            onDismissRequest = { zoomedBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Full page preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { zoomedBitmap = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Big Page Card with prominent high-definition preview image and quick reorder controls
 */
@Composable
private fun BigPageReorderCard(
    position: Int,
    totalPages: Int,
    originalPageNum: Int,
    thumbnail: Bitmap?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onJumpClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Card Header: Page Badge + Position Info + Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Number Badge (clickable to trigger "Move to #" jump)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onJumpClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#$position",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change Position",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Page $position of $totalPages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (originalPageNum != position) {
                        Text(
                            text = "Originally Page $originalPageNum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Reorder Action Buttons (⏫ Top, ▲ Up, ▼ Down, ⏬ Bottom)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onMoveToTop,
                        enabled = position > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardDoubleArrowUp,
                            contentDescription = "Move to Top",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onMoveUp,
                        enabled = position > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onMoveDown,
                        enabled = position < totalPages,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onMoveToBottom,
                        enabled = position < totalPages,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardDoubleArrowDown,
                            contentDescription = "Move to Bottom",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Big High-Definition Page Preview ────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable { onPreviewClick() },
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Page $position preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Tap to zoom hint badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Tap to enlarge",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
