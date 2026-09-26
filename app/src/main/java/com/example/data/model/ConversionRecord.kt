package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_records")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalFileName: String,
    val convertedFileName: String,
    val conversionType: String, // "WORD_TO_PDF" or "PDF_TO_WORD"
    val sourceSizeBytes: Long,
    val resultSizeBytes: Long,
    val filePath: String,
    val pageCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // "SUCCESS", "FAILED"
    val errorMessage: String? = null,
    val isFavorite: Boolean = false
) {
    val isWordToPdf: Boolean
        get() = conversionType == "WORD_TO_PDF"

    val formattedSourceSize: String
        get() = formatFileSize(sourceSizeBytes)

    val formattedResultSize: String
        get() = formatFileSize(resultSizeBytes)

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val size = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.1f %s", size, units[digitGroups])
        }
    }
}
