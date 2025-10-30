package com.quakescope.data.repo

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.quakescope.data.local.db.AppDatabase
import com.quakescope.data.mapper.toDomain
import com.quakescope.data.mapper.toEntity
import com.quakescope.data.paging.EarthquakePairPagingSource
import com.quakescope.data.remote.ApiService
import com.quakescope.data.util.EarthquakePairUtils
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.model.FilterState
import com.quakescope.domain.model.EarthquakeType
import com.quakescope.domain.model.SortOption
import com.quakescope.domain.model.SortOptionRules
import com.quakescope.domain.repo.EarthquakeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class EarthquakeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val appDatabase: AppDatabase
) : EarthquakeRepository {

    override fun getEarthquakePairs(filterState: FilterState): Flow<PagingData<EarthquakePair>> {
        return if (filterState.earthquakeType == EarthquakeType.PAIRS) {
            flow {
                val pairs = try {
                    fetchPairedEarthquakes(filterState)
                } catch (io: IOException) {
                    Log.w(TAG, "Failed to fetch paired earthquakes due to network error", io)
                    emptyList()
                } catch (http: HttpException) {
                    Log.w(TAG, "Failed to fetch paired earthquakes: ${http.code()} ${http.message()}", http)
                    emptyList()
                }
                emit(PagingData.from(pairs))
            }
        } else {
            val pagingSourceFactory = { EarthquakePairPagingSource(appDatabase.earthquakeDao(), filterState) }
            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = pagingSourceFactory
            ).flow.onStart { refreshEarthquakes(filterState) }
        }
    }

    override fun getEarthquakesForMap(filterState: FilterState): Flow<List<EarthquakePair>> = flow {
        val normalizedSort = SortOptionRules.ensureAllowed(filterState.sortOption, filterState.earthquakeType)
        if (filterState.earthquakeType == EarthquakeType.PAIRS) {
            val pairs = try {
                fetchPairedEarthquakes(filterState)
            } catch (io: IOException) {
                Log.w(TAG, "Failed to fetch paired earthquakes for map due to network error", io)
                emptyList()
            } catch (http: HttpException) {
                Log.w(TAG, "Failed to fetch paired earthquakes for map: ${http.code()} ${http.message()}", http)
                emptyList()
            }
            emit(sortPairs(pairs, normalizedSort))
        } else {
            refreshEarthquakes(filterState)
            val pairs = when (filterState.earthquakeType) {
                EarthquakeType.REAL -> {
                    appDatabase.earthquakeDao().getAll(
                        minMagnitude = filterState.magnitudeRange.start,
                        maxMagnitude = filterState.magnitudeRange.endInclusive,
                        minDepth = filterState.depthRange.start,
                        maxDepth = filterState.depthRange.endInclusive,
                        isReal = true
                    ).map { EarthquakePair(it.toDomain(), null) }
                }
                EarthquakeType.ESTIMATED -> {
                    appDatabase.earthquakeDao().getAll(
                        minMagnitude = filterState.magnitudeRange.start,
                        maxMagnitude = filterState.magnitudeRange.endInclusive,
                        minDepth = filterState.depthRange.start,
                        maxDepth = filterState.depthRange.endInclusive,
                        isReal = false
                    ).map { EarthquakePair(null, it.toDomain()) }
                }
                EarthquakeType.PAIRS -> emptyList()
            }
            emit(sortPairs(pairs, normalizedSort))
        }
    }

    private suspend fun fetchPairedEarthquakes(filterState: FilterState): List<EarthquakePair> {
        val response = apiService.getPairedEarthquakePairs(
            realMinMagnitude = filterState.realMagnitudeRange.start.toDouble(),
            realMaxMagnitude = filterState.realMagnitudeRange.endInclusive.toDouble(),
            realMinDepth = filterState.realDepthRange.start.toDouble(),
            realMaxDepth = filterState.realDepthRange.endInclusive.toDouble(),
            expectedMinMagnitude = filterState.estimatedMagnitudeRange.start.toDouble(),
            expectedMaxMagnitude = filterState.estimatedMagnitudeRange.endInclusive.toDouble(),
            expectedMinDepth = filterState.estimatedDepthRange.start.toDouble(),
            expectedMaxDepth = filterState.estimatedDepthRange.endInclusive.toDouble(),
            limit = filterState.limit
        )
        val normalizedSort = SortOptionRules.ensureAllowed(filterState.sortOption, EarthquakeType.PAIRS)
        val domainPairs = response.items.map { it.toDomain() }
        return sortPairs(domainPairs, normalizedSort)
    }

    private suspend fun refreshEarthquakes(filterState: FilterState) {
        if (filterState.earthquakeType == EarthquakeType.PAIRS) {
            return
        }
        try {
            val minMag = filterState.magnitudeRange.start.toDouble()
            val maxMag = filterState.magnitudeRange.endInclusive.toDouble()
            val requestLimit = (filterState.limit * 2).coerceIn(10, 500)
            val detected = apiService.getDetectedEarthquakes(minMag, maxMag, requestLimit).items
            val expected = apiService.getExpectedEarthquakes(minMag, maxMag, requestLimit).items
            val earthquakeEntities = detected.map { it.toEntity(true) } + expected.map { it.toEntity(false) }
            appDatabase.withTransaction {
                appDatabase.earthquakeDao().clearAll()
                appDatabase.earthquakeDao().upsertAll(earthquakeEntities)
            }
        } catch (io: IOException) {
            Log.w(TAG, "Failed to refresh earthquakes due to network error", io)
        } catch (http: HttpException) {
            Log.w(TAG, "Failed to refresh earthquakes: ${http.code()} ${http.message()}", http)
        }
    }

    private fun sortPairs(
        pairs: List<EarthquakePair>,
        sortOption: SortOption
    ): List<EarthquakePair> {
        if (pairs.isEmpty()) return pairs
        val comparator = EarthquakePairUtils.getComparator(sortOption)
        return pairs.sortedWith(comparator)
    }

    private companion object {
        const val TAG = "EarthquakeRepository"
    }
}
