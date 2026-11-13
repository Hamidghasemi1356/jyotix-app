package com.jyotix.app.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jyotix.app.core.db.entities.SavedChartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedChartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chart: SavedChartEntity): Long

    @Update
    suspend fun update(chart: SavedChartEntity)

    @Delete
    suspend fun delete(chart: SavedChartEntity)

    @Query("SELECT * FROM saved_charts WHERE owner_phone = :phone ORDER BY created_at DESC")
    fun chartsFor(phone: String): Flow<List<SavedChartEntity>>

    @Query("SELECT * FROM saved_charts WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): SavedChartEntity?
}
