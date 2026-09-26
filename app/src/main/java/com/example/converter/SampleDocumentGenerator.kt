package com.example.converter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleDocumentGenerator {

    fun getSampleWordFile(context: Context): File {
        val sampleDir = File(context.cacheDir, "samples").apply { mkdirs() }
        val docxFile = File(sampleDir, "Sample_Business_Proposal.docx")
        if (docxFile.exists() && docxFile.length() > 0) {
            return docxFile
        }

        // Build valid DOCX
        ZipOutputStream(FileOutputStream(docxFile)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="36"/><w:color w:val="2563EB"/></w:rPr><w:t>Cloud Solutions Proposal 2026</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:i/><w:sz w:val="22"/><w:color w:val="64748B"/></w:rPr><w:t>Modern Enterprise Architecture &amp; Digital Transformation</w:t></w:r>
    </w:p>
    <w:p><w:r><w:t></w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr><w:t>1. Executive Summary</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>This proposal outlines a phased migration path to streamline file conversion pipelines, automate document processing, and eliminate vendor lock-in. Our mobile-first architecture provides offline resilience, end-to-end security, and instant turnaround for field teams.</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr><w:t>2. Key Objectives &amp; Milestones</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:ind w:left="720"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>• Sub-second document parsing and layout extraction</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:ind w:left="720"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>• Bi-directional conversion between Microsoft Word and Adobe PDF</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:ind w:left="720"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>• Zero data transmission: 100% on-device privacy guarantee</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:ind w:left="720"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>• Intuitive mobile drag-and-drop workspace with batch queue</w:t></w:r>
    </w:p>
    <w:p><w:r><w:t></w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr><w:t>3. Project Investment</w:t></w:r>
    </w:p>
    <w:tbl>
      <w:tr>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Phase</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Deliverable</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Timeline</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 1</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Core Conversion Engine</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Weeks 1-3</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 2</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Compose UI &amp; Drag-Drop</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Weeks 4-6</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 3</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>QA &amp; Device Testing</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Weeks 7-8</w:t></w:r></w:p></w:tc>
      </w:tr>
    </w:tbl>
    <w:p><w:r><w:t></w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/><w:sz w:val="24"/><w:color w:val="10B981"/></w:rPr><w:t>Approved by: Strategic Planning Committee</w:t></w:r>
    </w:p>
  </w:body>
</w:document>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        return docxFile
    }

    fun getSamplePdfFile(context: Context): File {
        val sampleDir = File(context.cacheDir, "samples").apply { mkdirs() }
        val pdfFile = File(sampleDir, "Sample_Executive_Report.pdf")
        if (pdfFile.exists() && pdfFile.length() > 0) {
            return pdfFile
        }

        val pdfDocument = PdfDocument()

        // Page 1
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas
        canvas1.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(220, 38, 38)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas1.drawRect(0f, 0f, 595f, 90f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("Executive Digital Strategy 2026", 45f, 52f, titlePaint)

        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 130f
        canvas1.drawText("1. Mission Overview", 45f, y, headingPaint)
        y += 24f
        canvas1.drawText("This PDF document highlights enterprise workflow automation benchmarks and document transformation efficiency.", 45f, y, textPaint)
        y += 18f
        canvas1.drawText("Through intelligent format conversion, team collaboration cycle time is reduced by up to 64%.", 45f, y, textPaint)

        y += 35f
        canvas1.drawText("2. Key Performance Indicators", 45f, y, headingPaint)
        y += 24f
        canvas1.drawText("• Average conversion time: Less than 1.2 seconds per file", 60f, y, textPaint)
        y += 20f
        canvas1.drawText("• Native fidelity: Preserves text typography, headings, tables, and images", 60f, y, textPaint)
        y += 20f
        canvas1.drawText("• Multi-platform sharing: Seamless integration with WhatsApp, Drive, and Email", 60f, y, textPaint)
        y += 20f
        canvas1.drawText("• Security: 100% offline local conversion with no cloud dependency", 60f, y, textPaint)

        y += 40f
        canvas1.drawText("3. Summary & Next Steps", 45f, y, headingPaint)
        y += 24f
        canvas1.drawText("Converting this document to Word DOCX allows editing text, reflowing paragraphs, and updating data.", 45f, y, textPaint)

        // Footer
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            isAntiAlias = true
        }
        canvas1.drawText("DocuConvert Official Sample Document", 45f, 800f, footerPaint)
        canvas1.drawText("Page 1 of 1", 500f, 800f, footerPaint)

        pdfDocument.finishPage(page1)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }
}
