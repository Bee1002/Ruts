package com.example.ruts.domain

import java.util.UUID

data class Route(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val optimizedAtMillis: Long? = null,
    val startLocation: GeoPoint? = null,
    val stops: List<DeliveryStop> = emptyList(),
)

fun Route.displayLabel(): String {
    val suffixNumber = parseRouteSuffix(name)
  return when {
        name.isNotBlank() && suffixNumber == null -> name
        suffixNumber != null -> "${formatDrawerRoutePrefix(createdAtMillis)} Ruta $suffixNumber"
        else -> formatDrawerRoutePrefix(createdAtMillis)
    }
}

fun Route.displaySubtitle(): String? {
    return if (name.isNotBlank() && !isRouteSuffixName(name)) {
        formatDrawerRoutePrefix(createdAtMillis)
    } else {
        null
    }
}
