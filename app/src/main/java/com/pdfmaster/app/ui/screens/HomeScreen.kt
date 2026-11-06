package com.pdfmaster.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pdfmaster.app.data.PdfOperationsHelper
import com.pdfmaster.app.data.PdfTool
import com.pdfmaster.app.data.PdfToolkitCatalog
import com.pdfmaster.app.ui.components.ActiveToolProcessingDialog
import com.pdfmaster.app.ui.components.JpgToPdfPreviewDialog
import com.pdfmaster.app.ui.components.OrganizePdfDialog
import com.pdfmaster.app.ui.components.ToolDetailBottomSheet
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasStoragePermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var selectedToolForDetail by remember { mutableStateOf<PdfTool?>(null) }
    var activeToolForAction by remember { mutableStateOf<PdfTool?>(null) }
    var selectedUrisForProcessing by remember { mutableStateOf<List<Uri>?>(null) }
    var activeNoticeMessage by remember { mutableStateOf<String?>(null) }

    // State for JPG to PDF preview
    var showJpgPreviewDialog by remember { mutableStateOf(false) }
    var imageUrisForJpgToPdf by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // State for Organize PDF visual reorder dialog
    var showOrganizePdfDialog by remember { mutableStateOf(false) }
    var organizeInputUri by remember { mutableStateOf<Uri?>(null) }

    // File pickers using Android Storage Access Framework (SAF)
    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Check if this was for Organize PDF → open the dedicated dialog
            if (activeToolForAction?.id == "organize_pdf") {
                organizeInputUri = uri
                showOrganizePdfDialog = true
                activeToolForAction = null  // Don't open the generic dialog
            } else {
                selectedUrisForProcessing = listOf(uri)
            }
        }
    }

    val multiplePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUrisForProcessing = uris
        }
    }

    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            imageUrisForJpgToPdf = uris
            showJpgPreviewDialog = true
        }
    }

    val anyFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUrisForProcessing = listOf(uri)
        }
    }

    fun launchPickerForTool(tool: PdfTool) {
        activeToolForAction = tool
        when (tool.id) {
            "jpg_to_pdf" -> {
                multipleImagePicker.launch("image/*")
            }
            "merge_pdf" -> {
                multiplePdfPicker.launch(arrayOf("application/pdf"))
            }
            "word_to_pdf", "excel_to_pdf", "powerpoint_to_pdf", "html_to_pdf" -> {
                anyFilePicker.launch(arrayOf("*/*"))
            }
            else -> {
                singlePdfPicker.launch(arrayOf("application/pdf"))
            }
        }
    }

    // Map to keep track of which main categories and sections are expanded
    val expandedCategories = remember {
        mutableStateMapOf<String, Boolean>().apply {
            PdfToolkitCatalog.categories.forEach { put(it.id, true) }
        }
    }

    val expandedSections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            PdfToolkitCatalog.categories.forEach { cat ->
                cat.sections.forEach { sec ->
                    put("${cat.id}_${sec.sectionTitle}", true)
                }
            }
        }
    }

    // MEMOIZED FILTERING: Pre-computed outside the scroll loop so scrolling NEVER stutters!
    val filteredCategories = remember(searchQuery, selectedCategoryFilter) {
        val q = searchQuery.trim().lowercase()
        PdfToolkitCatalog.categories.filter { cat ->
            if (selectedCategoryFilter != "all" && cat.id != selectedCategoryFilter) {
                false
            } else if (q.isEmpty()) {
                true
            } else {
                cat.sections.any { sec ->
                    sec.tools.any { tool ->
                        tool.title.lowercase().contains(q) ||
                        tool.shortSummary.lowercase().contains(q) ||
                        tool.capabilities.any { it.lowercase().contains(q) }
                    }
                }
            }
        }.map { cat ->
            if (q.isEmpty()) cat
            else {
                cat.copy(
                    sections = cat.sections.map { sec ->
                        sec.copy(
                            tools = sec.tools.filter { tool ->
                                tool.title.lowercase().contains(q) ||
                                tool.shortSummary.lowercase().contains(q) ||
                                tool.capabilities.any { it.lowercase().contains(q) }
                            }
                        )
                    }.filter { it.tools.isNotEmpty() }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PDF Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "OFFLINE",
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRequestPermissions) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Permissions",
                            tint = if (hasStoragePermission) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick search input
            item(key = "search_box") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tools (e.g. Word, Merge, Rotate)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )
            }

            // Filter Chips
            item(key = "filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == "all",
                        onClick = { selectedCategoryFilter = "all" },
                        label = { Text("All Tools") }
                    )
                    FilterChip(
                        selected = selectedCategoryFilter == "convert_pdfs",
                        onClick = { selectedCategoryFilter = "convert_pdfs" },
                        label = { Text("📄 Convert") }
                    )
                    FilterChip(
                        selected = selectedCategoryFilter == "edit_manage_pdfs",
                        onClick = { selectedCategoryFilter = "edit_manage_pdfs" },
                        label = { Text("🛠️ Edit & Manage") }
                    )
                }
            }

            // Status notification banner
            if (activeNoticeMessage != null) {
                item(key = "notice_msg") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = activeNoticeMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { activeNoticeMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // High Performance Memoized Categories rendering
            items(filteredCategories, key = { it.id }) { category ->
                val isCategoryExpanded = expandedCategories[category.id] ?: true
                val categoryRotationState by animateFloatAsState(
                    targetValue = if (isCategoryExpanded) 180f else 0f,
                    label = "categoryChevron"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Main Category Heading (CLICKABLE)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    expandedCategories[category.id] = !isCategoryExpanded
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.emoji,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = category.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isCategoryExpanded) "Collapse" else "Expand",
                                modifier = Modifier.rotate(categoryRotationState),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Category Content: Sections
                        if (isCategoryExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                category.sections.forEach { section ->
                                    val sectionKey = "${category.id}_${section.sectionTitle}"
                                    val isSectionExpanded = expandedSections[sectionKey] ?: true
                                    val sectionRotationState by animateFloatAsState(
                                        targetValue = if (isSectionExpanded) 180f else 0f,
                                        label = "sectionChevron"
                                    )

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            // Section Subheading (CLICKABLE to expand/collapse)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        expandedSections[sectionKey] = !isSectionExpanded
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = section.sectionTitle,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = section.sectionDescription,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        text = "${section.tools.size} tools",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Icon(
                                                    imageVector = Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    modifier = Modifier.rotate(sectionRotationState),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Section Tools List
                                            if (isSectionExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(top = 10.dp)
                                                        .fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    section.tools.forEach { tool ->
                                                        ToolItemCard(
                                                            tool = tool,
                                                            onClick = {
                                                                selectedToolForDetail = tool
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Tool Detail Bottom Sheet: explains what it can do and launches file picker
    selectedToolForDetail?.let { tool ->
        ToolDetailBottomSheet(
            tool = tool,
            onDismiss = { selectedToolForDetail = null },
            onActionClick = { selectedTool ->
                selectedToolForDetail = null
                launchPickerForTool(selectedTool)
            }
        )
    }

    // Interactive JPG to PDF Preview Dialog (Reorder & Rotate)
    if (showJpgPreviewDialog && imageUrisForJpgToPdf.isNotEmpty()) {
        JpgToPdfPreviewDialog(
            initialUris = imageUrisForJpgToPdf,
            onDismiss = { showJpgPreviewDialog = false },
            onSuccess = { generatedPdf ->
                showJpgPreviewDialog = false
                activeNoticeMessage = "PDF generated successfully (${imageUrisForJpgToPdf.size} pages)!"
                PdfOperationsHelper.openFile(context, generatedPdf)
            }
        )
    }

    // Organize PDF — visual drag-to-reorder page thumbnail dialog
    if (showOrganizePdfDialog && organizeInputUri != null) {
        OrganizePdfDialog(
            inputUri = organizeInputUri!!,
            onDismiss = {
                showOrganizePdfDialog = false
                organizeInputUri = null
            },
            onSuccess = { resultFile ->
                showOrganizePdfDialog = false
                organizeInputUri = null
                activeNoticeMessage = "Pages reorganized! Saved as ${resultFile.name}"
                PdfOperationsHelper.openFile(context, resultFile)
            }
        )
    }

    // Processing & Options Dialog (Opens after user selects files)
    if (activeToolForAction != null && selectedUrisForProcessing != null) {
        ActiveToolProcessingDialog(
            tool = activeToolForAction!!,
            selectedUris = selectedUrisForProcessing!!,
            onDismiss = {
                activeToolForAction = null
                selectedUrisForProcessing = null
            }
        )
    }
}

@Composable
private fun ToolItemCard(
    tool: PdfTool,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tool.iconEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tool.badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = tool.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Text(
                    text = tool.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
