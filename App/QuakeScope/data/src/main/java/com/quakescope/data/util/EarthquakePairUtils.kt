package com.quakescope.data.util

import com.quakescope.domain.model.Earthquake
import com.quakescope.domain.model.EarthquakePair
import android.util.Log
import com.quakescope.domain.model.FilterState
import com.quakescope.domain.model.SortOption

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object EarthquakePairUtils {

    private const val TIME_THRESHOLD_MS = 60 * 60 * 1000 // 1 hour
    private const val DISTANCE_THRESHOLD_KM = 100

    fun buildPairsByProximity(
        realEarthquakes: List<Earthquake>,
        estimatedEarthquakes: List<Earthquake>,
        sortOption: SortOption
    ): List<EarthquakePair> {
        val pairs = mutableListOf<EarthquakePair>()
        val usedEstimated = mutableSetOf<Earthquake>()

        for (real in realEarthquakes) {
            var bestMatch: Earthquake? = null
            var minDistance = Double.MAX_VALUE

            for (estimated in estimatedEarthquakes) {
                if (usedEstimated.contains(estimated)) continue

                val timeDiff = kotlin.math.abs(real.time.toEpochMilli() - estimated.time.toEpochMilli())
                if (timeDiff < TIME_THRESHOLD_MS) {
                    val distance = haversine(real.latitude, real.longitude, estimated.latitude, estimated.longitude)
                    if (distance < DISTANCE_THRESHOLD_KM && distance < minDistance) {
                        minDistance = distance
                        bestMatch = estimated
                    }
                }
            }

            if (bestMatch != null) {
                pairs.add(EarthquakePair(real, bestMatch))
                usedEstimated.add(bestMatch)
            }
        }

        val comparator = getComparator(sortOption)
        return pairs.sortedWith(comparator)
    }

    fun combineIntoPairs(
        realList: List<Earthquake>,
        estimatedList: List<Earthquake>
    ): List<EarthquakePair> {
        // Use earthquakeId so both sources align on the same identifier exposed by the backend.
        val realMap = realList.associateBy { it.earthquakeId }
        val pairs = mutableListOf<EarthquakePair>()

        Log.d(
            "EarthquakePairUtils",
            "combineIntoPairs real=${realList.size}, estimated=${estimatedList.size}"
        )

        for (estimated in estimatedList) {
            val matchKey = when {
                estimated.earthquakeId.isNotBlank() -> estimated.earthquakeId
                estimated.id.startsWith("exp-") -> estimated.id.removePrefix("exp-")
                else -> estimated.id
            }

            realMap[matchKey]?.let { real ->
                pairs.add(EarthquakePair(real, estimated))
            } ?: Log.d(
                "EarthquakePairUtils",
                "No real match for estimated id=${estimated.id} key=$matchKey"
            )
        }
        Log.d("EarthquakePairUtils", "combinedPairs=${pairs.size}")
        return pairs
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of Earth in kilometers
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun buildPairs(
        earthquakes: List<Earthquake>,
        sortOption: SortOption
    ): List<EarthquakePair> {
        val rawPairs = earthquakes
            .groupBy { it.earthquakeId }
            .map { (_, grouped) ->
                val real = grouped.find { it.isReal }
                val estimated = grouped.find { !it.isReal }
                EarthquakePair(real, estimated)
            }

        val comparator = getComparator(sortOption)
        return rawPairs.sortedWith(comparator)
    }

    fun getComparator(sortOption: SortOption): Comparator<EarthquakePair> = when (sortOption) {
        SortOption.TIME_DESC -> compareByDescending { it.latestEpochMillis() }
        SortOption.TIME_ASC -> compareBy { it.earliestEpochMillis() }
        SortOption.MAGNITUDE_DESC -> compareByDescending { it.maxMagnitude() }
        SortOption.MAGNITUDE_ASC -> compareBy { it.minMagnitude() }
        SortOption.DEPTH_DESC -> compareByDescending { it.maxDepth() }
        SortOption.DEPTH_ASC -> compareBy { it.minDepth() }

        SortOption.REAL_TIME_DESC -> compareByDescending { it.real?.time?.toEpochMilli() }
        SortOption.REAL_TIME_ASC -> compareBy { it.real?.time?.toEpochMilli() }
        SortOption.REAL_MAGNITUDE_DESC -> compareByDescending { it.real?.magnitude }
        SortOption.REAL_MAGNITUDE_ASC -> compareBy { it.real?.magnitude }
        SortOption.REAL_DEPTH_DESC -> compareByDescending { it.real?.depth }
        SortOption.REAL_DEPTH_ASC -> compareBy { it.real?.depth }

        SortOption.ESTIMATED_TIME_DESC -> compareByDescending { it.estimated?.time?.toEpochMilli() }
        SortOption.ESTIMATED_TIME_ASC -> compareBy { it.estimated?.time?.toEpochMilli() }
        SortOption.ESTIMATED_MAGNITUDE_DESC -> compareByDescending { it.estimated?.magnitude }
        SortOption.ESTIMATED_MAGNITUDE_ASC -> compareBy { it.estimated?.magnitude }
        SortOption.ESTIMATED_DEPTH_DESC -> compareByDescending { it.estimated?.depth }
        SortOption.ESTIMATED_DEPTH_ASC -> compareBy { it.estimated?.depth }
    }

    private fun EarthquakePair.latestEpochMillis(): Long =
        listOfNotNull(real?.time, estimated?.time)
            .maxOfOrNull { it.toEpochMilli() } ?: Long.MIN_VALUE

    private fun EarthquakePair.earliestEpochMillis(): Long =
        listOfNotNull(real?.time, estimated?.time)
            .minOfOrNull { it.toEpochMilli() } ?: Long.MAX_VALUE

    private fun EarthquakePair.maxMagnitude(): Double =
        listOfNotNull(real?.magnitude, estimated?.magnitude)
            .maxOrNull() ?: Double.NEGATIVE_INFINITY

    private fun EarthquakePair.minMagnitude(): Double =
        listOfNotNull(real?.magnitude, estimated?.magnitude)
            .minOrNull() ?: Double.POSITIVE_INFINITY

    private fun EarthquakePair.maxDepth(): Double =
        listOfNotNull(real?.depth, estimated?.depth)
            .maxOrNull() ?: Double.NEGATIVE_INFINITY

    private fun EarthquakePair.minDepth(): Double =
        listOfNotNull(real?.depth, estimated?.depth)
            .minOrNull() ?: Double.POSITIVE_INFINITY

    fun filterPairs(
        pairs: List<EarthquakePair>,
        filterState: FilterState
    ): List<EarthquakePair> {
        return pairs.filter { pair ->
            val real = pair.real
            val estimated = pair.estimated

            if (real == null || estimated == null) {
                return@filter false
            }

            val realMatch = real.magnitude in filterState.realMagnitudeRange &&
                    real.depth in filterState.realDepthRange

            val estimatedMatch = estimated.magnitude in filterState.estimatedMagnitudeRange &&
                    estimated.depth in filterState.estimatedDepthRange

            realMatch && estimatedMatch
        }
    }
}
