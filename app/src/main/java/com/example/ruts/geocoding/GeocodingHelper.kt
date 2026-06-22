package com.example.ruts.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.RouteOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

data class AddressResult(
    val address: String,
    val location: GeoPoint,
    val locality: String? = null,
)

data class SearchBias(
    val locality: String? = null,
    val anchor: GeoPoint? = null,
)

class GeocodingHelper(context: Context) {
    private val geocoder = Geocoder(context, Locale.forLanguageTag("es-ES"))

    suspend fun search(query: String, bias: SearchBias? = null): List<AddressResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) {
            return@withContext emptyList()
        }

        val normalizedQuery = query.normalizedForSearch()
        val locality = bias?.locality?.takeIf { it.isNotBlank() }
        val biasedQuery = locality
            ?.takeUnless { normalizedQuery.contains(it.normalizedForSearch()) }
            ?.let { "$query $it" }

        val biasedResults = biasedQuery
            ?.let { geocode(it, bias) }
            .orEmpty()

        if (biasedResults.isNotEmpty()) {
            return@withContext biasedResults
        }

        geocode(query, bias)
    }

    suspend fun reverseGeocode(location: GeoPoint): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?: return@withContext null

        results.firstOrNull()?.getAddressLine(0)
    }

    suspend fun reverseGeocodeResult(location: GeoPoint): AddressResult? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?: return@withContext null

        results.firstOrNull()?.toAddressResult()
    }

    private fun geocode(query: String, bias: SearchBias?): List<AddressResult> {
        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocationName(query, 8) ?: emptyList()

        return results
            .mapNotNull { it.toAddressResult() }
            .distinctBy { it.address.normalizedForSearch() }
            .sortedWith(compareByDescending<AddressResult> { result ->
                val biasLocality = bias?.locality?.normalizedForSearch()
                biasLocality != null && result.locality?.normalizedForSearch() == biasLocality
            }.thenBy { result ->
                val anchor = bias?.anchor
                if (anchor != null) RouteOptimizer.distanceKm(anchor, result.location) else 0.0
            })
    }

    private fun Address.toAddressResult(): AddressResult? {
        val formattedAddress = getAddressLine(0) ?: return null
        if (!hasLatitude() || !hasLongitude()) return null

        return AddressResult(
            address = formattedAddress,
            location = GeoPoint(latitude, longitude),
            locality = locality ?: subAdminArea ?: adminArea,
        )
    }

    private fun String.normalizedForSearch(): String {
        return Normalizer.normalize(lowercase(Locale.forLanguageTag("es-ES")), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
    }
}
