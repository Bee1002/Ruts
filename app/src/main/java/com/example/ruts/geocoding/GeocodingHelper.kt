package com.example.ruts.geocoding

import android.content.Context
import android.location.Geocoder
import com.example.ruts.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AddressResult(
    val address: String,
    val location: GeoPoint,
)

class GeocodingHelper(context: Context) {
    private val geocoder = Geocoder(context, Locale.forLanguageTag("es-ES"))

    suspend fun search(query: String): List<AddressResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) {
            return@withContext emptyList()
        }

        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocationName(query, 8) ?: emptyList()

        results.mapNotNull { address ->
            val latitude = address.latitude
            val longitude = address.longitude
            val formattedAddress = address.getAddressLine(0) ?: return@mapNotNull null

            AddressResult(
                address = formattedAddress,
                location = GeoPoint(latitude, longitude),
            )
        }
    }

    suspend fun reverseGeocode(location: GeoPoint): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?: return@withContext null

        results.firstOrNull()?.getAddressLine(0)
    }
}
