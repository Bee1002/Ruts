package com.example.ruts.presentation.components

import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.StopOrderPreference
import com.example.ruts.domain.StopStatus
import com.example.ruts.domain.StopType
import com.example.ruts.routing.OsrmRouteClient
import com.example.ruts.ui.theme.pendingMarkerArgb
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapView as VectorMapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
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
private const val MARKER_BITMAP_SIZE = 96
private const val CURRENT_LOCATION_MARKER_SIZE = 48
private const val CURRENT_LOCATION_MARKER_COLOR = "#0A84FF"

private const val OVERVIEW_ZOOM = 15.0
// ~65% opacity — lets street labels show through like Google Maps navigation
private const val ROUTE_LINE_COLOR = 0xA64285F4.toInt()
private const val ROUTE_LINE_WIDTH = 13f
private const val USE_VECTOR_MAP_EXPERIMENT = true
private const val VECTOR_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
private const val VECTOR_ROUTE_SOURCE_ID = "ruts-route-source"
private const val VECTOR_ROUTE_LAYER_ID = "ruts-route-layer"

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
    if (USE_VECTOR_MAP_EXPERIMENT && drawRoutePath) {
        VectorRouteMapView(
            currentLocation = currentLocation,
            startLocation = startLocation,
            stops = stops,
            activeStopId = activeStopId,
            modifier = modifier,
            drawRoutePath = drawRoutePath,
            roundTrip = roundTrip,
            onStopClick = onStopClick,
            focusPoint = focusPoint,
        )
    } else {
        OsmdroidRouteMapView(
            currentLocation = currentLocation,
            startLocation = startLocation,
            stops = stops,
            activeStopId = activeStopId,
            modifier = modifier,
            drawRoutePath = drawRoutePath,
            roundTrip = roundTrip,
            onStopClick = onStopClick,
            focusPoint = focusPoint,
        )
    }
}

@Composable
private fun OsmdroidRouteMapView(
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
    val resources = LocalResources.current
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
    var previousActiveStopId by remember { mutableStateOf<String?>(null) }

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
        val deselectedStop = previousActiveStopId != null && activeStopId == null
        previousActiveStopId = activeStopId

        val activeStop = stops.firstOrNull { it.id == activeStopId }
        val activePoint = activeStop?.location ?: focusPoint

        if (activePoint != null) {
            // Pan only: keep the user's current zoom while editing nearby stops.
            val target = OsmGeoPoint(activePoint.latitude, activePoint.longitude)
            map.controller.animateTo(target)
            return@LaunchedEffect
        }

        if (deselectedStop) {
            // User collapsed the sheet to keep editing on the map — preserve zoom.
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
                    setBackgroundColor(MAP_BACKGROUND_COLOR.toColorInt())
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
                            resources = resources,
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
                        icon = createCurrentLocationMarkerDrawable(resources)
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

@Composable
private fun VectorRouteMapView(
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
    var vectorMapView by remember { mutableStateOf<VectorMapView?>(null) }
    var vectorMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }
    val osrmUserAgent = remember { "${context.packageName}/1.0 (Android)" }

    remember {
        MapLibre.getInstance(context.applicationContext)
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
    var previousActiveStopId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(routePathSignature, osrmUserAgent) {
        routedRoutePath = null
        if (routeWaypoints.size >= 2) {
            routedRoutePath = OsrmRouteClient.fetchRouteGeometry(
                waypoints = routeWaypoints,
                userAgent = osrmUserAgent,
            )
        }
    }

    val renderSignature = remember(
        currentLocation,
        startLocation,
        stops,
        activeStopId,
        focusPoint,
        routedRoutePath,
    ) {
        buildString {
            append("active=$activeStopId")
            append("|focus=${focusPoint?.latitude},${focusPoint?.longitude}")
            append("|current=${currentLocation?.latitude},${currentLocation?.longitude}")
            append("|start=${startLocation?.latitude},${startLocation?.longitude}")
            stops.forEach { stop ->
                append("|${stop.id}:${stop.location?.latitude},${stop.location?.longitude}:${stop.status}:${stop.stopType}:${stop.orderPreference}")
            }
            routedRoutePath?.forEach { point ->
                append("|route=${point.latitude},${point.longitude}")
            }
        }
    }

    LaunchedEffect(renderSignature, vectorMap, isStyleReady) {
        val map = vectorMap ?: return@LaunchedEffect
        if (!isStyleReady) return@LaunchedEffect

        renderVectorRouteMap(
            map = map,
            appContext = context.applicationContext,
            currentLocation = currentLocation,
            startLocation = startLocation,
            stops = stops,
            activeStopId = activeStopId,
            routePath = routedRoutePath ?: routeWaypoints,
            onStopClick = onStopClick,
        )
    }

    LaunchedEffect(cameraSignature, vectorMap, isStyleReady) {
        val map = vectorMap ?: return@LaunchedEffect
        if (!isStyleReady) return@LaunchedEffect

        val deselectedStop = previousActiveStopId != null && activeStopId == null
        previousActiveStopId = activeStopId

        focusVectorCamera(
            map = map,
            activePoint = stops.firstOrNull { it.id == activeStopId }?.location ?: focusPoint,
            points = buildList {
                startLocation?.let { add(it) }
                addAll(stops.mapNotNull { it.location })
            },
            preserveZoom = deselectedStop,
        )
    }

    DisposableEffect(vectorMapView) {
        vectorMapView?.onStart()
        vectorMapView?.onResume()
        onDispose {
            vectorMapView?.onPause()
            vectorMapView?.onStop()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapLibre.getInstance(ctx.applicationContext)
            VectorMapView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                onCreate(null)
                getMapAsync { map ->
                    vectorMap = map
                    map.setStyle(VECTOR_MAP_STYLE_URL) {
                        isStyleReady = true
                    }
                }
            }.also { created -> vectorMapView = created }
        },
        onRelease = { map ->
            map.onPause()
            map.onStop()
            map.onDestroy()
            if (vectorMapView == map) {
                vectorMapView = null
                vectorMap = null
                isStyleReady = false
            }
        },
    )
}

@Suppress("DEPRECATION")
private fun renderVectorRouteMap(
    map: MapLibreMap,
    appContext: android.content.Context,
    currentLocation: GeoPoint?,
    startLocation: GeoPoint?,
    stops: List<DeliveryStop>,
    activeStopId: String?,
    routePath: List<GeoPoint>,
    onStopClick: ((String) -> Unit)?,
) {
    val iconFactory = org.maplibre.android.annotations.IconFactory.getInstance(appContext)

    map.clear()
    map.setOnMarkerClickListener { marker ->
        marker.snippet?.let { stopId ->
            onStopClick?.invoke(stopId)
            true
        } ?: false
    }

    val routePoints = routePath.map { point -> point.toLatLng() }
    map.getStyle { style ->
        upsertVectorRouteLine(style, routePoints)
    }

    startLocation?.let { location ->
        map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(location.toLatLng())
                .title("Inicio de ruta"),
        )
    }

    stops.sortedBy { it.orderIndex }.forEachIndexed { index, stop ->
        val location = stop.location ?: return@forEachIndexed
        val markerBitmap = createNumberedMarkerBitmap(
            number = index + 1,
            isActive = stop.id == activeStopId,
            status = stop.status,
            stopType = stop.stopType,
            orderPreference = stop.orderPreference,
            useBlueHighlight = true,
        )

        map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(location.toLatLng())
                .title("Parada ${index + 1}: ${stop.address}")
                .snippet(stop.id)
                .icon(iconFactory.fromBitmap(markerBitmap)),
        )
    }

    currentLocation?.let { location ->
        val currentLocationBitmap = createCurrentLocationMarkerBitmap()
        map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(location.toLatLng())
                .title("Tu ubicación")
                .icon(iconFactory.fromBitmap(currentLocationBitmap)),
        )
    }
}

private fun upsertVectorRouteLine(
    style: Style,
    routePoints: List<LatLng>,
) {
    if (routePoints.size < 2) {
        style.getLayerOrNull(VECTOR_ROUTE_LAYER_ID)?.let { style.removeLayer(it) }
        style.getSourceOrNull(VECTOR_ROUTE_SOURCE_ID)?.let { style.removeSource(it) }
        return
    }

    val routeGeometry = LineString.fromLngLats(
        routePoints.map { point -> Point.fromLngLat(point.longitude, point.latitude) },
    )
    val existingSource = style.getSourceOrNull(VECTOR_ROUTE_SOURCE_ID) as? GeoJsonSource
    if (existingSource != null) {
        existingSource.setGeoJson(Feature.fromGeometry(routeGeometry))
    } else {
        style.addSource(
            GeoJsonSource(
                VECTOR_ROUTE_SOURCE_ID,
                Feature.fromGeometry(routeGeometry),
            ),
        )
    }

    if (style.getLayerOrNull(VECTOR_ROUTE_LAYER_ID) != null) return

    val routeLayer = LineLayer(VECTOR_ROUTE_LAYER_ID, VECTOR_ROUTE_SOURCE_ID)
        .withProperties(
            lineColor(ROUTE_LINE_COLOR),
            lineWidth(ROUTE_LINE_WIDTH),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
    val firstLabelLayerId = style.getLayers()
        .firstOrNull { layer -> layer.getId().contains("label", ignoreCase = true) }
        ?.getId()

    if (firstLabelLayerId != null) {
        style.addLayerBelow(routeLayer, firstLabelLayerId)
    } else {
        style.addLayer(routeLayer)
    }
}

private fun Style.getLayerOrNull(layerId: String) = try {
    getLayer(layerId)
} catch (_: RuntimeException) {
    null
}

private fun Style.getSourceOrNull(sourceId: String) = try {
    getSource(sourceId)
} catch (_: RuntimeException) {
    null
}

private fun focusVectorCamera(
    map: MapLibreMap,
    activePoint: GeoPoint?,
    points: List<GeoPoint>,
    preserveZoom: Boolean = false,
) {
    if (activePoint != null) {
        // Pan only: keep the user's current zoom while editing nearby stops.
        map.animateCamera(
            CameraUpdateFactory.newLatLng(activePoint.toLatLng()),
        )
        return
    }

    if (preserveZoom) return

    if (points.isEmpty()) return

    if (points.size == 1) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), OVERVIEW_ZOOM),
        )
        return
    }

    val boundsBuilder = LatLngBounds.Builder()
    points.forEach { point -> boundsBuilder.include(point.toLatLng()) }
    map.moveCamera(
        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 96),
    )
}

private fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun createCurrentLocationMarkerDrawable(
    resources: android.content.res.Resources,
): Drawable = createCurrentLocationMarkerBitmap().toDrawable(resources)

private const val PREFERENCE_BADGE_FIRST_COLOR = "#34C759"
private const val PREFERENCE_BADGE_LAST_COLOR = "#FF9500"

private fun createCurrentLocationMarkerBitmap(): Bitmap {
    val size = CURRENT_LOCATION_MARKER_SIZE
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val outerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CURRENT_LOCATION_MARKER_COLOR.toColorInt()
        style = Paint.Style.FILL
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, outerRing)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, fill)

    return bitmap
}

private fun createNumberedMarkerDrawable(
    resources: android.content.res.Resources,
    number: Int,
    isActive: Boolean,
    status: StopStatus,
    stopType: StopType,
    orderPreference: StopOrderPreference = StopOrderPreference.Automatic,
    useBlueHighlight: Boolean = false,
): Drawable = createNumberedMarkerBitmap(
    number = number,
    isActive = isActive,
    status = status,
    stopType = stopType,
    orderPreference = orderPreference,
    useBlueHighlight = useBlueHighlight,
).toDrawable(resources)

private fun createNumberedMarkerBitmap(
    number: Int,
    isActive: Boolean,
    status: StopStatus,
    stopType: StopType,
    orderPreference: StopOrderPreference = StopOrderPreference.Automatic,
    useBlueHighlight: Boolean = false,
): Bitmap {
    val size = MARKER_BITMAP_SIZE
    val bitmap = createBitmap(size, size)
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
            status == StopStatus.Pending && !isActive && stopType == StopType.Delivery -> Color.BLACK
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
    if (isActive || status != StopStatus.Pending || stopType == StopType.Pickup) {
        canvas.drawCircle(size / 2f, size / 2.4f, 34f, stroke)
    }
    val markerLabel = if (status == StopStatus.Failed) "X" else number.toString()
    canvas.drawText(markerLabel, size / 2f, size / 2.4f + 12f, text)

    if (status == StopStatus.Pending && orderPreference != StopOrderPreference.Automatic) {
        drawOrderPreferenceBadge(
            canvas = canvas,
            orderPreference = orderPreference,
        )
    }

    return bitmap
}

private fun drawOrderPreferenceBadge(
    canvas: Canvas,
    orderPreference: StopOrderPreference,
) {
    val badgeRadius = 14f
    val centerX = MARKER_BITMAP_SIZE - 20f
    val centerY = 20f
    val fillColor = when (orderPreference) {
        StopOrderPreference.First -> PREFERENCE_BADGE_FIRST_COLOR.toColorInt()
        StopOrderPreference.Last -> PREFERENCE_BADGE_LAST_COLOR.toColorInt()
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
