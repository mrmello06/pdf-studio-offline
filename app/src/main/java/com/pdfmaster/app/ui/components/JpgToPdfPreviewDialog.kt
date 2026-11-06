package com.pdfmaster.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfmaster.app.data.PdfOperationsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ImageItem(
    val uri: Uri,
    var rotation: Int = 0,
    var thumbnail: Bitmap? = null
)

@Composable
fun JpgToPdfPreviewDialog(
    initialUris: List<Uri>,
    onDismiss: () -> Unit,
    onSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imageItems = remember {
        mutableStateListOf<ImageItem>().apply {
            initialUris.forEach { add(ImageItem(uri = it)) }
        }
    }

    var isGenerating by remember { mutableStateOf(false) }

    // Load thumbnails
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            imageItems.forEachIndexed { index, item ->
                try {
                    val stream = context.contentResolver.openInputStream(item.uri)
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    imageItems[index] = item.copy(thumbnail = bmp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "📷 JPG to PDF Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${imageItems.size} images • Reorder & rotate before compiling",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isGenerating) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Images Carousel / Horizontal List
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(imageItems) { index, item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .width(160.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Image preview box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.thumbnail != null) {
                                        Image(
                                            bitmap = item.thumbnail!!.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(item.rotation.toFloat())
                                        )
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }

                                    // Page Number Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Controls: Rotate & Swap Order
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Move Left
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val prev = imageItems[index - 1]
                                                imageItems[index - 1] = item
                                                imageItems[index] = prev
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                                    }

                                    // Rotate 90°
                                    IconButton(
                                        onClick = {
                                            imageItems[index] = item.copy(rotation = (item.rotation + 90) % 360)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(18.dp))
                                    }

                                    // Move Right
                                    IconButton(
                                        onClick = {
                                            if (index < imageItems.size - 1) {
                                                val next = imageItems[index + 1]
                                                imageItems[index + 1] = item
                                                imageItems[index] = next
                                            }
                                        },
                                        enabled = index < imageItems.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = {
                                            if (imageItems.size > 1) {
                                                imageItems.removeAt(index)
                                            }
                                        },
                                        enabled = imageItems.size > 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Compiling PDF in selected order...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                try {
                                    val pairs = imageItems.map { Pair(it.uri, it.rotation) }
                                    val pdfFile = PdfOperationsHelper.imagesToPdfCustom(context, pairs)
                                    onSuccess(pdfFile)
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate PDF (${imageItems.size} Pages)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
