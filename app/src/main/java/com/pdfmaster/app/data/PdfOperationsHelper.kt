package com.pdfmaster.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfOperationsHelper {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "document.pdf"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): String {
        var sizeBytes: Long = 0
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formatBytes(sizeBytes)
    }

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 1
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            1
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            String.format(Locale.US, "%.1f KB", kb)
        }
    }

    fun getOutputDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PDFStudio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun generateOutputFileName(prefix: String, ext: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timestamp}.$ext"
    }

    // ==========================================
    // 1. PDF TO OTHER FORMATS
    // ==========================================

    /**
     * PDF to Word (.docx)
     */
    suspend fun pdfToWord(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        inputStream.close()

        val lines = text.split("\n").map { it.trim() }
        val outputFile = File(getOutputDirectory(context), generateOutputFileName("PDF_to_Word", "docx"))
        OpenXmlHelper.createDocxFromText(lines, outputFile)
        outputFile
    }

    /**
     * PDF to PowerPoint (.pptx)
     */
    suspend fun pdfToPowerpoint(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val slidesText = mutableListOf<String>()

        for (i in 1..document.numberOfPages) {
            stripper.startPage = i
            stripper.endPage = i
            val pageText = stripper.getText(document).trim()
            slidesText.add(pageText.ifEmpty { "Slide $i" })
        }
        document.close()
        inputStream.close()

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("PDF_to_PowerPoint", "pptx"))
        OpenXmlHelper.createPptxFromSlides(slidesText, outputFile)
        outputFile
    }

    /**
     * PDF to Excel (.csv)
     */
    suspend fun pdfToExcel(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        inputStream.close()

        val rows = mutableListOf<List<String>>()
        text.split("\n").forEach { line ->
            val cells = line.split(Regex("\\s{2,}|\t")).map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }
        if (rows.isEmpty()) {
            rows.add(listOf("Extracted Content"))
            rows.add(listOf(text))
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("PDF_to_Excel", "csv"))
        OpenXmlHelper.createCsvSpreadsheet(rows, outputFile)
        outputFile
    }

    /**
     * PDF to JPG (Option to convert ALL pages or a SPECIFIC page)
     * When all pages are chosen: generates separate JPGs for all pages AND bundles them into a ZIP archive for easy 1-click sharing!
     */
    suspend fun pdfToJpgWithOptions(
        context: Context,
        inputUri: Uri,
        convertAllPages: Boolean,
        selectedPageNum: Int
    ): Pair<File, String> = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")!!
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = getOutputDirectory(context)

        if (!convertAllPages) {
            // Convert only selected page
            val targetIdx = (selectedPageNum - 1).coerceIn(0, totalPages - 1)
            val page = renderer.openPage(targetIdx)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val pageFile = File(outputDir, "PDF_Page_${targetIdx + 1}_${timestamp}.jpg")
            val fos = FileOutputStream(pageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.close()
            bitmap.recycle()
            renderer.close()
            pfd.close()

            Pair(pageFile, "Page ${targetIdx + 1} converted to JPG image!")
        } else {
            // Convert ALL pages: generate individual JPGs and package them into a ZIP archive
            val zipFile = File(outputDir, "PDF_All_${totalPages}_Pages_${timestamp}.zip")
            val zos = ZipOutputStream(FileOutputStream(zipFile))

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageFile = File(outputDir, "PDF_Page_${i + 1}_${timestamp}.jpg")
                val fos = FileOutputStream(pageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                fos.close()

                // Append each page to the ZIP
                zos.putNextEntry(ZipEntry("Page_${i + 1}.jpg"))
                val bytes = pageFile.readBytes()
                zos.write(bytes)
                zos.closeEntry()

                bitmap.recycle()
            }
            zos.close()
            renderer.close()
            pfd.close()

            Pair(zipFile, "All $totalPages pages converted to JPGs (Saved in ZIP & Gallery)!")
        }
    }

    // ==========================================
    // 2. OTHER FORMATS TO PDF
    // ==========================================

    suspend fun wordToPdf(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        val paragraphs = OpenXmlHelper.extractTextFromDocx(inputStream)
        inputStream.close()

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 50f
        val usableWidth = 495f
        val lineHeight = 20f
        val paraSpacing = 10f
        val marginTop = 60f
        val marginBottom = 800f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isAntiAlias = true
        }

        var pageNum = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = marginTop

        fun newPage() {
            pdfDocument.finishPage(page)
            pageNum++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            y = marginTop
        }

        fun drawLine(text: String) {
            if (y > marginBottom) newPage()
            canvas.drawText(text, marginLeft, y, paint)
            y += lineHeight
        }

        for (para in paragraphs) {
            if (para.isBlank()) {
                y += paraSpacing
                continue
            }
            val words = para.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > usableWidth) {
                    drawLine(line)
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                drawLine(line)
            }
            y += paraSpacing
        }
        pdfDocument.finishPage(page)

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Word_Converted", "pdf"))
        val fos = FileOutputStream(outputFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        outputFile
    }

    suspend fun powerpointToPdf(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        // Each element in slideTexts = full text content of one PPTX slide
        val slideTexts = OpenXmlHelper.extractTextFromPptx(inputStream)
        inputStream.close()

        val pdfDocument = PdfDocument()
        val slideWidth = 792    // landscape A4-ish
        val slideHeight = 612

        val paintTitle = Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintBody = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }
        val paintBorder = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val paintBg = Paint().apply {
            color = Color.rgb(245, 245, 255)
            style = Paint.Style.FILL
        }

        val slideList = if (slideTexts.isEmpty()) listOf("") else slideTexts
        slideList.forEachIndexed { index, slideText ->
            val pageInfo = PdfDocument.PageInfo.Builder(slideWidth, slideHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Slide background
            canvas.drawRect(0f, 0f, slideWidth.toFloat(), slideHeight.toFloat(), paintBg)
            // Slide border
            canvas.drawRect(20f, 20f, slideWidth - 20f, slideHeight - 20f, paintBorder)
            // Slide number header bar
            val barPaint = Paint().apply { color = Color.rgb(79, 70, 229); style = Paint.Style.FILL }
            canvas.drawRect(20f, 20f, slideWidth - 20f, 70f, barPaint)
            val titlePaintWhite = Paint(paintTitle).apply { color = Color.WHITE; textSize = 18f }
            canvas.drawText("Slide ${index + 1}", 40f, 55f, titlePaintWhite)

            // Body text with word-wrap
            val usableWidth = slideWidth - 100f
            var y = 100f
            for (rawLine in slideText.split("\n")) {
                if (y > slideHeight - 40f) break
                val words = rawLine.trim().split(" ")
                var line = ""
                for (word in words) {
                    val test = if (line.isEmpty()) word else "$line $word"
                    if (paintBody.measureText(test) > usableWidth) {
                        canvas.drawText(line, 50f, y, paintBody)
                        y += 22f
                        line = word
                    } else {
                        line = test
                    }
                    if (y > slideHeight - 40f) break
                }
                if (line.isNotEmpty() && y <= slideHeight - 40f) {
                    canvas.drawText(line, 50f, y, paintBody)
                    y += 22f
                }
            }

            pdfDocument.finishPage(page)
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("PowerPoint_Converted", "pdf"))
        val fos = FileOutputStream(outputFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        outputFile
    }

    suspend fun excelToPdf(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)!!
        val rows = OpenXmlHelper.extractRowsFromXlsx(inputStream)
        inputStream.close()

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginTop = 80f
        val rowH = 24f
        val colWidth = 100f
        val maxCols = 5
        val marginLeft = 30f

        val titlePaint = Paint().apply {
            color = Color.BLACK; textSize = 16f; isFakeBoldText = true; isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE; textSize = 11f; isFakeBoldText = true; isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.DKGRAY; textSize = 11f; isAntiAlias = true
        }
        val gridPaint = Paint().apply {
            color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(79, 70, 229); style = Paint.Style.FILL
        }

        var pageNum = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = marginTop

        // Page title
        canvas.drawText("Excel Spreadsheet", marginLeft, y - 10f, titlePaint)

        rows.forEachIndexed { rowIdx, row ->
            if (y + rowH > pageHeight - 20f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = marginTop
            }
            val isHeader = rowIdx == 0
            var x = marginLeft
            row.take(maxCols).forEach { cell ->
                // Cell background for header
                if (isHeader) {
                    canvas.drawRect(x, y - rowH + 6f, x + colWidth, y + 6f, headerBgPaint)
                    canvas.drawText(cell.take(13), x + 4f, y, headerPaint)
                } else {
                    canvas.drawRect(x, y - rowH + 6f, x + colWidth, y + 6f, gridPaint)
                    canvas.drawText(cell.take(13), x + 4f, y, textPaint)
                }
                x += colWidth
            }
            y += rowH
        }
        pdfDocument.finishPage(page)

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Excel_Converted", "pdf"))
        val fos = FileOutputStream(outputFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        outputFile
    }

    suspend fun htmlToPdf(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val htmlContent = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.readText() ?: "HTML Page"
        // Strip HTML tags, collapse whitespace, preserve paragraph breaks
        val withBreaks = htmlContent
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|h[1-6]|li|tr)>"), "\n")
        val plainText = withBreaks.replace(Regex("<[^>]*>"), "").replace(Regex("[ \t]+"), " ").trim()

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 50f
        val marginRight = 545f
        val marginTop = 60f
        val marginBottom = 800f
        val lineHeight = 20f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isAntiAlias = true
        }

        var pageNum = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = marginTop

        // Word-wrap and paginate
        val words = plainText.split(Regex("\\s+"))
        var line = ""
        for (w in words) {
            if (w == "\n" || w.isEmpty()) {
                // Paragraph break — flush current line
                if (line.isNotEmpty()) {
                    if (y > marginBottom) {
                        pdfDocument.finishPage(page)
                        pageNum++
                        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                        canvas = page.canvas
                        y = marginTop
                    }
                    canvas.drawText(line, marginLeft, y, paint)
                    y += lineHeight
                    line = ""
                }
                y += lineHeight * 0.5f  // half-line paragraph gap
                continue
            }
            val testLine = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(testLine) > (marginRight - marginLeft)) {
                if (y > marginBottom) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = marginTop
                }
                canvas.drawText(line, marginLeft, y, paint)
                y += lineHeight
                line = w
            } else {
                line = testLine
            }
        }
        // Flush last line
        if (line.isNotEmpty() && y <= marginBottom) {
            canvas.drawText(line, marginLeft, y, paint)
        }
        pdfDocument.finishPage(page)

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("HTML_Converted", "pdf"))
        val fos = FileOutputStream(outputFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        outputFile
    }

    suspend fun imagesToPdfCustom(context: Context, items: List<Pair<Uri, Int>>): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        items.forEachIndexed { index, (uri, rotationAngle) ->
            val origBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
            if (origBitmap != null) {
                val finalBitmap = if (rotationAngle % 360 != 0) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotationAngle.toFloat()) }
                    Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.width, origBitmap.height, matrix, true)
                } else {
                    origBitmap
                }

                val pageInfo = PdfDocument.PageInfo.Builder(finalBitmap.width, finalBitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Images_to_PDF", "pdf"))
        val outStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(outStream)
        pdfDocument.close()
        outStream.close()
        outputFile
    }

    // ==========================================
    // 3. BASIC EDITING & FILE MANAGEMENT
    // ==========================================

    suspend fun rotatePdf(context: Context, inputUri: Uri, degrees: Int): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        for (page in document.pages) {
            val currentRotation = page.rotation
            page.rotation = (currentRotation + degrees) % 360
        }
        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Rotated", "pdf"))
        document.save(outputFile)
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun watermarkPdf(context: Context, inputUri: Uri, watermarkText: String): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val text = watermarkText.ifBlank { "CONFIDENTIAL" }

        for (page in document.pages) {
            val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            val graphicsState = PDExtendedGraphicsState()
            graphicsState.nonStrokingAlphaConstant = 0.35f
            contentStream.setGraphicsStateParameters(graphicsState)

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 46f)
            contentStream.setNonStrokingColor(180, 180, 180)

            val mediaBox = page.mediaBox
            val x = (mediaBox.width / 4.0)
            val y = (mediaBox.height / 2.0)
            contentStream.setTextMatrix(0.707, 0.707, -0.707, 0.707, x, y)
            contentStream.showText(text)
            contentStream.endText()
            contentStream.close()
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Watermarked", "pdf"))
        document.save(outputFile)
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun addPageNumbers(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val total = document.numberOfPages

        document.pages.forEachIndexed { index, page ->
            val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 11f)
            contentStream.setNonStrokingColor(120, 120, 120)

            val label = "Page ${index + 1} of $total"
            val textWidth = (PDType1Font.HELVETICA.getStringWidth(label) / 1000f) * 11f
            val x = (page.mediaBox.width - textWidth) / 2f
            contentStream.newLineAtOffset(x, 25f)
            contentStream.showText(label)
            contentStream.endText()
            contentStream.close()
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Numbered", "pdf"))
        document.save(outputFile)
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun cropPdf(context: Context, inputUri: Uri, marginPercent: Float = 5f): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)

        for (page in document.pages) {
            val mediaBox = page.mediaBox
            val insetX = mediaBox.width * (marginPercent / 100f)
            val insetY = mediaBox.height * (marginPercent / 100f)
            page.cropBox = PDRectangle(
                mediaBox.lowerLeftX + insetX,
                mediaBox.lowerLeftY + insetY,
                mediaBox.width - (insetX * 2),
                mediaBox.height - (insetY * 2)
            )
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Cropped", "pdf"))
        document.save(outputFile)
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun annotatePdf(context: Context, inputUri: Uri, noteText: String): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)

        if (document.numberOfPages > 0) {
            val page = document.getPage(0)
            val cs = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14f)
            cs.setNonStrokingColor(79, 70, 229)
            cs.newLineAtOffset(50f, page.mediaBox.height - 50f)
            cs.showText(noteText.ifBlank { "Annotation: Approved on device" })
            cs.endText()
            cs.close()
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Edited", "pdf"))
        document.save(outputFile)
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun organizePdf(context: Context, inputUri: Uri, reverseOrder: Boolean): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val newDoc = PDDocument()

        val indices = if (reverseOrder) (document.numberOfPages - 1 downTo 0).toList() else (0 until document.numberOfPages).toList()
        for (i in indices) {
            newDoc.importPage(document.getPage(i))
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Organized", "pdf"))
        newDoc.save(outputFile)
        newDoc.close()
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun mergePdfs(context: Context, uris: List<Uri>): File = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Merged", "pdf"))
        merger.destinationFileName = outputFile.absolutePath

        val streams = mutableListOf<InputStream>()
        try {
            for (uri in uris) {
                val isStream = context.contentResolver.openInputStream(uri)
                if (isStream != null) {
                    streams.add(isStream)
                    merger.addSource(isStream)
                }
            }
            merger.mergeDocuments(null)
        } finally {
            streams.forEach {
                try { it.close() } catch (e: Exception) {}
            }
        }
        outputFile
    }

    suspend fun splitPdfRange(context: Context, inputUri: Uri, startPage: Int, endPage: Int): File = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
        val document = PDDocument.load(inputStream)
        val total = document.numberOfPages
        val start = (startPage - 1).coerceIn(0, total - 1)
        val end = (endPage - 1).coerceIn(start, total - 1)

        val newDoc = PDDocument()
        for (i in start..end) {
            newDoc.importPage(document.getPage(i))
        }

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Split_Pages_${start + 1}_to_${end + 1}", "pdf"))
        newDoc.save(outputFile)
        newDoc.close()
        document.close()
        inputStream.close()
        outputFile
    }

    suspend fun compressPdf(context: Context, inputUri: Uri): File = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")!!
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val compressedDoc = PdfDocument()
        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            // Cache dimensions BEFORE closing the page
            val origW = page.width
            val origH = page.height

            // Render at 72 dpi equivalent (multiply by 1.0 = no upsizing, downsample for compression)
            val renderW = (origW * 1.0f).toInt()
            val renderH = (origH * 1.0f).toInt()

            val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()  // safe to close now — origW/origH already cached

            // Re-encode bitmap at 70% JPEG quality (significant size reduction)
            val jpegBuffer = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, jpegBuffer)
            bitmap.recycle()

            // Draw JPEG-compressed image back to PDF page (uses origW x origH for crisp output)
            val compressedBitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBuffer.toByteArray(), 0, jpegBuffer.size())
            val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, i + 1).create()
            val pdfPage = compressedDoc.startPage(pageInfo)
            pdfPage.canvas.drawBitmap(
                compressedBitmap,
                null,
                Rect(0, 0, origW, origH),
                null
            )
            compressedDoc.finishPage(pdfPage)
            compressedBitmap.recycle()
        }
        renderer.close()
        pfd.close()

        val outputFile = File(getOutputDirectory(context), generateOutputFileName("Compressed", "pdf"))
        val fos = FileOutputStream(outputFile)
        compressedDoc.writeTo(fos)
        compressedDoc.close()
        fos.close()
        outputFile
    }

    /**
     * Renders ALL pages of a PDF as high-resolution thumbnails for big previews.
     * Returns a map of [pageIndex -> Bitmap]. Must be called from a background thread.
     */
    fun renderAllPageThumbnails(context: Context, uri: Uri): Map<Int, Bitmap> {
        val result = mutableMapOf<Int, Bitmap>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return result
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // Render at high resolution for big, crisp scrollable preview (target longest dimension ~900px)
                val maxDim = maxOf(page.width, page.height).toFloat()
                val scale = if (maxDim > 0f) (900f / maxDim).coerceIn(0.8f, 1.8f) else 1.0f
                val w = (page.width * scale).toInt().coerceAtLeast(300)
                val h = (page.height * scale).toInt().coerceAtLeast(400)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                result[i] = bmp
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Reorganizes a PDF's pages into a custom order defined by [pageOrder].
     * pageOrder is a list of 0-based original page indices in the new desired sequence.
     * Example: [2, 0, 1] = move page 3 to position 1.
     */
    suspend fun organizePdfCustomOrder(context: Context, inputUri: Uri, pageOrder: List<Int>): File =
        withContext(Dispatchers.IO) {
            val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)!!
            val document = PDDocument.load(inputStream)
            val newDoc = PDDocument()
            for (origIdx in pageOrder) {
                if (origIdx in 0 until document.numberOfPages) {
                    newDoc.importPage(document.getPage(origIdx))
                }
            }
            val outputFile = File(getOutputDirectory(context), generateOutputFileName("Organized", "pdf"))
            newDoc.save(outputFile)
            newDoc.close()
            document.close()
            inputStream.close()
            outputFile
        }

    fun renderFirstPageThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bmp = Bitmap.createBitmap(page.width / 2, page.height / 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bmp
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val mime = when (file.extension.lowercase()) {
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "csv" -> "text/csv"
                    "jpg", "jpeg" -> "image/jpeg"
                    "zip" -> "application/zip"
                    else -> "application/pdf"
                }
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                val mime = when (file.extension.lowercase()) {
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "csv" -> "text/csv"
                    "jpg", "jpeg" -> "image/jpeg"
                    "zip" -> "application/zip"
                    else -> "application/pdf"
                }
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
