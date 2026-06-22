package com.example.ruts.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.ruts.data.RouteRepository
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.Route
import com.example.ruts.domain.RouteEstimator
import com.example.ruts.domain.RouteOptimizer
import com.example.ruts.domain.formatDuration
import com.example.ruts.domain.displayLabel
import com.example.ruts.geocoding.AddressResult
import com.example.ruts.geocoding.GeocodingHelper
import com.example.ruts.location.LocationHelper
import com.example.ruts.presentation.components.CompactAddressSearchBar
import com.example.ruts.presentation.components.ExpandedAddressSearchBar
import com.example.ruts.presentation.components.OptimizedRouteOverview
import com.example.ruts.presentation.components.RouteOptimizationOverlay
import com.example.ruts.presentation.components.RouteMapView
import com.example.ruts.presentation.components.StopDetailEditor
import com.example.ruts.presentation.components.RouteStopsOverview
import androidx.compose.ui.unit.dp
import com.example.ruts.ui.theme.RutsUi
import com.example.ruts.ui.theme.Success
import com.example.ruts.ui.theme.SurfaceCard
import com.example.ruts.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class OptimizationUiState {
    Idle,
    Optimizing,
    Optimized,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(
    routeId: String,
    repository: RouteRepository,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val collapsedPeek = 320.dp
    val sheetState = rememberBottomSheetScaffoldState()
    val geocodingHelper = remember { GeocodingHelper(context) }
    val locationHelper = remember { LocationHelper(context) }

    var route by remember { mutableStateOf<Route?>(null) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var startAddress by remember { mutableStateOf<String?>(null) }
    var searchExpanded by remember { mutableStateOf(true) }
    var selectedStopId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AddressResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var optimizationState by remember { mutableStateOf(OptimizationUiState.Idle) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch {
                currentLocation = locationHelper.getCurrentLocation()
                route?.let { currentRoute ->
                    if (currentRoute.startLocation == null && currentLocation != null) {
                        val updated = currentRoute.copy(startLocation = currentLocation)
                        route = updated
                        repository.saveRoute(updated)
                    }
                }
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                searchExpanded = true
                searchQuery = spokenText
            }
        }
    }

    LaunchedEffect(routeId) {
        route = repository.getRoute(routeId)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    LaunchedEffect(route?.startLocation, currentLocation) {
        val location = route?.startLocation ?: currentLocation ?: return@LaunchedEffect
        startAddress = geocodingHelper.reverseGeocode(location)
    }

    LaunchedEffect(searchQuery, searchExpanded) {
        searchJob?.cancel()
        if (!searchExpanded || searchQuery.length < 3) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchJob = scope.launch {
            delay(400)
            searchResults = geocodingHelper.search(searchQuery)
            isSearching = false
        }
    }

    LaunchedEffect(sheetState.bottomSheetState.currentValue) {
        when (sheetState.bottomSheetState.currentValue) {
            SheetValue.PartiallyExpanded -> {
                if (!searchExpanded) {
                    selectedStopId = null
                }
            }
            SheetValue.Hidden -> {
                scope.launch { sheetState.bottomSheetState.partialExpand() }
            }
            else -> Unit
        }
    }

    fun returnToOverview(expandSheet: Boolean = false) {
        selectedStopId = null
        searchExpanded = false
        searchQuery = ""
        searchResults = emptyList()
        scope.launch {
            if (expandSheet) {
                sheetState.bottomSheetState.expand()
            } else {
                sheetState.bottomSheetState.partialExpand()
            }
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("es-ES"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di la calle o dirección")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            voiceLauncher.launch(intent)
        }
    }

    fun addStop(result: AddressResult) {
        val currentRoute = route ?: return
        val newStop = DeliveryStop(
            customerName = result.address.substringBefore(",").ifBlank { "Parada" },
            address = result.address,
            location = result.location,
            orderIndex = currentRoute.stops.size,
        )
        val updatedRoute = currentRoute.copy(stops = currentRoute.stops + newStop)
        route = updatedRoute
        repository.saveRoute(updatedRoute)
        selectedStopId = newStop.id
        searchQuery = ""
        searchResults = emptyList()
        searchExpanded = false
        scope.launch {
            sheetState.bottomSheetState.expand()
        }
    }

    fun updateStop(stopId: String, transform: (DeliveryStop) -> DeliveryStop) {
        val currentRoute = route ?: return
        val updatedRoute = currentRoute.copy(
            stops = currentRoute.stops.map { stop ->
                if (stop.id == stopId) transform(stop) else stop
            },
        )
        route = updatedRoute
        repository.saveRoute(updatedRoute)
    }

    fun deleteStop(stopId: String) {
        val currentRoute = route ?: return
        val updatedStops = currentRoute.stops
            .filterNot { it.id == stopId }
            .sortedBy { it.orderIndex }
            .mapIndexed { index, stop -> stop.copy(orderIndex = index) }
        val updatedRoute = currentRoute.copy(stops = updatedStops)
        route = updatedRoute
        repository.saveRoute(updatedRoute)
        selectedStopId = null
        if (updatedStops.isEmpty()) {
            searchExpanded = true
        }
        scope.launch {
            sheetState.bottomSheetState.partialExpand()
        }
    }

    fun optimizeRoute() {
        val currentRoute = route ?: return
        val startPoint = currentRoute.startLocation ?: currentLocation ?: return
        if (currentRoute.stops.size < 2) return

        optimizationState = OptimizationUiState.Optimizing

        scope.launch {
            val optimizedDeferred = async {
                RouteOptimizer.optimize(currentRoute.stops, startPoint)
            }
            delay(2500)
            val optimizedStops = optimizedDeferred.await()
            val updatedRoute = currentRoute.copy(stops = optimizedStops)
            route = updatedRoute
            repository.saveRoute(updatedRoute)
            optimizationState = OptimizationUiState.Optimized
            sheetState.bottomSheetState.partialExpand()
        }
    }

    fun adjustOptimizedRoute() {
        optimizationState = OptimizationUiState.Idle
        scope.launch { sheetState.bottomSheetState.expand() }
    }

    fun confirmOptimizedRoute() {
        optimizationState = OptimizationUiState.Idle
        onFinished()
    }

    val currentRoute = route
    val selectedStop = currentRoute?.stops?.firstOrNull { it.id == selectedStopId }
    val isSheetExpanded = sheetState.bottomSheetState.currentValue == SheetValue.Expanded
    val isOptimizedView = optimizationState == OptimizationUiState.Optimized
    val startPoint = currentRoute?.startLocation ?: currentLocation
    val orderedStops = currentRoute?.stops?.sortedBy { it.orderIndex } ?: emptyList()
    val optimizedDurationMinutes = if (startPoint != null && orderedStops.isNotEmpty()) {
        RouteEstimator.estimatedDurationMinutes(orderedStops, startPoint)
    } else {
        0
    }
    val optimizedDistanceKm = if (startPoint != null && orderedStops.isNotEmpty()) {
        RouteEstimator.totalDistanceKm(orderedStops, startPoint)
    } else {
        0.0
    }
    val arrivalTimes = if (startPoint != null && currentRoute != null && isOptimizedView) {
        RouteEstimator.arrivalTimesMillis(currentRoute.createdAtMillis, startPoint, orderedStops)
    } else {
        emptyList()
    }
    val highlightedStopId = when {
        selectedStop != null -> selectedStop.id
        isOptimizedView -> orderedStops.firstOrNull()?.id
        else -> null
    }

    LaunchedEffect(currentRoute?.stops?.size) {
        if (selectedStopId == null && currentRoute?.stops?.isNotEmpty() == true && !searchExpanded) {
            searchExpanded = false
        }
    }

    if (currentRoute == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text("Ruta no encontrada", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            when {
                optimizationState == OptimizationUiState.Optimizing -> Unit

                isOptimizedView -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = formatDuration(optimizedDurationMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Success,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = ::adjustOptimizedRoute,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Ajustar")
                        }
                        Button(
                            onClick = ::confirmOptimizedRoute,
                            modifier = Modifier.weight(1f),
                            colors = RutsUi.primaryButtonColors,
                        ) {
                            Text("Confirmar")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Button(
                            onClick = {
                                searchExpanded = false
                                onFinished()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = RutsUi.primaryButtonColors,
                        ) {
                            Text(
                                if (currentRoute.stops.isEmpty()) {
                                    "Continuar sin paradas"
                                } else {
                                    "Continuar con la ruta"
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            BottomSheetScaffold(
            modifier = Modifier.padding(scaffoldPadding),
            scaffoldState = sheetState,
            sheetPeekHeight = collapsedPeek,
            containerColor = MaterialTheme.colorScheme.background,
            sheetContainerColor = MaterialTheme.colorScheme.background,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetDragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextSecondary.copy(alpha = 0.4f)),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            currentRoute.displayLabel(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Rutas",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            sheetContent = {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (!searchExpanded && (isOptimizedView || currentRoute.stops.isEmpty() || isSheetExpanded || selectedStop != null)) {
                        CompactAddressSearchBar(
                            onOpenExpanded = {
                                searchExpanded = true
                                scope.launch { sheetState.bottomSheetState.expand() }
                            },
                            onVoiceClick = { launchVoiceSearch() },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (searchExpanded) {
                            ExpandedAddressSearchBar(
                                query = searchQuery,
                                results = searchResults,
                                isSearching = isSearching,
                                onQueryChange = { searchQuery = it },
                                onVoiceClick = { launchVoiceSearch() },
                                onClose = {
                                    searchExpanded = false
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    if (currentRoute.stops.isEmpty()) {
                                        scope.launch { sheetState.bottomSheetState.partialExpand() }
                                    }
                                },
                                onResultSelected = ::addStop,
                            )
                        }

                        when {
                            searchExpanded -> Unit

                            isOptimizedView -> {
                                OptimizedRouteOverview(
                                    route = currentRoute,
                                    startAddress = startAddress,
                                    durationMinutes = optimizedDurationMinutes,
                                    distanceKm = optimizedDistanceKm,
                                    arrivalTimes = arrivalTimes,
                                    isSheetExpanded = isSheetExpanded,
                                )
                            }

                            selectedStop != null -> {
                                StopDetailEditor(
                                    stop = selectedStop,
                                    onNotesChange = { notes ->
                                        updateStop(selectedStop.id) { it.copy(notes = notes) }
                                    },
                                    onTypeChange = { type ->
                                        updateStop(selectedStop.id) { it.copy(stopType = type) }
                                    },
                                    onPackageCountChange = { count ->
                                        updateStop(selectedStop.id) {
                                            it.copy(packageCount = count.coerceAtLeast(1))
                                        }
                                    },
                                    onOrderPreferenceChange = { preference ->
                                        updateStop(selectedStop.id) { it.copy(orderPreference = preference) }
                                    },
                                    onDelete = { deleteStop(selectedStop.id) },
                                    onBack = { returnToOverview(expandSheet = true) },
                                )
                            }

                            currentRoute.stops.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceCard)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "Añade nuevas paradas",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        text = "Pulsa la barra de búsqueda para encontrar una dirección.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            else -> {
                                RouteStopsOverview(
                                    route = currentRoute,
                                    startAddress = startAddress,
                                    isSheetExpanded = isSheetExpanded,
                                    onSearchClick = {
                                        searchExpanded = true
                                        scope.launch { sheetState.bottomSheetState.expand() }
                                    },
                                    onMenuClick = {
                                        scope.launch { sheetState.bottomSheetState.expand() }
                                    },
                                    onStopClick = { stop ->
                                        selectedStopId = stop.id
                                        scope.launch { sheetState.bottomSheetState.expand() }
                                    },
                                    onOptimize = ::optimizeRoute,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            },
        ) { innerPadding ->
            RouteMapView(
                currentLocation = currentLocation,
                startLocation = currentRoute.startLocation ?: currentLocation,
                stops = currentRoute.stops,
                activeStopId = highlightedStopId,
                drawRoutePath = isOptimizedView,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )
        }

            RouteOptimizationOverlay(
                visible = optimizationState == OptimizationUiState.Optimizing,
            )
        }
    }
}
