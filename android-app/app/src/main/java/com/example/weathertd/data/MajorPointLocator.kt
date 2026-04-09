package com.example.weathertd.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class MajorPointLocator(
    private val points: List<MajorPoint> = MajorPointCatalog.all,
) {

    fun findNearest(latitude: Double, longitude: Double): MajorPoint {
        return points.minByOrNull { point ->
            distanceInMeters(
                startLatitude = latitude,
                startLongitude = longitude,
                endLatitude = point.latitude,
                endLongitude = point.longitude,
            )
        } ?: MajorPointCatalog.fallback
    }

    private fun distanceInMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val earthRadius = 6_371_000.0
        val latDistance = Math.toRadians(endLatitude - startLatitude)
        val lonDistance = Math.toRadians(endLongitude - startLongitude)
        val originLat = Math.toRadians(startLatitude)
        val destinationLat = Math.toRadians(endLatitude)

        val a = sin(latDistance / 2) * sin(latDistance / 2) +
            cos(originLat) * cos(destinationLat) *
            sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - min(a, 1.0)))
        return earthRadius * c
    }
}
