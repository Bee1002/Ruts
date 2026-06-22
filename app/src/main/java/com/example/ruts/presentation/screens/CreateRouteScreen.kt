package com.example.ruts.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ruts.domain.Route
import com.example.ruts.domain.formatRelativeDateOptionLabel
import com.example.ruts.domain.resolveStoredRouteName
import com.example.ruts.domain.routesOnSameDay
import com.example.ruts.domain.suggestRouteNameForCreation
import com.example.ruts.domain.todayMillis
import com.example.ruts.domain.tomorrowMillis
import com.example.ruts.ui.theme.AccentBlue
import com.example.ruts.ui.theme.Border
import com.example.ruts.ui.theme.RutsUi
import com.example.ruts.ui.theme.TextSecondary
import com.example.ruts.ui.theme.White

private enum class RouteDateChoice {
    Today,
    Tomorrow,
    Custom,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRouteScreen(
    routes: List<Route>,
    onBack: () -> Unit,
    onConfirm: (createdAtMillis: Long, routeName: String) -> Unit,
) {
    val referenceMillis = todayMillis()
    val todayMillis = referenceMillis
    val tomorrowMillisValue = tomorrowMillis(referenceMillis)

    var dateChoice by remember { mutableStateOf(RouteDateChoice.Today) }
    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var copyPastStops by remember { mutableStateOf(false) }

    val selectedDateMillis = when (dateChoice) {
        RouteDateChoice.Today -> todayMillis
        RouteDateChoice.Tomorrow -> tomorrowMillisValue
        RouteDateChoice.Custom -> customDateMillis ?: todayMillis
    }

    val existingSameDay = routesOnSameDay(routes, selectedDateMillis)
    val suggestedName = suggestRouteNameForCreation(selectedDateMillis, existingSameDay)

    var routeName by remember { mutableStateOf(suggestedName) }
    var nameManuallyEdited by remember { mutableStateOf(false) }

    LaunchedEffect(suggestedName) {
        if (!nameManuallyEdited) {
            routeName = suggestedName
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            customDateMillis = millis
                            dateChoice = RouteDateChoice.Custom
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    val storedName = resolveStoredRouteName(
                        routeName,
                        selectedDateMillis,
                        existingSameDay,
                    )
                    onConfirm(selectedDateMillis, storedName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                colors = RutsUi.primaryButtonColors,
            ) {
                Text("Confirmar")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Crea una ruta",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nombre de la ruta (opcional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { value ->
                        routeName = value
                        nameManuallyEdited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = RutsUi.textFieldColors,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Selecciona la fecha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                DateChoiceRow(
                    label = formatRelativeDateOptionLabel(todayMillis, referenceMillis),
                    selected = dateChoice == RouteDateChoice.Today,
                    onClick = { dateChoice = RouteDateChoice.Today },
                )
                HorizontalDivider(color = Border)
                DateChoiceRow(
                    label = formatRelativeDateOptionLabel(tomorrowMillisValue, referenceMillis),
                    selected = dateChoice == RouteDateChoice.Tomorrow,
                    onClick = { dateChoice = RouteDateChoice.Tomorrow },
                )
                HorizontalDivider(color = Border)
                CustomDateChoiceRow(
                    label = if (dateChoice == RouteDateChoice.Custom && customDateMillis != null) {
                        formatRelativeDateOptionLabel(customDateMillis!!, referenceMillis)
                    } else {
                        "Elegir una fecha"
                    },
                    selected = dateChoice == RouteDateChoice.Custom,
                    onClick = { showDatePicker = true },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Opciones de inicio rápido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { copyPastStops = !copyPastStops }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    Text(
                        text = "Elige paradas pasadas para trasladar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = copyPastStops,
                        onCheckedChange = { copyPastStops = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentBlue,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = White,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = if (selected) AccentBlue else TextSecondary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentBlue,
                unselectedColor = TextSecondary,
            ),
        )
    }
}

@Composable
private fun CustomDateChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = if (selected) AccentBlue else TextSecondary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (!selected) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
            )
        } else {
            RadioButton(
                selected = true,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentBlue,
                    unselectedColor = TextSecondary,
                ),
            )
        }
    }
}
