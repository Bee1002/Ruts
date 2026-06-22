package com.example.ruts.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.ruts.domain.DeliveryStop

object MapsNavigator {
    fun openNavigation(context: Context, stop: DeliveryStop) {
        val destination = stop.location?.let { location ->
            "${location.latitude},${location.longitude}"
        } ?: stop.address

        val navigationUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri)
            .setPackage("com.google.android.apps.maps")

        if (navigationIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navigationIntent)
            return
        }

        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}
