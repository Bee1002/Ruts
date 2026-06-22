package com.example.ruts.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object RouteOptimizer {
    private const val EarthRadiusKm = 6371.0

    fun optimize(stops: List<DeliveryStop>, start: GeoPoint): List<DeliveryStop> {
        val pendingWithLocation = stops
            .filter { it.status == StopStatus.Pending && it.location != null }

        val otherStops = stops.filterNot { it in pendingWithLocation }

        if (pendingWithLocation.size < 2) {
            return (pendingWithLocation + otherStops)
                .mapIndexed { index, stop -> stop.copy(orderIndex = index) }
        }

        val nearestRoute = buildNearestNeighborRoute(pendingWithLocation, start)
        val improvedRoute = improveWithTwoOpt(nearestRoute, start)

        return (improvedRoute + otherStops)
            .mapIndexed { index, stop -> stop.copy(orderIndex = index) }
    }

    fun distanceKm(first: GeoPoint, second: GeoPoint): Double {
        val deltaLat = Math.toRadians(second.latitude - first.latitude)
        val deltaLon = Math.toRadians(second.longitude - first.longitude)
        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)

        val haversine = sin(deltaLat / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)

        return 2 * EarthRadiusKm * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    fun totalDistanceKm(stops: List<DeliveryStop>, start: GeoPoint): Double {
        var current = start
        var total = 0.0

        stops.forEach { stop ->
            val location = stop.location ?: return@forEach
            total += distanceKm(current, location)
            current = location
        }

        return total
    }

    private fun buildNearestNeighborRoute(
        stops: List<DeliveryStop>,
        start: GeoPoint,
    ): List<DeliveryStop> {
        val remaining = stops.toMutableList()
        val orderedStops = mutableListOf<DeliveryStop>()
        var currentLocation = start

        while (remaining.isNotEmpty()) {
            val nextStop = remaining.minBy { stop ->
                distanceKm(currentLocation, requireNotNull(stop.location))
            }

            orderedStops += nextStop
            remaining -= nextStop
            currentLocation = requireNotNull(nextStop.location)
        }

        return orderedStops
    }

    private fun improveWithTwoOpt(
        route: List<DeliveryStop>,
        start: GeoPoint,
    ): List<DeliveryStop> {
        var bestRoute = route
        var improved = true

        while (improved) {
            improved = false

            for (i in 0 until bestRoute.lastIndex) {
                for (j in i + 1..bestRoute.lastIndex) {
                    val candidate = bestRoute.take(i) +
                        bestRoute.subList(i, j + 1).reversed() +
                        bestRoute.drop(j + 1)

                    if (totalDistanceKm(candidate, start) < totalDistanceKm(bestRoute, start)) {
                        bestRoute = candidate
                        improved = true
                    }
                }
            }
        }

        return bestRoute
    }
}
