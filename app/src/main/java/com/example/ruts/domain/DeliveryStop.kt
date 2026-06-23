package com.example.ruts.domain

import java.util.UUID

enum class StopStatus {
    Pending,
    Delivered,
    Failed,
}

enum class StopType {
    Delivery,
    Pickup,
}

enum class StopOrderPreference {
    First,
    Automatic,
    Last,
}

const val DEFAULT_SERVICE_MINUTES = 5

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class DeliveryStop(
    val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val address: String,
    val location: GeoPoint? = null,
    val orderIndex: Int,
    val status: StopStatus = StopStatus.Pending,
    val stopType: StopType = StopType.Delivery,
    val packageCount: Int = 1,
    val serviceMinutes: Int = DEFAULT_SERVICE_MINUTES,
    val notes: String = "",
    val orderPreference: StopOrderPreference = StopOrderPreference.Automatic,
    val failureReason: String = "",
)
