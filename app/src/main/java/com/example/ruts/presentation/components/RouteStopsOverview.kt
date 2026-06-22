package com.example.ruts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.Route
import com.example.ruts.domain.formatTime
import com.example.ruts.domain.formatWeekday
import com.example.ruts.ui.theme.AccentBlue
import com.example.ruts.ui.theme.stopAccentColor
import com.example.ruts.ui.theme.Border
import com.example.ruts.ui.theme.SectionHeader
import com.example.ruts.ui.theme.SurfaceCard
import com.example.ruts.ui.theme.TextSecondary
import com.example.ruts.ui.theme.White

private val IconBadgeShape = RoundedCornerShape(8.dp)
private val StopBadgeShape = RoundedCornerShape(6.dp)

@Composable
fun RouteStopsOverview(
    route: Route,
    startAddress: String?,
    onStopClick: (DeliveryStop) -> Unit,
    onOptimize: () -> Unit,
    modifier: Modifier = Modifier,
    isSheetExpanded: Boolean = true,
    onSearchClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    val orderedStops = route.stops.sortedBy { it.orderIndex }
    val startPoint = route.startLocation
    val resolvedStartAddress = startAddress ?: "Ubicación de inicio"
    val stopCountLabel = if (orderedStops.size == 1) "1 parada" else "${orderedStops.size} paradas"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (isSheetExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stopCountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = formatWeekday(route.createdAtMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = White,
                )
            }
        } else {
            CollapsedOverviewHeader(
                stopCountLabel = stopCountLabel,
                onSearchClick = onSearchClick,
                onMenuClick = onMenuClick,
            )
        }

        SectionHeaderBar(title = "Configuración de ruta")

        RouteConfigRow(
            leading = {
                Text(
                    text = formatTime(route.createdAtMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
            title = resolvedStartAddress,
            subtitle = null,
            trailing = {
                IconBadge(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = AccentBlue) },
                    background = AccentBlue.copy(alpha = 0.15f),
                )
            },
        )

        HorizontalDivider(color = Border)

        RouteConfigRow(
            leading = {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TextSecondary),
                )
            },
            title = "Viaje de ida y vuelta",
            subtitle = if (startPoint != null) {
                "Volver a ${resolvedStartAddress.substringBefore(",").ifBlank { resolvedStartAddress }}"
            } else {
                "Volver al punto de partida"
            },
            trailing = {
                IconBadge(
                    icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = AccentBlue) },
                    background = AccentBlue.copy(alpha = 0.15f),
                )
            },
        )

        HorizontalDivider(color = Border)

        RouteConfigRow(
            leading = {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TextSecondary),
                )
            },
            title = "Sin descanso",
            subtitle = "Pulsa para planificar un descanso",
            trailing = {
                IconBadge(
                    icon = { Icon(Icons.Default.LocalCafe, contentDescription = null, tint = TextSecondary) },
                    background = SurfaceCard,
                )
            },
        )

        if (isSheetExpanded) {
            SectionHeaderBar(title = "Paradas")

            if (orderedStops.isEmpty()) {
                Text(
                    text = "Aún no hay paradas en esta ruta.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                orderedStops.forEachIndexed { index, stop ->
                    if (index > 0) {
                        HorizontalDivider(color = Border)
                    }
                    StopOverviewRow(
                        stop = stop,
                        onClick = { onStopClick(stop) },
                    )
                }
            }
        }

        Button(
            onClick = onOptimize,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp),
            enabled = orderedStops.size > 1,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                contentColor = White,
                disabledContainerColor = SurfaceCard,
                disabledContentColor = TextSecondary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
            )
            Text(
                text = "Optimizar la ruta",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CollapsedOverviewHeader(
    stopCountLabel: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stopCountLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = White,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Buscar parada", tint = White)
        }
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = White)
        }
    }
}

@Composable
private fun SectionHeaderBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(SectionHeader)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
    }
}

@Composable
private fun RouteConfigRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun IconBadge(
    icon: @Composable () -> Unit,
    background: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(IconBadgeShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun StopOverviewRow(
    stop: DeliveryStop,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = String.format("%02d", stop.orderIndex + 1),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(end = 4.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stop.streetTitle(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stop.addressSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(StopBadgeShape)
                .background(stopAccentColor(stop.stopType).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "A${stop.orderIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = stopAccentColor(stop.stopType),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
        )
    }
}

private fun DeliveryStop.streetTitle(): String {
    return address.substringBefore(",").trim().ifBlank { customerName }
}

private fun DeliveryStop.addressSubtitle(): String {
    val subtitle = address.substringAfter(",", "").trim()
    return subtitle.ifBlank { address }
}
