package com.example.ruts.domain

import java.util.UUID

data class Route(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val startLocation: GeoPoint? = null,
    val stops: List<DeliveryStop> = emptyList(),
)

fun Route.displayLabel(): String {
    return if (name.isNotBlank()) name else formatShortRouteLabel(createdAtMillis)
}

fun Route.displaySubtitle(): String? {
    return if (name.isNotBlank()) formatShortRouteLabel(createdAtMillis) else null
}
