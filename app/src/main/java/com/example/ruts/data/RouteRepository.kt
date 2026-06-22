package com.example.ruts.data

import android.content.Context
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.Route
import com.example.ruts.domain.StopStatus
import com.example.ruts.domain.StopType
import org.json.JSONArray
import org.json.JSONObject

class RouteRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    fun getAllRoutes(): List<Route> {
        val rawRoutes = preferences.getString(RoutesKey, null) ?: return emptyList()
        val routesJson = JSONArray(rawRoutes)

        return buildList {
            for (index in 0 until routesJson.length()) {
                add(routesJson.getJSONObject(index).toRoute())
            }
        }.sortedByDescending { it.createdAtMillis }
    }

    fun getRoute(routeId: String): Route? {
        return getAllRoutes().firstOrNull { it.id == routeId }
    }

    fun getLastRouteId(): String? {
        val lastId = preferences.getString(LastRouteKey, null)
        if (lastId != null && getRoute(lastId) != null) {
            return lastId
        }
        return getAllRoutes().firstOrNull()?.id
    }

    fun saveRoute(route: Route) {
        val routes = getAllRoutes()
            .filterNot { it.id == route.id }
            .plus(route)
            .sortedByDescending { it.createdAtMillis }

        persistRoutes(routes)
        setLastRouteId(route.id)
    }

    fun setLastRouteId(routeId: String) {
        preferences.edit().putString(LastRouteKey, routeId).apply()
    }

    fun deleteRoute(routeId: String) {
        val routes = getAllRoutes().filterNot { it.id == routeId }
        persistRoutes(routes)

        if (preferences.getString(LastRouteKey, null) == routeId) {
            preferences.edit()
                .putString(LastRouteKey, routes.firstOrNull()?.id)
                .apply()
        }
    }

    fun createRoute(createdAtMillis: Long = System.currentTimeMillis()): Route {
        val route = Route(createdAtMillis = createdAtMillis)
        saveRoute(route)
        return route
    }

    fun duplicateRouteWithStops(source: Route, createdAtMillis: Long = System.currentTimeMillis()): Route {
        val copiedStops = source.stops
            .sortedBy { it.orderIndex }
            .mapIndexed { index, stop ->
                stop.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    orderIndex = index,
                    status = StopStatus.Pending,
                    failureReason = "",
                )
            }

        val route = Route(
            createdAtMillis = createdAtMillis,
            startLocation = source.startLocation,
            stops = copiedStops,
        )
        saveRoute(route)
        return route
    }

    private fun persistRoutes(routes: List<Route>) {
        val payload = JSONArray()
        routes.forEach { route -> payload.put(route.toJson()) }

        preferences.edit()
            .putString(RoutesKey, payload.toString())
            .apply()
    }

    private fun Route.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("createdAtMillis", createdAtMillis)
        .put("startLatitude", startLocation?.latitude)
        .put("startLongitude", startLocation?.longitude)
        .put("stops", JSONArray().also { stopsArray ->
            stops.forEach { stop -> stopsArray.put(stop.toJson()) }
        })

    private fun DeliveryStop.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("customerName", customerName)
        .put("address", address)
        .put("latitude", location?.latitude)
        .put("longitude", location?.longitude)
        .put("orderIndex", orderIndex)
        .put("status", status.name)
        .put("stopType", stopType.name)
        .put("packageCount", packageCount)
        .put("notes", notes)
        .put("failureReason", failureReason)

    private fun JSONObject.toRoute(): Route {
        val startLatitude = optNullableDouble("startLatitude")
        val startLongitude = optNullableDouble("startLongitude")
        val stopsJson = getJSONArray("stops")
        val stops = buildList {
            for (index in 0 until stopsJson.length()) {
                add(stopsJson.getJSONObject(index).toDeliveryStop())
            }
        }.sortedBy { it.orderIndex }

        return Route(
            id = getString("id"),
            name = optString("name"),
            createdAtMillis = getLong("createdAtMillis"),
            startLocation = if (startLatitude != null && startLongitude != null) {
                GeoPoint(startLatitude, startLongitude)
            } else {
                null
            },
            stops = stops,
        )
    }

    private fun JSONObject.toDeliveryStop(): DeliveryStop {
        val latitude = optNullableDouble("latitude")
        val longitude = optNullableDouble("longitude")

        return DeliveryStop(
            id = getString("id"),
            customerName = getString("customerName"),
            address = getString("address"),
            location = if (latitude != null && longitude != null) {
                GeoPoint(latitude, longitude)
            } else {
                null
            },
            orderIndex = getInt("orderIndex"),
            status = StopStatus.valueOf(getString("status")),
            stopType = if (has("stopType") && !isNull("stopType")) {
                StopType.valueOf(getString("stopType"))
            } else {
                StopType.Delivery
            },
            packageCount = if (has("packageCount") && !isNull("packageCount")) {
                getInt("packageCount").coerceAtLeast(1)
            } else {
                1
            },
            notes = optString("notes"),
            failureReason = optString("failureReason"),
        )
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        return if (has(key) && !isNull(key)) optDouble(key) else null
    }

    private companion object {
        const val PrefsName = "ruts_routes"
        const val RoutesKey = "routes"
        const val LastRouteKey = "last_route_id"
    }
}
