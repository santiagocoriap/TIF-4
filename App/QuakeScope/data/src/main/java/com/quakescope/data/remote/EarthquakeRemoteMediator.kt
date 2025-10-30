package com.quakescope.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.quakescope.data.local.db.AppDatabase
import com.quakescope.data.local.db.EarthquakeEntity
import com.quakescope.data.mapper.toEntity
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class EarthquakeRemoteMediator(
    private val apiService: ApiService,
    private val appDatabase: AppDatabase
) : RemoteMediator<Int, EarthquakeEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, EarthquakeEntity>): MediatorResult {
        return try {
            if (loadType == LoadType.PREPEND || loadType == LoadType.APPEND) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val detected = apiService.getDetectedEarthquakes().items.map { it.toEntity(true) }
            val expected = apiService.getExpectedEarthquakes().items.map { it.toEntity(false) }
            val earthquakeEntities = detected + expected

            appDatabase.withTransaction {
                appDatabase.earthquakeDao().clearAll()
                appDatabase.earthquakeDao().upsertAll(earthquakeEntities)
            }

            MediatorResult.Success(endOfPaginationReached = true)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
