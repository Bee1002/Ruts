package com.example.ruts.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.ruts.domain.GeoPoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationHelper(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GeoPoint? {
        return getFreshLocation() ?: getLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): GeoPoint? {
        return withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { continuation ->
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        val location = result.lastLocation
                        if (continuation.isActive) {
                            continuation.resume(
                                location?.let { GeoPoint(it.latitude, it.longitude) },
                            )
                        }
                    }
                }

                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener {
                        fusedClient.removeLocationUpdates(callback)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): GeoPoint? {
        return suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(GeoPoint(location.latitude, location.longitude))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
