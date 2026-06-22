package com.example.ruts.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private val MinimalStreetTileSource = XYTileSource(
    "CartoVoyager",
    0,
    20,
    256,
    ".png",
    arrayOf("https://basemaps.cartocdn.com/rastertiles/voyager/"),
    "© CARTO © OpenStreetMap contributors",
)

@Composable
fun RouteMapView(
    currentLocation: GeoPoint?,
    startLocation: GeoPoint?,
    stops: List<DeliveryStop>,
    activeStopId: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val cameraSignature = remember(stops, activeStopId, startLocation, currentLocation) {
        buildString {
            append("active=$activeStopId")
            append("|start=${startLocation?.latitude},${startLocation?.longitude}")
            append("|current=${currentLocation?.latitude},${currentLocation?.longitude}")
            stops.forEach { stop ->
                append("|${stop.id}:${stop.location?.latitude},${stop.location?.longitude}")
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setTileSource(MinimalStreetTileSource)
            setMultiTouchControls(true)
            setBackgroundColor(Color.parseColor("#101010"))
            minZoomLevel = 5.0
            maxZoomLevel = 20.0
            controller.setZoom(15.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            val rotationOverlay = RotationGestureOverlay(this).apply {
                isEnabled = true
            }
            overlays.add(rotationOverlay)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    LaunchedEffect(cameraSignature) {
        val stopLocations = stops.mapNotNull { it.location }
        val points = buildList {
            currentLocation?.let { add(it) }
            startLocation?.let { add(it) }
            addAll(stopLocations)
        }

        if (points.isEmpty()) {
            return@LaunchedEffect
        }

        if (points.size == 1) {
            val point = points.first()
            mapView.controller.setCenter(OsmGeoPoint(point.latitude, point.longitude))
            mapView.controller.setZoom(15.0)
            return@LaunchedEffect
        }

        val boundingBox = BoundingBox.fromGeoPoints(
            points.map { OsmGeoPoint(it.latitude, it.longitude) },
        )
        mapView.zoomToBoundingBox(boundingBox.increaseByScale(1.15f), false)
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.removeAll { overlay -> overlay is Marker }

            currentLocation?.let { location ->
                map.overlays += Marker(map).apply {
                    position = OsmGeoPoint(location.latitude, location.longitude)
                    title = "Tu ubicación"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            }

            startLocation?.let { location ->
                map.overlays += Marker(map).apply {
                    position = OsmGeoPoint(location.latitude, location.longitude)
                    title = "Inicio de ruta"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            }

            stops.sortedBy { it.orderIndex }.forEachIndexed { index, stop ->
                val location = stop.location ?: return@forEachIndexed
                val isActive = stop.id == activeStopId

                map.overlays += Marker(map).apply {
                    position = OsmGeoPoint(location.latitude, location.longitude)
                    title = "Parada ${index + 1}: ${stop.address}"
                    icon = createNumberedMarkerDrawable(
                        resources = context.resources,
                        number = index + 1,
                        isActive = isActive,
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            }

            map.invalidate()
        },
    )
}

private fun createNumberedMarkerDrawable(
    resources: android.content.res.Resources,
    number: Int,
    isActive: Boolean,
): BitmapDrawable {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) Color.rgb(76, 175, 80) else Color.rgb(255, 255, 255)
        style = Paint.Style.FILL
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) Color.WHITE else Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 34f
        typeface = Typeface.DEFAULT_BOLD
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    canvas.drawCircle(size / 2f, size / 2.4f, 34f, fill)
    if (isActive) {
        canvas.drawCircle(size / 2f, size / 2.4f, 34f, stroke)
    }
    canvas.drawText(number.toString(), size / 2f, size / 2.4f + 12f, text)

    return BitmapDrawable(resources, bitmap)
}
