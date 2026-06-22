package com.example.ruts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ruts.domain.Route
import com.example.ruts.domain.displayLabel
import com.example.ruts.domain.displaySubtitle
import com.example.ruts.domain.isRouteSuffixName
import com.example.ruts.domain.isSameDay
import com.example.ruts.ui.theme.RutsUi

@Composable
fun RoutesDrawerContent(
    routes: List<Route>,
    selectedRouteId: String?,
    onRouteSelected: (String) -> Unit,
    onCreateRoute: () -> Unit,
    onRenameRoute: (String, String) -> Unit,
    onDeleteRoute: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var routeToRename by remember { mutableStateOf<Route?>(null) }
    var routeToDelete by remember { mutableStateOf<Route?>(null) }
    var renameValue by remember { mutableStateOf("") }

        routeToRename?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToRename = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Renombrar ruta") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la ruta") },
                    singleLine = true,
                    colors = RutsUi.textFieldColors,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameValue.trim()
                        onRenameRoute(route.id, trimmed)
                        routeToRename = null
                    },
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToRename = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Eliminar ruta") },
            text = { Text("¿Eliminar \"${route.displayLabel()}\" y todas sus paradas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRoute(route.id)
                        routeToDelete = null
                    },
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClose)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver al mapa",
                tint = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Rutas",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(routes, key = { it.id }) { route ->
                val isSelected = route.id == selectedRouteId
                val stopCount = route.stops.size

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onRouteSelected(route.id) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = route.displayLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        route.displaySubtitle()?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "$stopCount paradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isSameDay(route.createdAtMillis, System.currentTimeMillis())) {
                            Text(
                                text = "Hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            renameValue = if (route.name.isNotBlank() && !isRouteSuffixName(route.name)) {
                                route.name
                            } else {
                                ""
                            }
                            routeToRename = route
                        },
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Renombrar",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { routeToDelete = route }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        OutlinedButton(
            onClick = onCreateRoute,
            modifier = Modifier.fillMaxWidth(),
            colors = RutsUi.outlinedButtonColors,
        ) {
            Text("Agregar nueva ruta")
        }
    }
}
