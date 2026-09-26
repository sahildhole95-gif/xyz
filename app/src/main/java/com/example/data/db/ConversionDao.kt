package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConversionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_records ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<ConversionRecord>>

    @Query("SELECT * FROM conversion_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteConversions(): Flow<List<ConversionRecord>>

    @Query("SELECT * FROM conversion_records WHERE conversionType = :type ORDER BY timestamp DESC")
    fun getConversionsByType(type: String): Flow<List<ConversionRecord>>

    @Query("SELECT * FROM conversion_records WHERE id = :id LIMIT 1")
    suspend fun getConversionById(id: Long): ConversionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(record: ConversionRecord): Long

    @Update
    suspend fun updateConversion(record: ConversionRecord)

    @Delete
    suspend fun deleteConversion(record: ConversionRecord)

    @Query("DELETE FROM conversion_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversion_records")
    suspend fun clearAll()

    @Query("UPDATE conversion_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
