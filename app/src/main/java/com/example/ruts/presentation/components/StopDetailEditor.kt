package com.example.ruts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.StopOrderPreference
import com.example.ruts.domain.StopType
import com.example.ruts.ui.theme.AccentBlue
import com.example.ruts.ui.theme.AccentPurple
import com.example.ruts.ui.theme.stopAccentColor
import com.example.ruts.ui.theme.Border
import com.example.ruts.ui.theme.Error
import com.example.ruts.ui.theme.RutsUi
import com.example.ruts.ui.theme.SectionHeader
import com.example.ruts.ui.theme.SurfaceCard
import com.example.ruts.ui.theme.TextSecondary
import com.example.ruts.ui.theme.White

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(20.dp)

@Composable
fun StopDetailEditor(
    stop: DeliveryStop,
    onNotesChange: (String) -> Unit,
    onTypeChange: (StopType) -> Unit,
    onPackageCountChange: (Int) -> Unit,
    onServiceMinutesChange: (Int) -> Unit,
    onOrderPreferenceChange: (StopOrderPreference) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(horizontal = 0.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a paradas",
                        tint = AccentBlue,
                    )
                }
                Text(
                    text = "Volver a paradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        StopHeader(stop = stop)

        headerActions?.invoke()

        StopTagsRow()

        HorizontalDivider(color = Border)

        NotesRow(
            notes = stop.notes,
            onNotesChange = onNotesChange,
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TextSecondary) },
            label = "Tipo",
            content = {
                SegmentedControl(
                    options = listOf("Entrega", "Recogida"),
                    selectedIndex = if (stop.stopType == StopType.Delivery) 0 else 1,
                    selectedColor = stopAccentColor(stop.stopType),
                    onSelected = { index ->
                        onTypeChange(if (index == 0) StopType.Delivery else StopType.Pickup)
                    },
                )
            },
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            label = "Localizador",
            content = {
                Text(
                    text = "Sin definir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextSecondary) },
            label = "Paquetes",
            content = {
                PackageStepper(
                    count = stop.packageCount,
                    onDecrease = { onPackageCountChange((stop.packageCount - 1).coerceAtLeast(1)) },
                    onIncrease = { onPackageCountChange(stop.packageCount + 1) },
                )
            },
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = TextSecondary) },
            label = "Orden",
            content = {
                SegmentedControl(
                    options = listOf("Primera", "Autom...", "Última"),
                    selectedIndex = when (stop.orderPreference) {
                        StopOrderPreference.First -> 0
                        StopOrderPreference.Automatic -> 1
                        StopOrderPreference.Last -> 2
                    },
                    onSelected = { index ->
                        val preference = when (index) {
                            0 -> StopOrderPreference.First
                            2 -> StopOrderPreference.Last
                            else -> StopOrderPreference.Automatic
                        }
                        onOrderPreferenceChange(preference)
                    },
                )
            },
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary) },
            label = "Hora de llegada",
            content = {
                Text(
                    text = "Cualquier hora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
        )

        HorizontalDivider(color = Border)

        SettingRow(
            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = TextSecondary) },
            label = "Tiempo esti...",
            content = {
                PackageStepper(
                    count = stop.serviceMinutes,
                    valueLabel = "${stop.serviceMinutes} min",
                    onDecrease = { onServiceMinutesChange((stop.serviceMinutes - 1).coerceAtLeast(1)) },
                    onIncrease = { onServiceMinutesChange((stop.serviceMinutes + 1).coerceAtMost(60)) },
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(SectionHeader),
        ) {
            ActionMenuRow(
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Error) },
                label = "Eliminar parada",
                tint = Error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun StopHeader(stop: DeliveryStop) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "A${stop.orderIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Añadida",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            text = stop.streetTitle(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = White,
        )
        Text(
            text = stop.addressSubtitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun StopTagsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(label = "Default", dotColor = AccentBlue, selected = true)
        TagChip(label = "Afternoon Pickup", dotColor = AccentPurple, selected = false)
        listOf(
            AccentBlue,
            AccentPurple,
            androidx.compose.ui.graphics.Color(0xFF64D2FF),
            androidx.compose.ui.graphics.Color(0xFF30D158),
            androidx.compose.ui.graphics.Color(0xFFFF9F0A),
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.dp, Border, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun TagChip(
    label: String,
    dotColor: androidx.compose.ui.graphics.Color,
    selected: Boolean,
) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .border(
                width = 1.dp,
                color = if (selected) AccentBlue else Border,
                shape = ChipShape,
            )
            .background(if (selected) AccentBlue.copy(alpha = 0.12f) else SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) White else TextSecondary,
        )
    }
}

@Composable
private fun NotesRow(
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Añadir notas", color = TextSecondary) },
            minLines = 1,
            maxLines = 3,
            colors = RutsUi.textFieldColors,
            shape = FieldShape,
        )
        Icon(
            Icons.Default.Key,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
        )
        Icon(
            Icons.Default.AddAPhoto,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = White,
        )
        content()
    }
}

@Composable
private fun ActionMenuRow(
    icon: @Composable () -> Unit,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
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
        icon()
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = tint,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = tint.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    selectedColor: androidx.compose.ui.graphics.Color = AccentBlue,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .background(SurfaceCard),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) selectedColor else SurfaceCard)
                    .clickable { onSelected(index) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) White else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun PackageStepper(
    count: Int,
    valueLabel: String = count.toString(),
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .background(SurfaceCard),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(label = "−", onClick = onDecrease)
        Text(
            text = valueLabel,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = White,
        )
        StepperButton(label = "+", onClick = onIncrease)
    }
}

@Composable
private fun StepperButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = White,
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
