package com.pdfmaster.app.data

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object OpenXmlHelper {

    /**
     * Creates a 100% valid Microsoft Word (.docx) file from extracted text lines.
     * Compatible with MS Word, Google Docs, WPS Office, and LibreOffice.
     * Includes the required word/_rels/document.xml.rels so Word won't reject it.
     */
    fun createDocxFromText(lines: List<String>, outputFile: File) {
        val zos = ZipOutputStream(FileOutputStream(outputFile))

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
    <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
    <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
</Types>""".trimIndent()
        zos.write(contentTypes.toByteArray())
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
        zos.write(rootRels.toByteArray())
        zos.closeEntry()

        // 3. word/_rels/document.xml.rels  ← REQUIRED; Word refuses to open without this
        zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
</Relationships>""".trimIndent()
        zos.write(docRels.toByteArray())
        zos.closeEntry()

        // 4. word/styles.xml  — minimal default styles
        zos.putNextEntry(ZipEntry("word/styles.xml"))
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
          xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml">
    <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
        <w:name w:val="Normal"/>
        <w:rPr><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>
    </w:style>
</w:styles>""".trimIndent()
        zos.write(stylesXml.toByteArray())
        zos.closeEntry()

        // 5. word/settings.xml — minimal settings
        zos.putNextEntry(ZipEntry("word/settings.xml"))
        val settingsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:defaultTabStop w:val="720"/>
</w:settings>""".trimIndent()
        zos.write(settingsXml.toByteArray())
        zos.closeEntry()

        // 6. word/document.xml
        zos.putNextEntry(ZipEntry("word/document.xml"))
        val docSb = StringBuilder()
        docSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>""")

        for (line in lines) {
            val sanitized = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").trim()
            if (sanitized.isNotEmpty()) {
                docSb.append("""
        <w:p>
            <w:pPr><w:pStyle w:val="Normal"/></w:pPr>
            <w:r>
                <w:rPr><w:sz w:val="24"/></w:rPr>
                <w:t xml:space="preserve">$sanitized</w:t>
            </w:r>
        </w:p>""")
            } else {
                docSb.append("\n        <w:p><w:pPr><w:pStyle w:val=\"Normal\"/></w:pPr></w:p>")
            }
        }

        docSb.append("""
        <w:sectPr>
            <w:pgSz w:w="11906" w:h="16838"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>""")
        zos.write(docSb.toString().toByteArray())
        zos.closeEntry()

        zos.close()
    }

    /**
     * Extracts plain text paragraphs from a Word (.docx) document.
     */
    fun extractTextFromDocx(inputStream: InputStream): List<String> {
        val lines = mutableListOf<String>()
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xml = zis.bufferedReader().readText()
                // Extract text from <w:t>...</w:t> tags
                val pattern = Regex("<w:t[^>]*>(.*?)</w:t>")
                val paragraphs = xml.split("</w:p>")
                for (p in paragraphs) {
                    val pMatches = pattern.findAll(p)
                    val pText = pMatches.map { it.groupValues[1] }.joinToString("")
                    val cleanText = pText.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").trim()
                    if (cleanText.isNotEmpty()) {
                        lines.add(cleanText)
                    }
                }
                break
            }
            entry = zis.nextEntry
        }
        zis.close()
        return lines
    }

    /**
     * Creates a PowerPoint (.pptx) with one proper slide per page of text.
     * All slides are listed in ContentTypes, presentation.xml sldIdLst, and rels.
     */
    fun createPptxFromSlides(slidesText: List<String>, outputFile: File) {
        val slides = if (slidesText.isEmpty()) listOf("Slide 1") else slidesText
        val n = slides.size
        val zos = ZipOutputStream(FileOutputStream(outputFile))

        // 1. [Content_Types].xml — declare every slide
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        val overrides = StringBuilder()
        for (i in 1..n) {
            overrides.append("""    <Override PartName="/ppt/slides/slide$i.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
            overrides.append("\n")
        }
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
$overrides</Types>""".trimIndent()
        zos.write(contentTypes.toByteArray())
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""".trimIndent().toByteArray())
        zos.closeEntry()

        // 3. ppt/presentation.xml — list all slide IDs
        zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
        val sldIdLst = StringBuilder()
        for (i in 1..n) {
            sldIdLst.append("""        <p:sldId id="${255 + i}" r:id="rId$i"/>""").append("\n")
        }
        val presentation = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
    <p:sldMasterIdLst/>
    <p:sldIdLst>
$sldIdLst    </p:sldIdLst>
    <p:sldSz cx="9144000" cy="6858000" type="screen4x3"/>
    <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>""".trimIndent()
        zos.write(presentation.toByteArray())
        zos.closeEntry()

        // 4. ppt/_rels/presentation.xml.rels — relationship to each slide
        zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
        val presRelsContent = StringBuilder()
        for (i in 1..n) {
            presRelsContent.append("""    <Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$i.xml"/>""").append("\n")
        }
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$presRelsContent</Relationships>""".trimIndent().toByteArray())
        zos.closeEntry()

        // 5. Write each slide XML and its .rels
        for ((idx, text) in slides.withIndex()) {
            val slideNum = idx + 1
            val safeText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

            // Split text into lines for multi-line body
            val bodyLines = safeText.split("\n").take(20)  // cap at 20 lines per slide
            val paraXml = bodyLines.joinToString("\n") { line ->
                """                        <a:p><a:r><a:rPr lang="en-US" sz="1600"/><a:t>${line.take(90)}</a:t></a:r></a:p>"""
            }

            zos.putNextEntry(ZipEntry("ppt/slides/slide$slideNum.xml"))
            val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
    <p:cSld>
        <p:spTree>
            <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
            <p:grpSpPr/>
            <!-- Title -->
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Title $slideNum"/><p:cNvSpPr><a:spLocks noGrp="1"/></p:cNvSpPr><p:nvPr/></p:nvSpPr>
                <p:spPr><a:xfrm><a:off x="457200" y="274638"/><a:ext cx="8229600" cy="1143000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
                <p:txBody>
                    <a:bodyPr/><a:lstStyle/>
                    <a:p><a:r><a:rPr lang="en-US" sz="2800" b="1"/><a:t>Slide $slideNum</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <!-- Body -->
            <p:sp>
                <p:nvSpPr><p:cNvPr id="3" name="Content $slideNum"/><p:cNvSpPr><a:spLocks noGrp="1"/></p:cNvSpPr><p:nvPr/></p:nvSpPr>
                <p:spPr><a:xfrm><a:off x="457200" y="1600200"/><a:ext cx="8229600" cy="4525963"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
                <p:txBody>
                    <a:bodyPr wrap="square"><a:normAutofit/></a:bodyPr><a:lstStyle/>
$paraXml
                </p:txBody>
            </p:sp>
        </p:spTree>
    </p:cSld>
</p:sld>""".trimIndent()
            zos.write(slideXml.toByteArray())
            zos.closeEntry()

            // Each slide also needs a _rels file (even if empty)
            zos.putNextEntry(ZipEntry("ppt/slides/_rels/slide$slideNum.xml.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>""".trimIndent().toByteArray())
            zos.closeEntry()
        }

        zos.close()
    }

    /**
     * Extracts text lines grouped PER SLIDE from PowerPoint (.pptx) slides.
     * Returns a list where each element is the full text content of one slide.
     */
    fun extractTextFromPptx(inputStream: InputStream): List<String> {
        // Map: slideNumber -> text
        val slideMap = sortedMapOf<Int, StringBuilder>()
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        val pattern = Regex("<a:t[^>]*>(.*?)</a:t>")
        while (entry != null) {
            val name = entry.name
            if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                // Extract slide number from filename e.g. "ppt/slides/slide3.xml" -> 3
                val num = name.removePrefix("ppt/slides/slide").removeSuffix(".xml").toIntOrNull() ?: 999
                val xml = zis.bufferedReader().readText()
                val sb = StringBuilder()
                pattern.findAll(xml).forEach { match ->
                    val text = match.groupValues[1]
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").trim()
                    if (text.isNotEmpty()) {
                        sb.append(text).append("\n")
                    }
                }
                slideMap[num] = sb
            }
            entry = zis.nextEntry
        }
        zis.close()
        return slideMap.values.map { it.toString().trim() }
    }

    /**
     * Creates an Excel CSV spreadsheet from table lines.
     */
    fun createCsvSpreadsheet(rows: List<List<String>>, outputFile: File) {
        val sb = StringBuilder()
        for (row in rows) {
            val formattedRow = row.map { cell ->
                val escaped = cell.replace("\"", "\"\"")
                if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
                    "\"$escaped\""
                } else {
                    escaped
                }
            }.joinToString(",")
            sb.append(formattedRow).append("\r\n")
        }
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    /**
     * Extracts spreadsheet rows from Excel (.xlsx) file.
     */
    fun extractRowsFromXlsx(inputStream: InputStream): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()

        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        var sheetXml: String? = null

        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                val xml = zis.bufferedReader().readText()
                val pattern = Regex("<t[^>]*>(.*?)</t>")
                pattern.findAll(xml).forEach { match ->
                    sharedStrings.add(match.groupValues[1].replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">"))
                }
            } else if (entry.name == "xl/worksheets/sheet1.xml") {
                sheetXml = zis.bufferedReader().readText()
            }
            entry = zis.nextEntry
        }
        zis.close()

        if (sheetXml != null) {
            val rowPattern = Regex("<row[^>]*>(.*?)</row>")
            val cellPattern = Regex("<c[^>]*t=\"(s?)\"[^>]*><v>(.*?)</v></c>")

            rowPattern.findAll(sheetXml).forEach { rMatch ->
                val rowContent = rMatch.groupValues[1]
                val rowCells = mutableListOf<String>()
                cellPattern.findAll(rowContent).forEach { cMatch ->
                    val isShared = cMatch.groupValues[1] == "s"
                    val value = cMatch.groupValues[2]
                    if (isShared) {
                        val idx = value.toIntOrNull()
                        if (idx != null && idx < sharedStrings.size) {
                            rowCells.add(sharedStrings[idx])
                        } else {
                            rowCells.add(value)
                        }
                    } else {
                        rowCells.add(value)
                    }
                }
                if (rowCells.isNotEmpty()) {
                    rows.add(rowCells)
                }
            }
        }
        return rows
    }
}
