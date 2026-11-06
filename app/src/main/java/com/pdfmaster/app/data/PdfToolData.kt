package com.pdfmaster.app.data

data class PdfTool(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val badge: String? = null,
    val shortSummary: String,
    val capabilities: List<String>,
    val inputFormats: String,
    val outputFormat: String,
    val offlineProcessingDetails: String
)

data class ToolSection(
    val sectionTitle: String,
    val sectionDescription: String,
    val tools: List<PdfTool>
)

data class MainCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val sections: List<ToolSection>
)

object PdfToolkitCatalog {

    val categories: List<MainCategory> = listOf(
        // ==========================================
        // 1. CONVERT PDFS
        // ==========================================
        MainCategory(
            id = "convert_pdfs",
            title = "Convert PDFs",
            emoji = "📄",
            subtitle = "Convert PDFs to Office/Image formats or create PDFs from other files",
            sections = listOf(
                ToolSection(
                    sectionTitle = "PDF to Other Formats",
                    sectionDescription = "Convert PDF documents into editable Word, PPT, Excel, or multi-page JPG images",
                    tools = listOf(
                        PdfTool(
                            id = "pdf_to_word",
                            title = "PDF to Word",
                            iconEmoji = "📝",
                            badge = "DOCX",
                            shortSummary = "Converts PDF pages into genuine editable Microsoft Word (.docx) documents.",
                            capabilities = listOf(
                                "Extracts paragraphs, text formatting, and line structures faithfully",
                                "Generates valid OpenXML Word (.docx) file package",
                                "Opens cleanly in MS Word, Google Docs, WPS, and LibreOffice",
                                "100% offline with zero cloud upload"
                            ),
                            inputFormats = "PDF documents (.pdf)",
                            outputFormat = "Microsoft Word Document (.docx)",
                            offlineProcessingDetails = "Parses PDF stream and synthesizes genuine OpenXML DOCX archive on device."
                        ),
                        PdfTool(
                            id = "pdf_to_powerpoint",
                            title = "PDF to PowerPoint",
                            iconEmoji = "📊",
                            badge = "PPTX",
                            shortSummary = "Transforms each PDF page into a presentation slide (.pptx).",
                            capabilities = listOf(
                                "Converts each PDF page into a dedicated slide canvas",
                                "Generates genuine OpenXML PowerPoint presentation (.pptx)",
                                "Opens cleanly in MS PowerPoint, Google Slides, and Keynote",
                                "100% offline generation"
                            ),
                            inputFormats = "PDF documents (.pdf)",
                            outputFormat = "PowerPoint Presentation (.pptx)",
                            offlineProcessingDetails = "Builds OpenXML PPTX presentation package containing rendered slide shapes."
                        ),
                        PdfTool(
                            id = "pdf_to_excel",
                            title = "PDF to Excel",
                            iconEmoji = "📈",
                            badge = "XLSX / CSV",
                            shortSummary = "Detects tabular rows and numbers, exporting directly to Excel spreadsheets.",
                            capabilities = listOf(
                                "Extracts table lines and cell columns from PDF content",
                                "Exports into spreadsheet format ready for calculations",
                                "Opens directly in Microsoft Excel and Google Sheets",
                                "Processed completely on your phone"
                            ),
                            inputFormats = "PDF documents (.pdf)",
                            outputFormat = "Excel Spreadsheet (.xlsx / .csv)",
                            offlineProcessingDetails = "Parses spatial text coordinates and compiles spreadsheet grid locally."
                        ),
                        PdfTool(
                            id = "pdf_to_jpg",
                            title = "PDF to JPG",
                            iconEmoji = "🖼️",
                            badge = "All Pages",
                            shortSummary = "Converts ALL pages of your PDF into high-resolution JPG pictures.",
                            capabilities = listOf(
                                "Iterates through all pages in the PDF automatically",
                                "Renders each page into a separate high-resolution image",
                                "Option to view individual pages or share all at once",
                                "Uses Android hardware-accelerated PdfRenderer"
                            ),
                            inputFormats = "PDF documents (.pdf)",
                            outputFormat = "Separate JPG image files (Page 1, 2...)",
                            offlineProcessingDetails = "Renders every page via Android OS PdfRenderer graphics engine."
                        )
                    )
                ),
                ToolSection(
                    sectionTitle = "Other Formats to PDF",
                    sectionDescription = "Create clean PDF documents from files, pictures, or web markup",
                    tools = listOf(
                        PdfTool(
                            id = "word_to_pdf",
                            title = "Word to PDF",
                            iconEmoji = "📄",
                            badge = "DOCX to PDF",
                            shortSummary = "Converts Word documents (.docx) into clean, standard PDF documents.",
                            capabilities = listOf(
                                "Parses Word OpenXML paragraphs, styles, and headers",
                                "Renders text cleanly onto standard A4 PDF pages",
                                "Eliminates formatting shifts across different devices",
                                "Compiles entirely offline on your phone"
                            ),
                            inputFormats = "Word documents (.docx)",
                            outputFormat = "Standard PDF (.pdf)",
                            offlineProcessingDetails = "Extracts document XML tokens and renders with Android native PdfDocument."
                        ),
                        PdfTool(
                            id = "powerpoint_to_pdf",
                            title = "PowerPoint to PDF",
                            iconEmoji = "📽️",
                            badge = "PPTX to PDF",
                            shortSummary = "Exports PowerPoint presentations (.pptx) into presentation PDF decks.",
                            capabilities = listOf(
                                "Extracts slides and titles from PPTX packages",
                                "Renders each slide onto landscape PDF page canvases",
                                "Ideal for creating printable handouts and slide notes",
                                "Works completely without internet"
                            ),
                            inputFormats = "PowerPoint presentations (.pptx)",
                            outputFormat = "Presentation PDF (.pdf)",
                            offlineProcessingDetails = "Draws slide canvases into vector PDF pages."
                        ),
                        PdfTool(
                            id = "excel_to_pdf",
                            title = "Excel to PDF",
                            iconEmoji = "📊",
                            badge = "XLSX to PDF",
                            shortSummary = "Converts spreadsheet tables (.xlsx, .csv) into auto-fitted PDF reports.",
                            capabilities = listOf(
                                "Parses workbook data, rows, and columns",
                                "Auto-fits tabular data onto clean printable pages",
                                "Draws clear grid borders and table headers",
                                "Maintains neat column alignment"
                            ),
                            inputFormats = "Spreadsheets (.xlsx, .csv)",
                            outputFormat = "Formatted Table PDF (.pdf)",
                            offlineProcessingDetails = "Local table layout paginator draws spreadsheet rows directly onto PDF canvas."
                        ),
                        PdfTool(
                            id = "jpg_to_pdf",
                            title = "JPG to PDF",
                            iconEmoji = "📷",
                            badge = "Interactive Preview",
                            shortSummary = "Packages photos into a PDF with thumbnail preview, page reordering & rotation.",
                            capabilities = listOf(
                                "Interactive visual preview of all selected images",
                                "Rotate individual images before generating",
                                "Move/swap image order effortlessly",
                                "Generates clean consolidated PDF in your exact order"
                            ),
                            inputFormats = "Images (.jpg, .jpeg, .png, .webp)",
                            outputFormat = "Consolidated PDF (.pdf)",
                            offlineProcessingDetails = "Encodes image streams into custom PDF page dimensions."
                        ),
                        PdfTool(
                            id = "html_to_pdf",
                            title = "HTML to PDF",
                            iconEmoji = "🌐",
                            badge = "Web to PDF",
                            shortSummary = "Converts HTML documents, code, or offline web content into printable PDFs.",
                            capabilities = listOf(
                                "Parses HTML tags, typography, and page sections",
                                "Formats headings, paragraphs, and list items into pages",
                                "Clean A4 printable layout",
                                "No internet connection needed"
                            ),
                            inputFormats = "HTML files (.html, .htm) or text",
                            outputFormat = "Printable PDF (.pdf)",
                            offlineProcessingDetails = "HTML parser compiles layout into PDF pages."
                        )
                    )
                )
            )
        ),

        // ==========================================
        // 2. EDIT & MANAGE PDFS
        // ==========================================
        MainCategory(
            id = "edit_manage_pdfs",
            title = "Edit & Manage PDFs",
            emoji = "🛠️",
            subtitle = "Edit, organize, rotate, crop, number, watermark, merge, split, and compress PDFs",
            sections = listOf(
                ToolSection(
                    sectionTitle = "Basic Editing",
                    sectionDescription = "Rotate, watermark, add page numbers, crop, or annotate PDF pages",
                    tools = listOf(
                        PdfTool(
                            id = "edit_pdf",
                            title = "Edit PDF",
                            iconEmoji = "✏️",
                            badge = "Annotate",
                            shortSummary = "Add custom text annotations, notes, and visual stamps on your PDF.",
                            capabilities = listOf(
                                "Insert custom text notes with configurable font size & color",
                                "Select which page to place your annotation on",
                                "Non-destructive overlay preserving original text and graphics",
                                "Saves valid, uncorrupted PDF document"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Annotated PDF (.pdf)",
                            offlineProcessingDetails = "Draws vector text streams onto PDF page dictionary."
                        ),
                        PdfTool(
                            id = "organize_pdf",
                            title = "Organize PDF",
                            iconEmoji = "🗂️",
                            badge = "Reorder & Delete",
                            shortSummary = "Delete unwanted pages and reorder page sequences in any PDF.",
                            capabilities = listOf(
                                "Shows page count and allows selecting pages to keep or remove",
                                "Reverse or rearrange page sequence",
                                "Generates clean restructured PDF",
                                "Operates directly on PDF page tree without corruption"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Reorganized PDF (.pdf)",
                            offlineProcessingDetails = "Re-indexes PDF catalog page tree entries cleanly."
                        ),
                        PdfTool(
                            id = "rotate_pdf",
                            title = "Rotate PDF",
                            iconEmoji = "🔄",
                            badge = "Visual Preview",
                            shortSummary = "Rotate all pages or individual pages with live preview.",
                            capabilities = listOf(
                                "Shows live page thumbnail preview before saving",
                                "Rotate 90°, 180°, or 270° clockwise",
                                "Apply rotation to entire document or specific pages",
                                "Universal PDF reader compatibility"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Correctly Oriented PDF (.pdf)",
                            offlineProcessingDetails = "Updates /Rotate attributes in PDF page dictionary."
                        ),
                        PdfTool(
                            id = "crop_pdf",
                            title = "Crop PDF",
                            iconEmoji = "✂️",
                            badge = "Trim Margins",
                            shortSummary = "Trim unwanted outer white borders and margins from PDF pages.",
                            capabilities = listOf(
                                "Adjusts page CropBox margins evenly or by custom margins",
                                "Removes scanner bleed and excess white space",
                                "Maintains original document resolution and quality",
                                "Compiles a clean valid PDF"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Trimmed PDF (.pdf)",
                            offlineProcessingDetails = "Recalculates and updates PDF /CropBox coordinates."
                        ),
                        PdfTool(
                            id = "page_numbers",
                            title = "Page Numbers",
                            iconEmoji = "🔢",
                            badge = "Header/Footer",
                            shortSummary = "Stamp page numbers (e.g. 'Page 1 of 5') on every page.",
                            capabilities = listOf(
                                "Inserts 'Page X of Y' centered at the bottom of each page",
                                "Crisp Helvetica typography overlay",
                                "Applies across all pages automatically",
                                "Saves valid, fully readable PDF"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Numbered PDF (.pdf)",
                            offlineProcessingDetails = "Appends page number text streams to every PDPage."
                        ),
                        PdfTool(
                            id = "watermark",
                            title = "Watermark",
                            iconEmoji = "💧",
                            badge = "Diagonal Stamp",
                            shortSummary = "Stamp diagonal watermark text (CONFIDENTIAL, DRAFT) across pages.",
                            capabilities = listOf(
                                "Customizable watermark text",
                                "Diagonal rotation with semi-transparent opacity",
                                "Applies to every page in the document",
                                "Completely offline and privacy-safe"
                            ),
                            inputFormats = "Existing PDF (.pdf)",
                            outputFormat = "Watermarked PDF (.pdf)",
                            offlineProcessingDetails = "Appends alpha-masked text streams to PDF content."
                        )
                    )
                ),
                ToolSection(
                    sectionTitle = "File Management",
                    sectionDescription = "Merge multiple PDFs, split documents into ranges, or compress file size",
                    tools = listOf(
                        PdfTool(
                            id = "merge_pdf",
                            title = "Merge PDF",
                            iconEmoji = "📑",
                            badge = "Combine Files",
                            shortSummary = "Combine multiple PDF documents into a single unified file.",
                            capabilities = listOf(
                                "Batch select 2 or more PDF files from your phone",
                                "Combines all pages in sequential order",
                                "Fast on-device merging without file size limits",
                                "Zero data leaves your phone"
                            ),
                            inputFormats = "Two or more PDF files (.pdf)",
                            outputFormat = "Merged Single PDF (.pdf)",
                            offlineProcessingDetails = "Clones page objects with PDFMergerUtility."
                        ),
                        PdfTool(
                            id = "split_pdf",
                            title = "Split PDF",
                            iconEmoji = "✂️",
                            badge = "Extract Pages",
                            shortSummary = "Extract specific page ranges or burst document into individual pages.",
                            capabilities = listOf(
                                "Choose custom page range (e.g. 1-3 or 2-5)",
                                "Option to extract only selected pages into a new PDF",
                                "Generates clean, valid extracted PDF",
                                "Leaves original document intact"
                            ),
                            inputFormats = "Multi-page PDF (.pdf)",
                            outputFormat = "Extracted PDF (.pdf)",
                            offlineProcessingDetails = "Extracts and imports selected PDPages into a new clean PDDocument."
                        ),
                        PdfTool(
                            id = "compress_pdf",
                            title = "Compress PDF",
                            iconEmoji = "🗜️",
                            badge = "Reduce Size",
                            shortSummary = "Optimizes PDF file size for fast emailing and sharing.",
                            capabilities = listOf(
                                "Optimizes and compresses page streams",
                                "Downsamples oversized embedded bitmaps",
                                "Strips unused metadata and redundant objects",
                                "Creates a valid, lightweight PDF"
                            ),
                            inputFormats = "Large PDF file (.pdf)",
                            outputFormat = "Compressed PDF (.pdf)",
                            offlineProcessingDetails = "Re-encodes content streams and renders optimized pages via PdfRenderer."
                        )
                    )
                )
            )
        )
    )

    fun findToolById(id: String): PdfTool? {
        return categories.flatMap { it.sections }.flatMap { it.tools }.find { it.id == id }
    }
}
