package com.jyotix.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jyotix.app.core.db.entities.CityEntity

@Dao
interface CityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cities: List<CityEntity>): List<Long>

    @Query("SELECT * FROM cities WHERE name LIKE '%' || :query || '%' OR name_fa LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%' ORDER BY population DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 50): List<CityEntity>

    @Query("SELECT * FROM cities WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): CityEntity?

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int
}
