package com.example.ruts.presentation.components

import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.StopOrderPreference
import com.example.ruts.domain.StopStatus
import com.example.ruts.domain.StopType
import com.example.ruts.routing.OsrmRouteClient
import com.example.ruts.ui.theme.pendingMarkerArgb
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private val MinimalStreetTileSource = XYTileSource(
    "CartoPositron",
    0,
    20,
    256,
    "@2x.png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/",
        "https://d.basemaps.cartocdn.com/light_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
)

private const val MAP_BACKGROUND_COLOR = "#FAFAFA"

private const val ACTIVE_STOP_ZOOM = 16.0
private const val OVERVIEW_ZOOM = 15.0
// ~65% opacity — lets street labels show through like Google Maps navigation
private const val ROUTE_LINE_COLOR = 0xA64285F4.toInt()
private const val ROUTE_LINE_WIDTH = 13f

@Composable
fun RouteMapView(
    currentLocation: GeoPoint?,
    startLocation: GeoPoint?,
    stops: List<DeliveryStop>,
    activeStopId: String?,
    modifier: Modifier = Modifier,
    drawRoutePath: Boolean = false,
    roundTrip: Boolean = true,
    onStopClick: ((String) -> Unit)? = null,
    focusPoint: GeoPoint? = null,
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val osrmUserAgent = remember { "${context.packageName}/1.0 (Android)" }

    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
            tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
            expirationOverrideDuration = 1000L * 60 * 60 * 24 * 30
        }
    }

    val cameraSignature = remember(stops, activeStopId, startLocation, focusPoint) {
        buildString {
            append("active=$activeStopId")
            append("|focus=${focusPoint?.latitude},${focusPoint?.longitude}")
            append("|start=${startLocation?.latitude},${startLocation?.longitude}")
            stops.forEach { stop ->
                append("|${stop.id}:${stop.location?.latitude},${stop.location?.longitude}")
            }
        }
    }

    val routeWaypoints = remember(drawRoutePath, roundTrip, startLocation, stops) {
        if (!drawRoutePath || startLocation == null) {
            emptyList()
        } else {
            buildList {
                add(startLocation)
                stops.sortedBy { it.orderIndex }.forEach { stop ->
                    val location = stop.location ?: return@forEach
                    add(location)
                }
                if (roundTrip && stops.isNotEmpty()) {
                    add(startLocation)
                }
            }
        }
    }
    val routePathSignature = remember(routeWaypoints) {
        routeWaypoints.joinToString(separator = "|") { point ->
            "${point.latitude},${point.longitude}"
        }
    }
    var routedRoutePath by remember { mutableStateOf<List<GeoPoint>?>(null) }

    LaunchedEffect(routePathSignature, osrmUserAgent) {
        routedRoutePath = null
        if (routeWaypoints.size >= 2) {
            routedRoutePath = OsrmRouteClient.fetchRouteGeometry(
                waypoints = routeWaypoints,
                userAgent = osrmUserAgent,
            )
        }
    }

    LaunchedEffect(routedRoutePath, mapView) {
        mapView?.post {
            mapView?.invalidate()
        }
    }

    DisposableEffect(mapView) {
        mapView?.onResume()
        onDispose { mapView?.onPause() }
    }

    LaunchedEffect(cameraSignature, mapView) {
        val map = mapView ?: return@LaunchedEffect
        val activeStop = stops.firstOrNull { it.id == activeStopId }
        val activePoint = activeStop?.location ?: focusPoint

        if (activePoint != null) {
            val target = OsmGeoPoint(activePoint.latitude, activePoint.longitude)
            map.controller.animateTo(target)
            map.controller.setZoom(ACTIVE_STOP_ZOOM)
            return@LaunchedEffect
        }

        val stopLocations = stops.mapNotNull { it.location }
        val points = buildList {
            startLocation?.let { add(it) }
            addAll(stopLocations)
        }

        if (points.isEmpty()) {
            return@LaunchedEffect
        }

        if (points.size == 1) {
            val point = points.first()
            map.controller.setCenter(OsmGeoPoint(point.latitude, point.longitude))
            map.controller.setZoom(OVERVIEW_ZOOM)
            return@LaunchedEffect
        }

        val boundingBox = BoundingBox.fromGeoPoints(
            points.map { OsmGeoPoint(it.latitude, it.longitude) },
        )
        map.zoomToBoundingBox(boundingBox.increaseByScale(1.15f), false)
    }

  Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                mapView?.let { map ->
                    if (size.width > 0 && size.height > 0) {
                        map.layout(0, 0, size.width, size.height)
                        map.invalidate()
                    }
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setTileSource(MinimalStreetTileSource)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    setBackgroundColor(Color.parseColor(MAP_BACKGROUND_COLOR))
                    minZoomLevel = 5.0
                    maxZoomLevel = 20.0
                    controller.setZoom(15.0)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false

                    val rotationOverlay = RotationGestureOverlay(this).apply {
                        isEnabled = true
                    }
                    overlays.add(rotationOverlay)

                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        val width = right - left
                        val height = bottom - top
                        val oldWidth = oldRight - oldLeft
                        val oldHeight = oldBottom - oldTop
                        if (width > 0 && height > 0 && (width != oldWidth || height != oldHeight)) {
                            invalidate()
                        }
                    }
                }.also { created -> mapView = created }
            },
            update = { map ->
                map.overlays.removeAll { overlay -> overlay is Marker || overlay is Polyline }

                if (drawRoutePath && routeWaypoints.isNotEmpty()) {
                    val routePoints = (routedRoutePath ?: routeWaypoints).map { point ->
                        OsmGeoPoint(point.latitude, point.longitude)
                    }

                    if (routePoints.size >= 2) {
                        map.overlays += Polyline().apply {
                            setPoints(routePoints)
                            applyRouteLineStyle(ROUTE_LINE_COLOR, ROUTE_LINE_WIDTH)
                        }
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
                        title = buildString {
                            append("Parada ${index + 1}: ${stop.address}")
                            when (stop.orderPreference) {
                                StopOrderPreference.First -> append(" · Primera")
                                StopOrderPreference.Last -> append(" · Última")
                                StopOrderPreference.Automatic -> Unit
                            }
                        }
                        if (onStopClick != null) {
                            setOnMarkerClickListener { _, _ ->
                                onStopClick(stop.id)
                                true
                            }
                        }
                        icon = createNumberedMarkerDrawable(
                            resources = context.resources,
                            number = index + 1,
                            isActive = isActive,
                            status = stop.status,
                            stopType = stop.stopType,
                            orderPreference = stop.orderPreference,
                            useBlueHighlight = drawRoutePath,
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                }

                currentLocation?.let { location ->
                    map.overlays += Marker(map).apply {
                        position = OsmGeoPoint(location.latitude, location.longitude)
                        title = "Tu ubicación"
                        icon = createCurrentLocationMarkerDrawable(context.resources)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                }

                map.post {
                    map.requestLayout()
                    map.invalidate()
                }
            },
            onRelease = { map ->
                map.onPause()
                if (mapView == map) {
                    mapView = null
                }
            },
        )
    }
}

private fun createCurrentLocationMarkerDrawable(
    resources: android.content.res.Resources,
): BitmapDrawable {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val outerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF")
        style = Paint.Style.FILL
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, outerRing)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, fill)

    return BitmapDrawable(resources, bitmap)
}

private const val PREFERENCE_BADGE_FIRST_COLOR = "#34C759"
private const val PREFERENCE_BADGE_LAST_COLOR = "#FF9500"

private fun createNumberedMarkerDrawable(
    resources: android.content.res.Resources,
    number: Int,
    isActive: Boolean,
    status: StopStatus,
    stopType: StopType,
    orderPreference: StopOrderPreference = StopOrderPreference.Automatic,
    useBlueHighlight: Boolean = false,
): BitmapDrawable {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (status) {
            StopStatus.Delivered -> Color.rgb(142, 142, 147)
            StopStatus.Failed -> Color.rgb(229, 57, 53)
            StopStatus.Pending -> pendingMarkerArgb(stopType, isActive, useBlueHighlight)
        }
        style = Paint.Style.FILL
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            status == StopStatus.Pending && !isActive -> Color.BLACK
            else -> Color.WHITE
        }
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
    if (isActive || status != StopStatus.Pending) {
        canvas.drawCircle(size / 2f, size / 2.4f, 34f, stroke)
    }
    val markerLabel = if (status == StopStatus.Failed) "X" else number.toString()
    canvas.drawText(markerLabel, size / 2f, size / 2.4f + 12f, text)

    if (status == StopStatus.Pending && orderPreference != StopOrderPreference.Automatic) {
        drawOrderPreferenceBadge(
            canvas = canvas,
            orderPreference = orderPreference,
            markerSize = size,
        )
    }

    return BitmapDrawable(resources, bitmap)
}

private fun drawOrderPreferenceBadge(
    canvas: Canvas,
    orderPreference: StopOrderPreference,
    markerSize: Int,
) {
    val badgeRadius = 14f
    val centerX = markerSize - 20f
    val centerY = 20f
    val fillColor = when (orderPreference) {
        StopOrderPreference.First -> Color.parseColor(PREFERENCE_BADGE_FIRST_COLOR)
        StopOrderPreference.Last -> Color.parseColor(PREFERENCE_BADGE_LAST_COLOR)
        StopOrderPreference.Automatic -> return
    }
    val label = when (orderPreference) {
        StopOrderPreference.First -> "↑"
        StopOrderPreference.Last -> "↓"
        StopOrderPreference.Automatic -> return
    }

    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }

    canvas.drawCircle(centerX, centerY, badgeRadius + 2f, ring)
    canvas.drawCircle(centerX, centerY, badgeRadius, fill)
    canvas.drawText(label, centerX, centerY + 6f, text)
}

private fun Polyline.applyRouteLineStyle(strokeColor: Int, strokeWidth: Float) {
    outlinePaint.color = strokeColor
    outlinePaint.strokeWidth = strokeWidth
    outlinePaint.isAntiAlias = true
    outlinePaint.strokeJoin = Paint.Join.ROUND
    outlinePaint.strokeCap = Paint.Cap.ROUND
}
