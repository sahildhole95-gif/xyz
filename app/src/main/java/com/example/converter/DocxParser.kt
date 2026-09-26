package com.example.converter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

sealed class DocxElement {
    data class Paragraph(
        val runs: List<DocxRun>,
        val isHeading: Boolean = false,
        val headingLevel: Int = 1,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val isBullet: Boolean = false
    ) : DocxElement()

    data class Table(
        val rows: List<TableRow>
    ) : DocxElement()

    data class Image(
        val bitmap: Bitmap,
        val width: Int,
        val height: Int
    ) : DocxElement()

    object PageBreak : DocxElement()
}

enum class TextAlignment { LEFT, CENTER, RIGHT, JUSTIFY }

data class DocxRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val colorHex: String? = null,
    val fontSizePt: Float = 11f
)

data class TableRow(val cells: List<TableCell>)
data class TableCell(val text: String, val isHeader: Boolean = false)

data class ParsedDocx(
    val title: String,
    val elements: List<DocxElement>
)

object DocxParser {

    fun parse(file: File): ParsedDocx {
        val elements = mutableListOf<DocxElement>()
        val mediaMap = mutableMapOf<String, ByteArray>()
        val relsMap = mutableMapOf<String, String>() // rId -> target
        var documentXmlContent: String? = null

        // 1. Read ZIP entries
        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                when {
                    entry.name == "word/document.xml" -> {
                        documentXmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    }
                    entry.name == "word/_rels/document.xml.rels" -> {
                        val relsContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                        parseRels(relsContent, relsMap)
                    }
                    entry.name.startsWith("word/media/") -> {
                        val bytes = zipFile.getInputStream(entry).use { it.readBytes() }
                        mediaMap[entry.name] = bytes
                        // also map short name like "media/image1.png"
                        val shortName = entry.name.removePrefix("word/")
                        mediaMap[shortName] = bytes
                    }
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            // Fallback for non-zip or corrupt files
            return parseFallbackText(file)
        }

        if (documentXmlContent.isNullOrBlank()) {
            return parseFallbackText(file)
        }

        // 2. Parse document.xml
        parseDocumentXml(documentXmlContent, elements, relsMap, mediaMap)

        var title = file.nameWithoutExtension
        val firstHeading = elements.filterIsInstance<DocxElement.Paragraph>()
            .firstOrNull { it.isHeading || it.runs.any { r -> r.fontSizePt >= 14f } }
            ?.runs?.joinToString("") { it.text }?.trim()

        if (!firstHeading.isNullOrBlank()) {
            title = firstHeading
        }

        return ParsedDocx(title = title, elements = elements)
    }

    private fun parseFallbackText(file: File): ParsedDocx {
        val elements = mutableListOf<DocxElement>()
        try {
            val lines = file.readLines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val isHeading = trimmed.startsWith("#") || (trimmed.length < 50 && trimmed.all { it.isUpperCase() || it.isWhitespace() })
                val cleanText = trimmed.removePrefix("#").trim()
                elements.add(
                    DocxElement.Paragraph(
                        runs = listOf(DocxRun(text = cleanText, isBold = isHeading, fontSizePt = if (isHeading) 14f else 11f)),
                        isHeading = isHeading,
                        isBullet = trimmed.startsWith("-") || trimmed.startsWith("*")
                    )
                )
            }
        } catch (_: Exception) {
            elements.add(
                DocxElement.Paragraph(
                    runs = listOf(DocxRun(text = "Document content could not be read as text.")),
                    isHeading = false
                )
            )
        }
        return ParsedDocx(title = file.nameWithoutExtension, elements = elements)
    }

    private fun parseRels(xml: String, relsMap: MutableMap<String, String>) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
                        relsMap[id] = target
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun parseDocumentXml(
        xml: String,
        elements: MutableList<DocxElement>,
        relsMap: Map<String, String>,
        mediaMap: Map<String, ByteArray>
    ) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var insideParagraph = false
            var insideRun = false
            var insideTable = false
            var insideRow = false
            var insideCell = false

            var currentParagraphRuns = mutableListOf<DocxRun>()
            var isHeading = false
            var headingLevel = 1
            var isBullet = false
            var alignment = TextAlignment.LEFT

            var runBold = false
            var runItalic = false
            var runUnderline = false
            var runColor: String? = null
            var runFontSize = 11f
            val runTextBuilder = StringBuilder()

            var currentTableRows = mutableListOf<TableRow>()
            var currentRowCells = mutableListOf<TableCell>()
            val currentCellText = StringBuilder()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "p" -> {
                                insideParagraph = true
                                currentParagraphRuns = mutableListOf()
                                isHeading = false
                                headingLevel = 1
                                isBullet = false
                                alignment = TextAlignment.LEFT
                            }
                            "pStyle" -> {
                                val style = parser.getAttributeValue(null, "val") ?: ""
                                if (style.contains("Heading", ignoreCase = true) || style.contains("Title", ignoreCase = true)) {
                                    isHeading = true
                                    val levelChar = style.lastOrNull()
                                    headingLevel = if (levelChar?.isDigit() == true) levelChar.digitToInt() else 1
                                }
                            }
                            "numPr" -> {
                                isBullet = true
                            }
                            "jc" -> {
                                val jcVal = parser.getAttributeValue(null, "val") ?: ""
                                alignment = when (jcVal.lowercase()) {
                                    "center" -> TextAlignment.CENTER
                                    "right" -> TextAlignment.RIGHT
                                    "both" -> TextAlignment.JUSTIFY
                                    else -> TextAlignment.LEFT
                                }
                            }
                            "r" -> {
                                insideRun = true
                                runBold = false
                                runItalic = false
                                runUnderline = false
                                runColor = null
                                runFontSize = if (isHeading) (16f - headingLevel).coerceAtLeast(12f) else 11f
                                runTextBuilder.clear()
                            }
                            "b" -> runBold = true
                            "i" -> runItalic = true
                            "u" -> runUnderline = true
                            "color" -> {
                                val c = parser.getAttributeValue(null, "val")
                                if (!c.isNullOrBlank() && c != "auto") {
                                    runColor = "#$c"
                                }
                            }
                            "sz" -> {
                                val szVal = parser.getAttributeValue(null, "val")?.toFloatOrNull()
                                if (szVal != null) {
                                    // sz is in half-points
                                    runFontSize = (szVal / 2f).coerceIn(8f, 36f)
                                }
                            }
                            "br" -> {
                                val type = parser.getAttributeValue(null, "type")
                                if (type == "page") {
                                    if (currentParagraphRuns.isNotEmpty()) {
                                        elements.add(DocxElement.Paragraph(currentParagraphRuns.toList(), isHeading, headingLevel, alignment, isBullet))
                                        currentParagraphRuns.clear()
                                    }
                                    elements.add(DocxElement.PageBreak)
                                }
                            }
                            "blip" -> {
                                val embedId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                                    ?: parser.getAttributeValue(null, "embed")
                                if (embedId != null) {
                                    val target = relsMap[embedId]
                                    if (target != null) {
                                        val mediaBytes = mediaMap[target] ?: mediaMap["word/$target"] ?: mediaMap[target.removePrefix("word/")]
                                        if (mediaBytes != null) {
                                            try {
                                                val bmp = BitmapFactory.decodeByteArray(mediaBytes, 0, mediaBytes.size)
                                                if (bmp != null) {
                                                    elements.add(DocxElement.Image(bmp, bmp.width, bmp.height))
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            }
                            "tbl" -> {
                                insideTable = true
                                currentTableRows = mutableListOf()
                            }
                            "tr" -> {
                                insideRow = true
                                currentRowCells = mutableListOf()
                            }
                            "tc" -> {
                                insideCell = true
                                currentCellText.clear()
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (insideCell) {
                            currentCellText.append(text)
                        } else if (insideRun) {
                            runTextBuilder.append(text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "r" -> {
                                insideRun = false
                                val runText = runTextBuilder.toString()
                                if (runText.isNotEmpty()) {
                                    currentParagraphRuns.add(
                                        DocxRun(
                                            text = runText,
                                            isBold = runBold || isHeading,
                                            isItalic = runItalic,
                                            isUnderline = runUnderline,
                                            colorHex = runColor,
                                            fontSizePt = runFontSize
                                        )
                                    )
                                }
                            }
                            "p" -> {
                                insideParagraph = false
                                if (!insideCell && currentParagraphRuns.isNotEmpty()) {
                                    elements.add(
                                        DocxElement.Paragraph(
                                            runs = currentParagraphRuns.toList(),
                                            isHeading = isHeading,
                                            headingLevel = headingLevel,
                                            alignment = alignment,
                                            isBullet = isBullet
                                        )
                                    )
                                }
                            }
                            "tc" -> {
                                insideCell = false
                                val cellContent = currentCellText.toString().trim()
                                currentRowCells.add(TableCell(cellContent, isHeader = currentTableRows.isEmpty()))
                            }
                            "tr" -> {
                                insideRow = false
                                if (currentRowCells.isNotEmpty()) {
                                    currentTableRows.add(TableRow(currentRowCells.toList()))
                                }
                            }
                            "tbl" -> {
                                insideTable = false
                                if (currentTableRows.isNotEmpty()) {
                                    elements.add(DocxElement.Table(currentTableRows.toList()))
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Keep what we extracted so far
        }
    }
}
