package com.example.converter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.ConversionRecord
import com.example.data.repository.ConversionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class SelectedFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val detectedType: DocumentType
)

enum class DocumentType {
    WORD, // .docx, .doc
    PDF,  // .pdf
    UNKNOWN
}

enum class ConversionMode {
    AUTO,
    WORD_TO_PDF,
    PDF_TO_WORD
}

data class ConversionProgress(
    val isConverting: Boolean = false,
    val currentFileName: String = "",
    val stage: String = "",
    val progressFraction: Float = 0f,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val latestResult: ConversionRecord? = null,
    val error: String? = null
)

class ConversionManager(
    private val context: Context,
    private val repository: ConversionRepository
) {
    private val _conversionProgress = MutableStateFlow(ConversionProgress())
    val conversionProgress: StateFlow<ConversionProgress> = _conversionProgress.asStateFlow()

    suspend fun convertFile(
        fileItem: SelectedFileItem,
        mode: ConversionMode,
        options: PdfRenderOptions = PdfRenderOptions()
    ): ConversionRecord? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val isWordSource = when (mode) {
            ConversionMode.WORD_TO_PDF -> true
            ConversionMode.PDF_TO_WORD -> false
            ConversionMode.AUTO -> fileItem.detectedType == DocumentType.WORD
        }

        val targetExt = if (isWordSource) "pdf" else "docx"
        val convType = if (isWordSource) "WORD_TO_PDF" else "PDF_TO_WORD"
        val baseName = fileItem.name.substringBeforeLast(".")
        val outFileName = "${baseName}_converted.$targetExt"

        _conversionProgress.value = ConversionProgress(
            isConverting = true,
            currentFileName = fileItem.name,
            stage = "Reading document...",
            progressFraction = 0.2f
        )

        try {
            // Copy source file to temp
            val tempSourceFile = File(context.cacheDir, "source_${System.currentTimeMillis()}_${fileItem.name}")
            context.contentResolver.openInputStream(fileItem.uri)?.use { input ->
                FileOutputStream(tempSourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not read file from storage")

            val outputDir = File(context.filesDir, "conversions").apply { mkdirs() }
            val outputFile = File(outputDir, outFileName)

            var pageCount = 1

            if (isWordSource) {
                _conversionProgress.value = _conversionProgress.value.copy(
                    stage = "Parsing Word layout & styles...",
                    progressFraction = 0.5f
                )
                val parsedDocx = DocxParser.parse(tempSourceFile)

                _conversionProgress.value = _conversionProgress.value.copy(
                    stage = "Rendering high-fidelity PDF pages...",
                    progressFraction = 0.8f
                )
                pageCount = WordToPdfRenderer.renderToPdf(parsedDocx, outputFile, options)
            } else {
                _conversionProgress.value = _conversionProgress.value.copy(
                    stage = "Extracting PDF text & media...",
                    progressFraction = 0.5f
                )
                val result = PdfToWordConverter.convertPdfToDocx(context, tempSourceFile, outputFile)
                pageCount = result.totalPages
            }

            tempSourceFile.delete()

            _conversionProgress.value = _conversionProgress.value.copy(
                stage = "Conversion Complete!",
                progressFraction = 1.0f
            )

            val record = ConversionRecord(
                originalFileName = fileItem.name,
                convertedFileName = outFileName,
                conversionType = convType,
                sourceSizeBytes = fileItem.size,
                resultSizeBytes = outputFile.length(),
                filePath = outputFile.absolutePath,
                pageCount = pageCount,
                timestamp = System.currentTimeMillis(),
                status = "SUCCESS"
            )

            val recordId = repository.insertConversion(record)
            val savedRecord = record.copy(id = recordId)

            _conversionProgress.value = _conversionProgress.value.copy(
                isConverting = false,
                latestResult = savedRecord
            )

            savedRecord
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Conversion failed"
            _conversionProgress.value = ConversionProgress(
                isConverting = false,
                currentFileName = fileItem.name,
                stage = "Failed",
                error = errorMsg
            )

            val failedRecord = ConversionRecord(
                originalFileName = fileItem.name,
                convertedFileName = outFileName,
                conversionType = convType,
                sourceSizeBytes = fileItem.size,
                resultSizeBytes = 0,
                filePath = "",
                pageCount = 0,
                timestamp = System.currentTimeMillis(),
                status = "FAILED",
                errorMessage = errorMsg
            )
            repository.insertConversion(failedRecord)
            null
        }
    }

    suspend fun convertBatch(
        items: List<SelectedFileItem>,
        mode: ConversionMode,
        options: PdfRenderOptions = PdfRenderOptions()
    ): List<ConversionRecord> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ConversionRecord>()
        for ((idx, item) in items.withIndex()) {
            _conversionProgress.value = _conversionProgress.value.copy(
                completedCount = idx,
                totalCount = items.size
            )
            val result = convertFile(item, mode, options)
            if (result != null) {
                results.add(result)
            }
        }
        _conversionProgress.value = _conversionProgress.value.copy(
            isConverting = false,
            completedCount = items.size,
            totalCount = items.size
        )
        results
    }

    fun dismissProgress() {
        _conversionProgress.value = ConversionProgress()
    }

    fun shareFile(record: ConversionRecord) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) return

            val mimeType = if (record.isWordToPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Converted Document: ${record.convertedFileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Document via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    fun openWithExternalApp(record: ConversionRecord) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) return

            val mimeType = if (record.isWordToPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun printDocument(record: ConversionRecord) {
        if (!record.isWordToPdf) return
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val file = File(record.filePath)
            if (!file.exists()) return

            val printAdapter = PdfPrintAdapter(file)
            printManager.print("DocuConvert - ${record.convertedFileName}", printAdapter, PrintAttributes.Builder().build())
        } catch (_: Exception) {}
    }

    companion object {
        fun detectType(name: String, mimeType: String? = null): DocumentType {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".docx") || lower.endsWith(".doc") || mimeType?.contains("word") == true -> DocumentType.WORD
                lower.endsWith(".pdf") || mimeType?.contains("pdf") == true -> DocumentType.PDF
                else -> DocumentType.UNKNOWN
            }
        }
    }
}
