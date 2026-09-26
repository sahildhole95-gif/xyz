package com.example.converter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.io.File
import java.io.FileOutputStream

data class PdfRenderOptions(
    val pageWidth: Int = 595, // Standard A4 width in points (72 dpi)
    val pageHeight: Int = 842, // Standard A4 height in points (72 dpi)
    val marginLeft: Float = 45f,
    val marginRight: Float = 45f,
    val marginTop: Float = 55f,
    val marginBottom: Float = 55f,
    val includeHeader: Boolean = true,
    val includeFooter: Boolean = true
)

object WordToPdfRenderer {

    fun renderToPdf(
        parsedDocx: ParsedDocx,
        outputPdfFile: File,
        options: PdfRenderOptions = PdfRenderOptions()
    ): Int {
        val pdfDocument = PdfDocument()

        val contentWidth = (options.pageWidth - options.marginLeft - options.marginRight).toInt()
        val contentHeight = options.pageHeight - options.marginTop - options.marginBottom

        val pages = mutableListOf<PdfPageContent>()
        var currentPage = PdfPageContent()
        var currentY = options.marginTop

        val lineSpacing = 4f
        val paragraphSpacing = 10f

        // Divide parsed elements into pages
        for (element in parsedDocx.elements) {
            when (element) {
                is DocxElement.PageBreak -> {
                    pages.add(currentPage)
                    currentPage = PdfPageContent()
                    currentY = options.marginTop
                }
                is DocxElement.Paragraph -> {
                    val staticLayout = createParagraphLayout(element, contentWidth)
                    val elementHeight = staticLayout.height.toFloat()

                    // Check if paragraph fits on current page
                    if (currentY + elementHeight > options.pageHeight - options.marginBottom && currentY > options.marginTop) {
                        // Start new page
                        pages.add(currentPage)
                        currentPage = PdfPageContent()
                        currentY = options.marginTop
                    }

                    currentPage.items.add(
                        PageItem.TextItem(
                            layout = staticLayout,
                            x = options.marginLeft + (if (element.isBullet) 14f else 0f),
                            y = currentY,
                            isBullet = element.isBullet
                        )
                    )
                    currentY += elementHeight + paragraphSpacing
                }
                is DocxElement.Image -> {
                    // Scale image to fit within content width
                    val maxImgWidth = contentWidth.toFloat()
                    val maxImgHeight = 350f
                    val scale = minOf(
                        maxImgWidth / element.bitmap.width.toFloat(),
                        maxImgHeight / element.bitmap.height.toFloat(),
                        1f
                    )
                    val renderW = element.bitmap.width * scale
                    val renderH = element.bitmap.height * scale

                    if (currentY + renderH > options.pageHeight - options.marginBottom && currentY > options.marginTop) {
                        pages.add(currentPage)
                        currentPage = PdfPageContent()
                        currentY = options.marginTop
                    }

                    currentPage.items.add(
                        PageItem.ImageItem(
                            bitmap = element.bitmap,
                            x = options.marginLeft + (contentWidth - renderW) / 2f,
                            y = currentY,
                            width = renderW,
                            height = renderH
                        )
                    )
                    currentY += renderH + paragraphSpacing
                }
                is DocxElement.Table -> {
                    val tableHeight = calculateTableHeight(element, contentWidth)
                    if (currentY + tableHeight > options.pageHeight - options.marginBottom && currentY > options.marginTop) {
                        pages.add(currentPage)
                        currentPage = PdfPageContent()
                        currentY = options.marginTop
                    }

                    currentPage.items.add(
                        PageItem.TableItem(
                            table = element,
                            x = options.marginLeft,
                            y = currentY,
                            width = contentWidth.toFloat()
                        )
                    )
                    currentY += tableHeight + paragraphSpacing
                }
            }
        }

        if (currentPage.items.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }

        val totalPages = pages.size

        // Paint components
        val headerPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerLinePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.75f
            isAntiAlias = true
        }

        val bulletPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            isAntiAlias = true
        }

        // Render each page into PdfDocument
        for ((pageIndex, pageContent) in pages.withIndex()) {
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(options.pageWidth, options.pageHeight, pageNumber).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            // Header
            if (options.includeHeader) {
                val headerText = parsedDocx.title.take(60)
                canvas.drawText(headerText, options.marginLeft, options.marginTop - 20f, headerPaint)
                canvas.drawLine(
                    options.marginLeft,
                    options.marginTop - 12f,
                    options.pageWidth - options.marginRight,
                    options.marginTop - 12f,
                    headerLinePaint
                )
            }

            // Page items
            for (item in pageContent.items) {
                when (item) {
                    is PageItem.TextItem -> {
                        canvas.save()
                        canvas.translate(item.x, item.y)
                        if (item.isBullet) {
                            canvas.drawCircle(-8f, 8f, 2.5f, bulletPaint)
                        }
                        item.layout.draw(canvas)
                        canvas.restore()
                    }
                    is PageItem.ImageItem -> {
                        val srcRect = Rect(0, 0, item.bitmap.width, item.bitmap.height)
                        val dstRect = RectF(item.x, item.y, item.x + item.width, item.y + item.height)
                        canvas.drawBitmap(item.bitmap, srcRect, dstRect, null)
                    }
                    is PageItem.TableItem -> {
                        drawTable(canvas, item.table, item.x, item.y, item.width)
                    }
                }
            }

            // Footer
            if (options.includeFooter) {
                canvas.drawLine(
                    options.marginLeft,
                    options.pageHeight - options.marginBottom + 12f,
                    options.pageWidth - options.marginRight,
                    options.pageHeight - options.marginBottom + 12f,
                    headerLinePaint
                )
                val footerDocu = "DocuConvert"
                canvas.drawText(footerDocu, options.marginLeft, options.pageHeight - options.marginBottom + 26f, headerPaint)

                val pageStr = "Page $pageNumber of $totalPages"
                val pageStrWidth = headerPaint.measureText(pageStr)
                canvas.drawText(pageStr, options.pageWidth - options.marginRight - pageStrWidth, options.pageHeight - options.marginBottom + 26f, headerPaint)
            }

            pdfDocument.finishPage(pdfPage)
        }

        // Write to file
        FileOutputStream(outputPdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return totalPages
    }

    private fun createParagraphLayout(paragraph: DocxElement.Paragraph, contentWidth: Int): StaticLayout {
        val ssb = SpannableStringBuilder()

        for (run in paragraph.runs) {
            val start = ssb.length
            ssb.append(run.text)
            val end = ssb.length

            if (start < end) {
                // Style span
                val style = when {
                    run.isBold && run.isItalic -> Typeface.BOLD_ITALIC
                    run.isBold -> Typeface.BOLD
                    run.isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                if (style != Typeface.NORMAL) {
                    ssb.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (run.isUnderline) {
                    ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (run.colorHex != null) {
                    try {
                        val color = Color.parseColor(run.colorHex)
                        ssb.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } catch (_: Exception) {}
                }
            }
        }

        // Determine base text paint
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = if (paragraph.isHeading) Color.rgb(15, 23, 42) else Color.rgb(30, 41, 59)
            textSize = when {
                paragraph.isHeading && paragraph.headingLevel == 1 -> 18f
                paragraph.isHeading && paragraph.headingLevel == 2 -> 15f
                paragraph.isHeading -> 13f
                else -> paragraph.runs.firstOrNull()?.fontSizePt ?: 10.5f
            }
            if (paragraph.isHeading) {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }

        val alignment = when (paragraph.alignment) {
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val effectiveWidth = if (paragraph.isBullet) contentWidth - 14 else contentWidth

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(ssb, 0, ssb.length, textPaint, effectiveWidth.coerceAtLeast(100))
                .setAlignment(alignment)
                .setLineSpacing(3f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                ssb,
                textPaint,
                effectiveWidth.coerceAtLeast(100),
                alignment,
                1.15f,
                3f,
                false
            )
        }
    }

    private fun calculateTableHeight(table: DocxElement.Table, contentWidth: Int): Float {
        var total = 0f
        for (row in table.rows) {
            total += 26f // approximate cell height
        }
        return total
    }

    private fun drawTable(canvas: Canvas, table: DocxElement.Table, x: Float, y: Float, width: Float) {
        if (table.rows.isEmpty()) return

        val colCount = table.rows.maxOfOrNull { it.cells.size } ?: 1
        val colWidth = width / colCount.toFloat()
        val rowHeight = 24f

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            isAntiAlias = true
        }

        var currentY = y

        for ((rowIndex, row) in table.rows.withIndex()) {
            val isHeader = rowIndex == 0 && row.cells.any { it.isHeader }
            if (isHeader) {
                canvas.drawRect(x, currentY, x + width, currentY + rowHeight, headerBgPaint)
            }

            for ((colIndex, cell) in row.cells.withIndex()) {
                val cellX = x + colIndex * colWidth
                canvas.drawRect(cellX, currentY, cellX + colWidth, currentY + rowHeight, borderPaint)

                textPaint.typeface = if (isHeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                val textToDraw = cell.text.take(25)
                canvas.drawText(textToDraw, cellX + 6f, currentY + 16f, textPaint)
            }
            currentY += rowHeight
        }
    }

    private class PdfPageContent {
        val items = mutableListOf<PageItem>()
    }

    private sealed class PageItem {
        data class TextItem(val layout: StaticLayout, val x: Float, val y: Float, val isBullet: Boolean) : PageItem()
        data class ImageItem(val bitmap: Bitmap, val x: Float, val y: Float, val width: Float, val height: Float) : PageItem()
        data class TableItem(val table: DocxElement.Table, val x: Float, val y: Float, val width: Float) : PageItem()
    }
}
