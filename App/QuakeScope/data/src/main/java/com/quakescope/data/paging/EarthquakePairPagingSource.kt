package com.quakescope.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.quakescope.data.local.db.EarthquakeDao
import com.quakescope.data.mapper.toDomain
import com.quakescope.data.util.EarthquakePairUtils
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.domain.model.FilterState

import com.quakescope.domain.model.EarthquakeType

class EarthquakePairPagingSource(
    private val earthquakeDao: EarthquakeDao,
    private val filterState: FilterState
) : PagingSource<Int, EarthquakePair>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EarthquakePair> {
        return try {
            val pageNumber = params.key ?: 0
            val pageSize = params.loadSize

            val pairs = when (filterState.earthquakeType) {
                EarthquakeType.REAL -> {
                    earthquakeDao.getAll(
                        minMagnitude = filterState.magnitudeRange.start,
                        maxMagnitude = filterState.magnitudeRange.endInclusive,
                        minDepth = filterState.depthRange.start,
                        maxDepth = filterState.depthRange.endInclusive,
                        isReal = true
                    ).map { EarthquakePair(it.toDomain(), null) }
                        .sortedWith(EarthquakePairUtils.getComparator(filterState.sortOption))
                }
                EarthquakeType.ESTIMATED -> {
                    earthquakeDao.getAll(
                        minMagnitude = filterState.magnitudeRange.start,
                        maxMagnitude = filterState.magnitudeRange.endInclusive,
                        minDepth = filterState.depthRange.start,
                        maxDepth = filterState.depthRange.endInclusive,
                        isReal = false
                    ).map { EarthquakePair(null, it.toDomain()) }
                        .sortedWith(EarthquakePairUtils.getComparator(filterState.sortOption))
                }
                EarthquakeType.PAIRS -> {
                    val allReal = earthquakeDao.getAll(
                        minMagnitude = -1000f, maxMagnitude = 1000f,
                        minDepth = -10000f, maxDepth = 10000f,
                        isReal = true
                    ).map { it.toDomain() }

                    val allEstimated = earthquakeDao.getAll(
                        minMagnitude = -1000f, maxMagnitude = 1000f,
                        minDepth = -10000f, maxDepth = 10000f,
                        isReal = false
                    ).map { it.toDomain() }

                    val combinedPairs = EarthquakePairUtils.combineIntoPairs(
                        realList = allReal,
                        estimatedList = allEstimated
                    )
                    val sortedPairs = combinedPairs.sortedWith(
                        EarthquakePairUtils.getComparator(filterState.sortOption)
                    )

                    val filteredPairs = EarthquakePairUtils.filterPairs(
                        pairs = sortedPairs,
                        filterState = filterState
                    )

                    Log.d(
                        "EarthquakePairPaging",
                        "Pair load: real=${allReal.size}, estimated=${allEstimated.size}, combined=${combinedPairs.size}, filtered=${filteredPairs.size}"
                    )

                    filteredPairs
                }
            }

            val start = pageNumber * pageSize
            val end = (start + pageSize).coerceAtMost(pairs.size)

            if (start >= pairs.size) {
                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }

            LoadResult.Page(
                data = pairs.subList(start, end),
                prevKey = if (pageNumber > 0) pageNumber - 1 else null,
                nextKey = if (end < pairs.size) pageNumber + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, EarthquakePair>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
