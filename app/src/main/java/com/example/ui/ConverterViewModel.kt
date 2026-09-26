package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.converter.ConversionManager
import com.example.converter.ConversionMode
import com.example.converter.ConversionProgress
import com.example.converter.DocumentType
import com.example.converter.PdfRenderOptions
import com.example.converter.SampleDocumentGenerator
import com.example.converter.SelectedFileItem
import com.example.data.db.AppDatabase
import com.example.data.model.ConversionRecord
import com.example.data.repository.ConversionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ConversionRepository(database.conversionDao())
    val conversionManager = ConversionManager(application, repository)

    val allConversions: StateFlow<List<ConversionRecord>> = repository.allConversions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteConversions: StateFlow<List<ConversionRecord>> = repository.favoriteConversions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversionProgress: StateFlow<ConversionProgress> = conversionManager.conversionProgress

    private val _conversionMode = MutableStateFlow(ConversionMode.AUTO)
    val conversionMode: StateFlow<ConversionMode> = _conversionMode.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<SelectedFileItem>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFileItem>> = _selectedFiles.asStateFlow()

    private val _renderOptions = MutableStateFlow(PdfRenderOptions())
    val renderOptions: StateFlow<PdfRenderOptions> = _renderOptions.asStateFlow()

    private val _currentPreviewRecord = MutableStateFlow<ConversionRecord?>(null)
    val currentPreviewRecord: StateFlow<ConversionRecord?> = _currentPreviewRecord.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setMode(mode: ConversionMode) {
        _conversionMode.value = mode
    }

    fun addFiles(uris: List<Uri>, context: Context) {
        val newItems = mutableListOf<SelectedFileItem>()
        for (uri in uris) {
            var fileName = "document"
            var fileSize = 0L

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}

            val mimeType = context.contentResolver.getType(uri)
            val docType = ConversionManager.detectType(fileName, mimeType)

            newItems.add(
                SelectedFileItem(
                    uri = uri,
                    name = fileName,
                    size = fileSize,
                    detectedType = docType
                )
            )
        }

        _selectedFiles.value = _selectedFiles.value + newItems
    }

    fun removeSelectedFile(index: Int) {
        val list = _selectedFiles.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _selectedFiles.value = list
        }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun convertSelectedFiles() {
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        viewModelScope.launch {
            val results = conversionManager.convertBatch(files, _conversionMode.value, _renderOptions.value)
            if (results.isNotEmpty()) {
                _toastMessage.value = "Successfully converted ${results.size} file(s)!"
                _currentPreviewRecord.value = results.lastOrNull()
            }
            clearSelectedFiles()
        }
    }

    fun convertSampleWord(context: Context) {
        viewModelScope.launch {
            val sampleDocx = SampleDocumentGenerator.getSampleWordFile(context)
            val item = SelectedFileItem(
                uri = Uri.fromFile(sampleDocx),
                name = sampleDocx.name,
                size = sampleDocx.length(),
                detectedType = DocumentType.WORD
            )
            val result = conversionManager.convertFile(item, ConversionMode.WORD_TO_PDF, _renderOptions.value)
            if (result != null) {
                _toastMessage.value = "Converted Sample Word doc to PDF!"
                _currentPreviewRecord.value = result
            }
        }
    }

    fun convertSamplePdf(context: Context) {
        viewModelScope.launch {
            val samplePdf = SampleDocumentGenerator.getSamplePdfFile(context)
            val item = SelectedFileItem(
                uri = Uri.fromFile(samplePdf),
                name = samplePdf.name,
                size = samplePdf.length(),
                detectedType = DocumentType.PDF
            )
            val result = conversionManager.convertFile(item, ConversionMode.PDF_TO_WORD, _renderOptions.value)
            if (result != null) {
                _toastMessage.value = "Converted Sample PDF to Word DOCX!"
                _currentPreviewRecord.value = result
            }
        }
    }

    fun toggleFavorite(record: ConversionRecord) {
        viewModelScope.launch {
            repository.toggleFavorite(record.id, record.isFavorite)
        }
    }

    fun deleteConversion(record: ConversionRecord) {
        viewModelScope.launch {
            repository.deleteConversion(record)
            if (_currentPreviewRecord.value?.id == record.id) {
                _currentPreviewRecord.value = null
            }
            _toastMessage.value = "Deleted conversion"
        }
    }

    fun clearAllConversions() {
        viewModelScope.launch {
            repository.clearAll()
            _currentPreviewRecord.value = null
            _toastMessage.value = "Conversion history cleared"
        }
    }

    fun setPreviewRecord(record: ConversionRecord?) {
        _currentPreviewRecord.value = record
    }

    fun shareRecord(record: ConversionRecord) {
        conversionManager.shareFile(record)
    }

    fun openRecord(record: ConversionRecord) {
        conversionManager.openWithExternalApp(record)
    }

    fun printRecord(record: ConversionRecord) {
        conversionManager.printDocument(record)
    }

    fun exportToDestination(context: Context, record: ConversionRecord, destinationUri: Uri) {
        viewModelScope.launch {
            try {
                val sourceFile = File(record.filePath)
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    _toastMessage.value = "Saved ${record.convertedFileName} to storage"
                }
            } catch (e: Exception) {
                _toastMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateRenderOptions(options: PdfRenderOptions) {
        _renderOptions.value = options
    }

    fun dismissProgress() {
        conversionManager.dismissProgress()
    }
}
