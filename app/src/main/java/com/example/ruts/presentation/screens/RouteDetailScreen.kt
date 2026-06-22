package com.example.ruts.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ruts.data.RouteRepository
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.Route
import com.example.ruts.domain.RouteOptimizer
import com.example.ruts.domain.StopStatus
import com.example.ruts.domain.StopType
import com.example.ruts.domain.displayLabel
import com.example.ruts.domain.formatTime
import com.example.ruts.domain.RouteEstimator
import com.example.ruts.geocoding.GeocodingHelper
import com.example.ruts.maps.MapsNavigator
import com.example.ruts.presentation.components.CompletedStopDetailView
import com.example.ruts.presentation.components.PendingStopWorkView
import com.example.ruts.presentation.components.RouteCompletedView
import com.example.ruts.presentation.components.extractPostalCode
import com.example.ruts.presentation.components.RouteCompletionSummaryView
import com.example.ruts.presentation.components.RouteMapView
import com.example.ruts.domain.formatDistanceKm
import com.example.ruts.presentation.components.RoutesDrawerContent
import com.example.ruts.presentation.components.StopDetailEditor
import com.example.ruts.ui.theme.Error
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeId: String,
    repository: RouteRepository,
    onCreateRoute: () -> Unit,
    onEditRoute: (String) -> Unit,
    onRouteSelected: (String) -> Unit,
    onRouteDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val sheetState = rememberBottomSheetScaffoldState()

    val geocodingHelper = remember { GeocodingHelper(context) }

    var route by remember { mutableStateOf<Route?>(null) }
    var allRoutes by remember { mutableStateOf(emptyList<Route>()) }
    var selectedStopId by remember { mutableStateOf<String?>(null) }
    var statusChangedAtMillis by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var editingStopId by remember { mutableStateOf<String?>(null) }
    var showRouteCompleted by remember { mutableStateOf(false) }
    var showRouteSummary by remember { mutableStateOf(false) }
    var routeCompletedAtMillis by remember { mutableStateOf<Long?>(null) }
    var startAddress by remember { mutableStateOf<String?>(null) }

    fun reload(routeToSelect: String = routeId) {
        allRoutes = repository.getAllRoutes()
        route = if (routeToSelect.isNotBlank()) {
            repository.getRoute(routeToSelect)?.also { repository.setLastRouteId(routeToSelect) }
        } else {
            null
        }
    }

    LaunchedEffect(routeId) {
        selectedStopId = null
        editingStopId = null
        showRouteCompleted = false
        showRouteSummary = false
        routeCompletedAtMillis = null
        reload()
    }

    LaunchedEffect(route?.startLocation) {
        val location = route?.startLocation ?: return@LaunchedEffect
        startAddress = geocodingHelper.reverseGeocode(location)
    }

    fun persist(updatedRoute: Route) {
        repository.saveRoute(updatedRoute)
        route = updatedRoute
        allRoutes = repository.getAllRoutes()
    }

    fun updateStops(transform: (List<DeliveryStop>) -> List<DeliveryStop>) {
        val currentRoute = route ?: return
        persist(currentRoute.copy(stops = transform(currentRoute.stops).reindexStops()))
    }

    fun handleDeleteRoute(deletedRouteId: String) {
        val wasCurrentRoute = deletedRouteId == route?.id
        repository.deleteRoute(deletedRouteId)
        allRoutes = repository.getAllRoutes()

        if (wasCurrentRoute) {
            val nextRouteId = allRoutes.firstOrNull()?.id
            if (nextRouteId != null) {
                onRouteSelected(nextRouteId)
            } else {
                onRouteDeleted()
            }
        } else {
            reload()
        }
    }

    val currentRoute = route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                RoutesDrawerContent(
                    routes = allRoutes,
                    selectedRouteId = currentRoute?.id,
                    onRouteSelected = { selectedId ->
                        scope.launch {
                            drawerState.close()
                            onRouteSelected(selectedId)
                        }
                    },
                    onCreateRoute = {
                        scope.launch {
                            drawerState.close()
                            onCreateRoute()
                        }
                    },
                    onRenameRoute = { id, newName ->
                        repository.getRoute(id)?.let { existing ->
                            repository.saveRoute(existing.copy(name = newName))
                            reload(route?.id ?: routeId)
                        }
                    },
                    onDeleteRoute = { id ->
                        scope.launch {
                            drawerState.close()
                            handleDeleteRoute(id)
                        }
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        when {
            currentRoute == null && allRoutes.isEmpty() -> {
                EmptyRoutesState(onCreateRoute = onCreateRoute)
            }

            currentRoute == null -> {
                MissingRouteState()
            }

            else -> {
                val orderedStops = currentRoute.stops.sortedBy { it.orderIndex }
                val selectedStop = orderedStops.firstOrNull { it.id == selectedStopId }
                val nextPendingStop = orderedStops.firstOrNull { it.status == StopStatus.Pending }
                val allStopsResolved = orderedStops.isNotEmpty() && nextPendingStop == null
                val viewingResolvedStop = selectedStop != null && selectedStop.status != StopStatus.Pending
                val activeStop = when {
                    allStopsResolved && selectedStopId == null -> null
                    viewingResolvedStop -> selectedStop
                    selectedStop != null -> selectedStop
                    else -> nextPendingStop
                }
                val startPoint = currentRoute.startLocation ?: GeoPoint(40.4168, -3.7038)
                val resolvedStartAddress = startAddress ?: "Ubicación de inicio"

                LaunchedEffect(allStopsResolved) {
                    if (allStopsResolved) {
                        if (routeCompletedAtMillis == null) {
                            routeCompletedAtMillis = System.currentTimeMillis()
                        }
                        if (selectedStopId == null) {
                            showRouteCompleted = true
                            sheetState.bottomSheetState.expand()
                        }
                    } else {
                        showRouteCompleted = false
                        showRouteSummary = false
                        routeCompletedAtMillis = null
                    }
                }

                val routeDistanceLabel = formatDistanceKm(
                    RouteEstimator.totalDistanceKm(orderedStops, startPoint),
                )

                fun selectStop(stopId: String) {
                    selectedStopId = stopId
                    editingStopId = null
                    showRouteCompleted = false
                    showRouteSummary = false
                    scope.launch { sheetState.bottomSheetState.expand() }
                }

                fun clearStopSelection() {
                    selectedStopId = null
                    editingStopId = null
                }

                val activeStopSubtitle = if (activeStop != null) {
                    val position = activeStop.orderIndex + 1
                    val arrivalMillis = RouteEstimator.arrivalTimesMillis(
                        currentRoute.createdAtMillis,
                        startPoint,
                        orderedStops,
                    ).firstOrNull { it.first.id == activeStop.id }?.second
                    if (arrivalMillis != null) {
                        "$position/${orderedStops.size}, ${formatTime(arrivalMillis)}"
                    } else {
                        "$position/${orderedStops.size}"
                    }
                } else {
                    null
                }

                BottomSheetScaffold(
                    scaffoldState = sheetState,
                    sheetPeekHeight = 280.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    sheetContainerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        TopAppBar(
                            title = { Text(currentRoute.displayLabel()) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Rutas")
                                }
                            },
                            actions = {
                                IconButton(onClick = { onEditRoute(currentRoute.id) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar paradas")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    },
                    sheetContent = {
                        RouteWorkSheet(
                            route = currentRoute,
                            activeStop = activeStop,
                            viewingResolvedStop = viewingResolvedStop,
                            showRouteCompleted = showRouteCompleted && allStopsResolved,
                            showRouteSummary = showRouteSummary && allStopsResolved,
                            routeCompletedAtMillis = routeCompletedAtMillis,
                            routeDistanceLabel = routeDistanceLabel,
                            arrivalAddress = resolvedStartAddress,
                            arrivalPostalCode = extractPostalCode(resolvedStartAddress),
                            isStopFocused = selectedStopId != null,
                            activeStopSubtitle = activeStopSubtitle,
                            editingStopId = editingStopId,
                            activeStatusChangedAtMillis = statusChangedAtMillis[activeStop?.id],
                            onNavigate = {
                                if (activeStop != null) {
                                    MapsNavigator.openNavigation(context, activeStop)
                                }
                            },
                            onDelivered = {
                                activeStop?.let { stop ->
                                    updateStops { stops ->
                                        stops.updateStop(stop.id) {
                                            it.copy(status = StopStatus.Delivered, failureReason = "")
                                        }
                                    }
                                    statusChangedAtMillis = statusChangedAtMillis + (stop.id to System.currentTimeMillis())
                                    clearStopSelection()
                                }
                            },
                            onFailed = {
                                activeStop?.let { stop ->
                                    updateStops { stops ->
                                        stops.updateStop(stop.id) {
                                            it.copy(
                                                status = StopStatus.Failed,
                                                failureReason = "Incidencia",
                                            )
                                        }
                                    }
                                    statusChangedAtMillis = statusChangedAtMillis + (stop.id to System.currentTimeMillis())
                                    clearStopSelection()
                                }
                            },
                            onUndoStatus = {
                                activeStop?.let { stop ->
                                    updateStops { stops ->
                                        stops.updateStop(stop.id) {
                                            it.copy(status = StopStatus.Pending, failureReason = "")
                                        }
                                    }
                                    statusChangedAtMillis = statusChangedAtMillis - stop.id
                                    selectedStopId = stop.id
                                    editingStopId = null
                                }
                            },
                            onCloseResolvedStop = ::clearStopSelection,
                            onEditStop = { stopId -> editingStopId = stopId },
                            onCancelEdit = { editingStopId = null },
                            onDeleteStop = { stop ->
                                updateStops { stops -> stops.filterNot { it.id == stop.id } }
                                statusChangedAtMillis = statusChangedAtMillis - stop.id
                                if (selectedStopId == stop.id) {
                                    selectedStopId = null
                                }
                            },
                            onUpdateStop = { stopId, transform ->
                                updateStops { stops ->
                                    stops.map { stop ->
                                        if (stop.id == stopId) transform(stop) else stop
                                    }
                                }
                            },
                            onOptimize = {
                                val optimized = RouteOptimizer.optimize(currentRoute.stops, startPoint)
                                persist(currentRoute.copy(stops = optimized))
                            },
                            onAddStop = { onEditRoute(currentRoute.id) },
                            onStopSelected = { selectedStop -> selectStop(selectedStop.id) },
                            onNavigateToStart = {
                                MapsNavigator.openNavigationToPoint(context, startPoint)
                            },
                            onAcknowledgeRouteCompleted = {
                                showRouteCompleted = false
                                showRouteSummary = true
                                scope.launch { sheetState.bottomSheetState.expand() }
                            },
                            onCloseRouteCompleted = { showRouteCompleted = false },
                            onEditArrivalPoint = { onEditRoute(currentRoute.id) },
                            onCopyStopsToNewRoute = {
                                val newRoute = repository.duplicateRouteWithStops(currentRoute)
                                onEditRoute(newRoute.id)
                            },
                            onCreateNewRoute = onCreateRoute,
                        )
                    },
                ) { innerPadding ->
                    RouteMapView(
                        currentLocation = null,
                        startLocation = currentRoute.startLocation,
                        stops = orderedStops,
                        activeStopId = activeStop?.id,
                        drawRoutePath = orderedStops.size > 1,
                        onStopClick = ::selectStop,
                        focusPoint = if (showRouteCompleted && allStopsResolved && selectedStopId == null) {
                            startPoint
                        } else {
                            null
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRoutesState(onCreateRoute: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Aún no tienes rutas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Crea tu primera ruta para empezar a organizar paradas.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateRoute) {
            Text("Agregar nueva ruta")
        }
    }
}

@Composable
private fun MissingRouteState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ruta no encontrada", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun RouteWorkSheet(
    route: Route,
    activeStop: DeliveryStop?,
    viewingResolvedStop: Boolean,
    showRouteCompleted: Boolean,
    showRouteSummary: Boolean,
    routeCompletedAtMillis: Long?,
    routeDistanceLabel: String,
    arrivalAddress: String,
    arrivalPostalCode: String?,
    isStopFocused: Boolean,
    activeStopSubtitle: String?,
    editingStopId: String?,
    activeStatusChangedAtMillis: Long?,
    onNavigate: () -> Unit,
    onDelivered: () -> Unit,
    onFailed: () -> Unit,
    onUndoStatus: () -> Unit,
    onCloseResolvedStop: () -> Unit,
    onEditStop: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteStop: (DeliveryStop) -> Unit,
    onUpdateStop: (String, (DeliveryStop) -> DeliveryStop) -> Unit,
    onOptimize: () -> Unit,
    onAddStop: () -> Unit,
    onStopSelected: (DeliveryStop) -> Unit,
    onNavigateToStart: () -> Unit,
    onAcknowledgeRouteCompleted: () -> Unit,
    onCloseRouteCompleted: () -> Unit,
    onEditArrivalPoint: () -> Unit,
    onCopyStopsToNewRoute: () -> Unit,
    onCreateNewRoute: () -> Unit,
) {
    val delivered = route.stops.count { it.status == StopStatus.Delivered }
    val failed = route.stops.count { it.status == StopStatus.Failed }
    val pending = route.stops.count { it.status == StopStatus.Pending }
    val orderedStops = route.stops.sortedBy { it.orderIndex }
    val totalStops = orderedStops.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showRouteSummary && routeCompletedAtMillis != null) {
            RouteCompletionSummaryView(
                stops = orderedStops,
                arrivalAddress = arrivalAddress,
                finishedAtMillis = routeCompletedAtMillis,
                distanceLabel = routeDistanceLabel,
                onCopyStopsToNewRoute = onCopyStopsToNewRoute,
                onCreateNewRoute = onCreateNewRoute,
            )
        } else if (showRouteCompleted && routeCompletedAtMillis != null) {
            RouteCompletedView(
                arrivalAddress = arrivalAddress,
                arrivalTimeMillis = routeCompletedAtMillis,
                postalCode = arrivalPostalCode,
                onNavigate = onNavigateToStart,
                onAcknowledge = onAcknowledgeRouteCompleted,
                onEditArrivalPoint = onEditArrivalPoint,
                onClose = onCloseRouteCompleted,
            )
        } else if (viewingResolvedStop && activeStop != null) {
            if (editingStopId == activeStop.id) {
                StopDetailEditor(
                    stop = activeStop,
                    onNotesChange = { notes ->
                        onUpdateStop(activeStop.id) { it.copy(notes = notes) }
                    },
                    onTypeChange = { type ->
                        onUpdateStop(activeStop.id) { it.copy(stopType = type) }
                    },
                    onPackageCountChange = { count ->
                        onUpdateStop(activeStop.id) { it.copy(packageCount = count.coerceAtLeast(1)) }
                    },
                    onOrderPreferenceChange = { },
                    onDelete = { onDeleteStop(activeStop) },
                    onBack = onCancelEdit,
                )
            } else {
                CompletedStopDetailView(
                    stop = activeStop,
                    stopPosition = activeStop.orderIndex + 1,
                    totalStops = totalStops,
                    statusChangedAtMillis = activeStatusChangedAtMillis,
                    onClose = onCloseResolvedStop,
                    onUndo = onUndoStatus,
                    onNavigate = onNavigate,
                    onEdit = { onEditStop(activeStop.id) },
                    onDelete = { onDeleteStop(activeStop) },
                )
            }
        } else {
            if (!isStopFocused) {
                Text("Ruta de trabajo", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Pendientes: $pending · Entregadas: $delivered · Fallidas: $failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                activeStop != null && activeStop.status == StopStatus.Pending && editingStopId == activeStop.id -> {
                    StopDetailEditor(
                        stop = activeStop,
                        onNotesChange = { notes ->
                            onUpdateStop(activeStop.id) { it.copy(notes = notes) }
                        },
                        onTypeChange = { type ->
                            onUpdateStop(activeStop.id) { it.copy(stopType = type) }
                        },
                        onPackageCountChange = { count ->
                            onUpdateStop(activeStop.id) { it.copy(packageCount = count.coerceAtLeast(1)) }
                        },
                        onOrderPreferenceChange = { },
                        onDelete = { onDeleteStop(activeStop) },
                        onBack = onCancelEdit,
                    )
                }

                activeStop != null && activeStop.status == StopStatus.Pending -> {
                    PendingStopWorkView(
                        stop = activeStop,
                        stopPosition = activeStop.orderIndex + 1,
                        totalStops = totalStops,
                        subtitle = activeStopSubtitle,
                        onNavigate = onNavigate,
                        onDelivered = onDelivered,
                        onFailed = onFailed,
                        onEdit = { onEditStop(activeStop.id) },
                        onDelete = { onDeleteStop(activeStop) },
                        onClose = if (isStopFocused) onCloseResolvedStop else null,
                    )
                }

                activeStop == null -> {
                    RutsCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Agrega paradas para empezar.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOptimize,
                    modifier = Modifier.weight(1f),
                    enabled = route.stops.size > 1,
                ) {
                    Text("Optimizar")
                }
                OutlinedButton(onClick = onAddStop, modifier = Modifier.weight(1f)) {
                    Text("Agregar parada")
                }
            }
        }

        if (!showRouteCompleted && !showRouteSummary && !viewingResolvedStop && !isStopFocused && editingStopId == null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text("Paradas organizadas", style = MaterialTheme.typography.titleSmall)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(orderedStops, key = { it.id }) { stop ->
                    StopListItem(
                        stop = stop,
                        isActive = stop.id == activeStop?.id,
                        onClick = { onStopSelected(stop) },
                        onDelete = { onDeleteStop(stop) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StopListItem(
    stop: DeliveryStop,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    RutsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${stop.orderIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.address, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = buildString {
                        append(stop.status.label())
                        append(" · ")
                        append(if (stop.stopType == StopType.Delivery) "Entrega" else "Recogida")
                        append(" · ")
                        append("${stop.packageCount} paq.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (stop.failureReason.isNotBlank()) {
                    Text(
                        text = stop.failureReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar parada",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RutsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            content()
        }
    }
}

private fun StopStatus.label(): String {
    return when (this) {
        StopStatus.Pending -> "Pendiente"
        StopStatus.Delivered -> "Entregada"
        StopStatus.Failed -> "Fallida"
    }
}

private fun List<DeliveryStop>.updateStop(
    stopId: String,
    transform: (DeliveryStop) -> DeliveryStop,
): List<DeliveryStop> = map { stop ->
    if (stop.id == stopId) transform(stop) else stop
}

private fun List<DeliveryStop>.reindexStops(): List<DeliveryStop> {
    return sortedBy { it.orderIndex }.mapIndexed { index, stop ->
        stop.copy(orderIndex = index)
    }
}
