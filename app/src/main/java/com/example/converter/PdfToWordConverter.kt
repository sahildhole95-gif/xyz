package com.example.converter

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExtractedPage(
    val pageNumber: Int,
    val paragraphs: List<ExtractedParagraph>,
    val images: List<ExtractedImage>
)

data class ExtractedParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val isBullet: Boolean = false
)

data class ExtractedImage(
    val fileName: String,
    val rId: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int
)

data class PdfConversionResult(
    val totalPages: Int,
    val paragraphCount: Int,
    val imageCount: Int,
    val outputFile: File
)

object PdfToWordConverter {

    fun convertPdfToDocx(
        context: Context,
        pdfFile: File,
        outputDocxFile: File
    ): PdfConversionResult {
        // Ensure PDFBox is initialized
        try {
            PDFBoxResourceLoader.init(context)
        } catch (_: Exception) {}

        val extractedPages = mutableListOf<ExtractedPage>()
        var globalImageIdx = 1
        var totalParagraphs = 0
        var totalImages = 0
        var pageCount = 0

        PDDocument.load(pdfFile).use { document ->
            pageCount = document.numberOfPages

            for (pageIndex in 0 until pageCount) {
                val pageNumber = pageIndex + 1
                val pdPage = document.getPage(pageIndex)

                // 1. Extract text for this page
                val stripper = PDFTextStripper().apply {
                    startPage = pageNumber
                    endPage = pageNumber
                }
                val rawText = try {
                    stripper.getText(document)
                } catch (e: Exception) {
                    ""
                }

                val pageParagraphs = parseTextIntoParagraphs(rawText)
                totalParagraphs += pageParagraphs.size

                // 2. Extract images from page resources
                val pageImages = mutableListOf<ExtractedImage>()
                try {
                    val resources = pdPage.resources
                    if (resources != null) {
                        for (xName in resources.xObjectNames) {
                            if (resources.isImageXObject(xName)) {
                                val pdImage = resources.getXObject(xName) as? PDImageXObject
                                if (pdImage != null) {
                                    val bmp: Bitmap? = try {
                                        pdImage.image
                                    } catch (_: Exception) {
                                        null
                                    }
                                    if (bmp != null) {
                                        val baos = ByteArrayOutputStream()
                                        bmp.compress(Bitmap.CompressFormat.PNG, 95, baos)
                                        val bytes = baos.toByteArray()
                                        val imgName = "image$globalImageIdx.png"
                                        val rId = "rIdImg$globalImageIdx"
                                        globalImageIdx++
                                        pageImages.add(
                                            ExtractedImage(
                                                fileName = imgName,
                                                rId = rId,
                                                bytes = bytes,
                                                width = bmp.width,
                                                height = bmp.height
                                            )
                                        )
                                        totalImages++
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                extractedPages.add(
                    ExtractedPage(
                        pageNumber = pageNumber,
                        paragraphs = pageParagraphs,
                        images = pageImages
                    )
                )
            }
        }

        // Build DOCX archive
        buildDocxFile(extractedPages, outputDocxFile)

        return PdfConversionResult(
            totalPages = pageCount,
            paragraphCount = totalParagraphs,
            imageCount = totalImages,
            outputFile = outputDocxFile
        )
    }

    private fun parseTextIntoParagraphs(rawText: String): List<ExtractedParagraph> {
        val result = mutableListOf<ExtractedParagraph>()
        val lines = rawText.split("\n", "\r\n")
        val currentParagraphBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (currentParagraphBuilder.isNotEmpty()) {
                    val text = currentParagraphBuilder.toString().trim()
                    val isHeading = detectHeading(text)
                    val isBullet = detectBullet(text)
                    result.add(ExtractedParagraph(text = text, isHeading = isHeading, isBullet = isBullet))
                    currentParagraphBuilder.clear()
                }
            } else {
                if (currentParagraphBuilder.isEmpty()) {
                    currentParagraphBuilder.append(trimmed)
                } else {
                    // Check if line starts with bullet or appears to be a new list item
                    if (detectBullet(trimmed)) {
                        val text = currentParagraphBuilder.toString().trim()
                        result.add(ExtractedParagraph(text = text, isHeading = detectHeading(text), isBullet = detectBullet(text)))
                        currentParagraphBuilder.clear()
                        currentParagraphBuilder.append(trimmed)
                    } else {
                        currentParagraphBuilder.append(" ").append(trimmed)
                    }
                }
            }
        }

        if (currentParagraphBuilder.isNotEmpty()) {
            val text = currentParagraphBuilder.toString().trim()
            result.add(ExtractedParagraph(text = text, isHeading = detectHeading(text), isBullet = detectBullet(text)))
        }

        if (result.isEmpty()) {
            result.add(ExtractedParagraph(text = "Extracted document content from PDF."))
        }

        return result
    }

    private fun detectHeading(text: String): Boolean {
        if (text.length in 3..60) {
            val isUpper = text.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ":-." }
            val hasHeadingPrefix = text.startsWith("Chapter", ignoreCase = true) ||
                    text.startsWith("Section", ignoreCase = true) ||
                    text.startsWith("Part", ignoreCase = true)
            return isUpper || hasHeadingPrefix
        }
        return false
    }

    private fun detectBullet(text: String): Boolean {
        return text.startsWith("•") ||
                text.startsWith("- ") ||
                text.startsWith("* ") ||
                (text.length > 2 && text[0].isDigit() && text[1] == '.')
    }

    private fun buildDocxFile(pages: List<ExtractedPage>, outputFile: File) {
        val allImages = pages.flatMap { it.images }

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(generateContentTypesXml(allImages).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(generateRootRelsXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(generateDocumentRelsXml(allImages).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. word/styles.xml
            zos.putNextEntry(ZipEntry("word/styles.xml"))
            zos.write(generateStylesXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. word/settings.xml
            zos.putNextEntry(ZipEntry("word/settings.xml"))
            zos.write(generateSettingsXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 6. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(generateDocumentXml(pages).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 7. Write embedded images to word/media/
            for (img in allImages) {
                zos.putNextEntry(ZipEntry("word/media/${img.fileName}"))
                zos.write(img.bytes)
                zos.closeEntry()
            }
        }
    }

    private fun generateContentTypesXml(images: List<ExtractedImage>): String {
        val imageExtensions = if (images.isNotEmpty()) {
            """<Default Extension="png" ContentType="image/png"/>"""
        } else ""

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  $imageExtensions
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
</Types>"""
    }

    private fun generateRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun generateDocumentRelsXml(images: List<ExtractedImage>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rIdSettings" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
""")
        for (img in images) {
            sb.append("""  <Relationship Id="${img.rId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${img.fileName}"/>
""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun generateStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:color w:val="222222"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="32"/>
      <w:color w:val="1E293B"/>
    </w:rPr>
  </w:style>
</w:styles>"""
    }

    private fun generateSettingsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:zoom w:percent="100"/>
</w:settings>"""
    }

    private fun generateDocumentXml(pages: List<ExtractedPage>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
""")

        for ((pageIndex, page) in pages.withIndex()) {
            // Write paragraphs
            for (p in page.paragraphs) {
                val cleanText = escapeXml(p.text)
                sb.append("    <w:p>\n")
                if (p.isHeading) {
                    sb.append("      <w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr>\n")
                    sb.append("      <w:r><w:rPr><w:b/><w:sz w:val=\"30\"/></w:rPr><w:t>$cleanText</w:t></w:r>\n")
                } else if (p.isBullet) {
                    sb.append("      <w:pPr><w:ind w:left=\"720\"/></w:pPr>\n")
                    sb.append("      <w:r><w:t>$cleanText</w:t></w:r>\n")
                } else {
                    sb.append("      <w:r><w:t xml:space=\"preserve\">$cleanText</w:t></w:r>\n")
                }
                sb.append("    </w:p>\n")
            }

            // Write embedded images if any
            for (img in page.images) {
                val cx = (img.width.coerceAtMost(550) * 9525).toLong() // 1 px ~ 9525 EMUs at 96 dpi
                val cy = (img.height.coerceAtMost(400) * 9525).toLong()

                sb.append("""    <w:p>
      <w:r>
        <w:drawing>
          <wp:inline distT="0" distB="0" distL="0" distR="0">
            <wp:extent cx="$cx" cy="$cy"/>
            <wp:docPr id="1" name="${img.fileName}"/>
            <a:graphic>
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:pic>
                  <pic:nvPicPr>
                    <pic:cNvPr id="0" name="${img.fileName}"/>
                    <pic:cNvPicPr/>
                  </pic:nvPicPr>
                  <pic:blipFill>
                    <a:blip r:embed="${img.rId}"/>
                    <a:stretch><a:fillRect/></a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm><a:off x="0" y="0"/><a:ext cx="$cx" cy="$cy"/></a:xfrm>
                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:inline>
        </w:drawing>
      </w:r>
    </w:p>
""")
            }

            // Page break between pages (except the last page)
            if (pageIndex < pages.size - 1) {
                sb.append("    <w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
            }
        }

        // Section properties (A4)
        sb.append("""    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
    </w:sectPr>
  </w:body>
</w:document>""")

        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
