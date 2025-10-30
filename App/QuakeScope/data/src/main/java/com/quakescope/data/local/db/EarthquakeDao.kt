package com.quakescope.data.local.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EarthquakeDao {

    @Upsert
    suspend fun upsertAll(earthquakes: List<EarthquakeEntity>)

    @Query("SELECT * FROM earthquakes WHERE magnitude BETWEEN :minMagnitude AND :maxMagnitude AND depth BETWEEN :minDepth AND :maxDepth AND (:isReal IS NULL OR isReal = :isReal)")
    fun pagingSource(minMagnitude: Float, maxMagnitude: Float, minDepth: Float, maxDepth: Float, isReal: Boolean?): PagingSource<Int, EarthquakeEntity>

    @Query("SELECT * FROM earthquakes WHERE magnitude BETWEEN :minMagnitude AND :maxMagnitude AND depth BETWEEN :minDepth AND :maxDepth AND (:isReal IS NULL OR isReal = :isReal)")
    suspend fun getAll(minMagnitude: Float, maxMagnitude: Float, minDepth: Float, maxDepth: Float, isReal: Boolean?): List<EarthquakeEntity>

    @Query("DELETE FROM earthquakes")
    suspend fun clearAll()
}
