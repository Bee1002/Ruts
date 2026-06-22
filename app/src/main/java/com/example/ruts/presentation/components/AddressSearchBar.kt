package com.example.ruts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ruts.geocoding.AddressResult
import com.example.ruts.ui.theme.AccentBlue
import com.example.ruts.ui.theme.RutsUi
import com.example.ruts.ui.theme.SurfaceCard
import com.example.ruts.ui.theme.TextSecondary
import com.example.ruts.ui.theme.White

private val SearchShape = RoundedCornerShape(28.dp)

@Composable
fun CompactAddressSearchBar(
    onOpenExpanded: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Escribe para añadir más",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SearchShape)
            .background(SurfaceCard)
            .clickable(onClick = onOpenExpanded)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = AccentBlue)
        Text(
            text = placeholder,
            modifier = Modifier.weight(1f),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onVoiceClick) {
            Icon(Icons.Default.Mic, contentDescription = "Buscar por voz", tint = AccentBlue)
        }
    }
}

@Composable
fun ExpandedAddressSearchBar(
    query: String,
    results: List<AddressResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onClose: () -> Unit,
    onResultSelected: (AddressResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe para añadir más", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentBlue) },
                trailingIcon = {
                    IconButton(onClick = onVoiceClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Buscar por voz", tint = AccentBlue)
                    }
                },
                singleLine = true,
                shape = SearchShape,
                colors = RutsUi.textFieldColors,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", tint = TextSecondary)
            }
        }

        if (isSearching) {
            Text(
                "Buscando...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        if (query.length >= 3 && results.isEmpty() && !isSearching) {
            Text(
                "No se encontraron direcciones",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            results.forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .clickable { onResultSelected(result) }
                        .padding(14.dp),
                ) {
                    Text(
                        text = result.address,
                        color = White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun StopSelectorRow(
    stops: List<com.example.ruts.domain.DeliveryStop>,
    selectedStopId: String?,
    onStopSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stops.size <= 1) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stops.sortedBy { it.orderIndex }.forEach { stop ->
            val selected = stop.id == selectedStopId
            Text(
                text = "A${stop.orderIndex + 1}",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) White else SurfaceCard)
                    .clickable { onStopSelected(stop.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (selected) MaterialTheme.colorScheme.background else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
