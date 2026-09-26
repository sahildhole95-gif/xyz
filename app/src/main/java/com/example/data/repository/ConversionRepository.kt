package com.example.data.repository

import com.example.data.db.ConversionDao
import com.example.data.model.ConversionRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

class ConversionRepository(private val conversionDao: ConversionDao) {
    val allConversions: Flow<List<ConversionRecord>> = conversionDao.getAllConversions()
    val favoriteConversions: Flow<List<ConversionRecord>> = conversionDao.getFavoriteConversions()

    fun getConversionsByType(type: String): Flow<List<ConversionRecord>> =
        conversionDao.getConversionsByType(type)

    suspend fun getConversionById(id: Long): ConversionRecord? =
        conversionDao.getConversionById(id)

    suspend fun insertConversion(record: ConversionRecord): Long =
        conversionDao.insertConversion(record)

    suspend fun updateConversion(record: ConversionRecord) =
        conversionDao.updateConversion(record)

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        conversionDao.updateFavorite(id, !currentFavorite)
    }

    suspend fun deleteConversion(record: ConversionRecord) {
        // Also remove physical file if it exists
        try {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        conversionDao.deleteConversion(record)
    }

    suspend fun clearAll() {
        conversionDao.clearAll()
    }
}
