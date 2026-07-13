package com.example.shikagera1.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shikagera1.domain.DayRecord
import com.example.shikagera1.domain.TimeParser
import com.example.shikagera1.ui.components.TimeInputField
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = state.todayLabel,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (state.showResetWarning) {
                item {
                    WarningBanner(
                        message = state.resetWarningMessage,
                        onDismiss = viewModel::dismissResetWarning,
                    )
                }
            }

            item {
                BalanceCard(
                    balanceMinutes = state.weekBalanceMinutes,
                    onResetClick = viewModel::requestResetBalance,
                )
            }

            if (!state.isDayClosed) {
                item {
                    ActionButtons(
                        onClockIn = viewModel::clockIn,
                        onClockOut = viewModel::clockOut,
                    )
                }
            } else {
                item {
                    Text(
                        text = "День закрыт — измените время вручную",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ManualTimeInputs(
                    arrivalDigits = state.manualArrivalDigits,
                    departureDigits = state.manualDepartureDigits,
                    onArrivalChanged = viewModel::onManualArrivalChanged,
                    onDepartureChanged = viewModel::onManualDepartureChanged,
                    onApplyArrival = viewModel::applyManualArrival,
                    onApplyDeparture = viewModel::applyManualDeparture,
                    error = state.inputError,
                )
            }

            state.predictedDeparture?.let { predicted ->
                item {
                    Text(
                        text = "Можно уйти в $predicted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.todayRecord?.let { today ->
                item {
                    TodaySummary(today)
                }
            }

            item {
                Text(
                    text = "Дни недели",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(state.currentWeekDays) { day ->
                WeekDayRow(day = day, onClick = { viewModel.openDayEditor(day.date) })
            }

            item {
                OutlinedButton(
                    onClick = viewModel::togglePreviousWeek,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.showPreviousWeek) "Скрыть прошлую неделю"
                        else "Прошлая неделя",
                    )
                }
            }

            if (state.showPreviousWeek) {
                items(state.previousWeekDays) { day ->
                    WeekDayRow(day = day, onClick = { viewModel.openDayEditor(day.date) })
                }
            }

            item {
                ExcludeSection(
                    isExpanded = state.showExcludeSection,
                    excludeInput = state.excludeMinutesInput,
                    currentExcluded = state.todayRecord?.excludedMinutes ?: 0,
                    onToggle = viewModel::toggleExcludeSection,
                    onInputChanged = viewModel::onExcludeMinutesChanged,
                    onApply = viewModel::applyExcludeMinutes,
                )
            }
        }
    }

    if (state.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetConfirmDialog,
            title = { Text("Сбросить баланс?") },
            text = { Text("Баланс недели будет обнулён. Записи дней сохранятся.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmResetBalance) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResetConfirmDialog) {
                    Text("Отмена")
                }
            },
        )
    }

    state.editingDay?.let { day ->
        DayEditDialog(
            record = day,
            onDismiss = viewModel::dismissDayEditor,
            onSave = viewModel::saveEditedDay,
        )
    }
}

@Composable
private fun WarningBanner(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        }
    }
}

@Composable
private fun BalanceCard(balanceMinutes: Int, onResetClick: () -> Unit) {
    val color = if (balanceMinutes >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Баланс недели", style = MaterialTheme.typography.labelLarge)
            Text(
                text = TimeParser.formatBalance(balanceMinutes),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            TextButton(onClick = onResetClick) {
                Text("Сбросить баланс")
            }
        }
    }
}

@Composable
private fun ActionButtons(onClockIn: () -> Unit, onClockOut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onClockIn,
            modifier = Modifier.weight(1f),
        ) {
            Text("Я пришёл")
        }
        OutlinedButton(
            onClick = onClockOut,
            modifier = Modifier.weight(1f),
        ) {
            Text("Я ушёл")
        }
    }
}

@Composable
private fun ManualTimeInputs(
    arrivalDigits: String,
    departureDigits: String,
    onArrivalChanged: (String) -> Unit,
    onDepartureChanged: (String) -> Unit,
    onApplyArrival: () -> Unit,
    onApplyDeparture: () -> Unit,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Ручной ввод времени", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeInputField(
                digits = arrivalDigits,
                onDigitsChanged = onArrivalChanged,
                label = "Приход",
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onApplyArrival) { Text("OK") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeInputField(
                digits = departureDigits,
                onDigitsChanged = onDepartureChanged,
                label = "Уход",
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onApplyDeparture) { Text("OK") }
        }
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ExcludeSection(
    isExpanded: Boolean,
    excludeInput: String,
    currentExcluded: Int,
    onToggle: () -> Unit,
    onInputChanged: (String) -> Unit,
    onApply: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Не учитывать")
        }

        if (currentExcluded > 0) {
            Text(
                text = "Не учтено сегодня: ${currentExcluded} мин",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isExpanded) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = excludeInput,
                    onValueChange = onInputChanged,
                    label = { Text("Минуты отсутствия") },
                    placeholder = { Text("45") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Button(onClick = onApply) { Text("OK") }
            }
            Text(
                text = "Это время не войдёт в рабочий день",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodaySummary(today: DayRecord) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val arrival = today.arrivalMinutes?.let(TimeParser::formatMinutes) ?: "—"
    val departure = today.departureMinutes?.let(TimeParser::formatMinutes) ?: "—"
    val excluded = if (today.excludedMinutes > 0) ", не учтено ${today.excludedMinutes} мин" else ""
    Text(
        text = "Сегодня (${today.date.format(formatter)}): $arrival → $departure$excluded",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun WeekDayRow(day: WeekDayItem, onClick: () -> Unit) {
    val balanceColor = if (day.balanceMinutes >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${day.dayLabel}${if (day.isToday) " • сегодня" else ""}",
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = TimeParser.formatBalance(day.balanceMinutes),
                    color = balanceColor,
                )
            }
            if (day.arrival != null || day.departure != null) {
                Text(
                    text = "${day.arrival ?: "—"} → ${day.departure ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (day.excludedMinutes > 0) {
                Text(
                    text = "Не учтено: ${day.excludedMinutes} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (day.note.isNotBlank()) {
                Text(
                    text = day.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayEditDialog(
    record: DayRecord,
    onDismiss: () -> Unit,
    onSave: (DayRecord) -> Unit,
) {
    key(record.date) {
        DayEditDialogContent(record = record, onDismiss = onDismiss, onSave = onSave)
    }
}

@Composable
private fun DayEditDialogContent(
    record: DayRecord,
    onDismiss: () -> Unit,
    onSave: (DayRecord) -> Unit,
) {
    var arrivalDigits by remember(record.date) {
        mutableStateOf(record.arrivalMinutes?.let(::minutesToDigits).orEmpty())
    }
    var departureDigits by remember(record.date) {
        mutableStateOf(record.departureMinutes?.let(::minutesToDigits).orEmpty())
    }
    var note by remember(record.date) { mutableStateOf(record.note) }
    var excluded by remember(record.date) {
        mutableStateOf(record.excludedMinutes.toString().takeIf { record.excludedMinutes > 0 }.orEmpty())
    }
    var error by remember(record.date) { mutableStateOf<String?>(null) }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.date.format(formatter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeInputField(
                    digits = arrivalDigits,
                    onDigitsChanged = { arrivalDigits = it },
                    label = "Приход",
                )
                TimeInputField(
                    digits = departureDigits,
                    onDigitsChanged = { departureDigits = it },
                    label = "Уход",
                )
                OutlinedTextField(
                    value = excluded,
                    onValueChange = { excluded = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Не учитывать (мин)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Пометка") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val arrivalMinutes = arrivalDigits.takeIf { it.isNotBlank() }?.let {
                        if (TimeParser.isValidTimeDigits(it)) TimeParser.parseCompact(it) else null
                    }
                    val departureMinutes = departureDigits.takeIf { it.isNotBlank() }?.let {
                        if (TimeParser.isValidTimeDigits(it)) TimeParser.parseCompact(it) else null
                    }
                    if ((arrivalDigits.isNotBlank() && arrivalMinutes == null) ||
                        (departureDigits.isNotBlank() && departureMinutes == null)
                    ) {
                        error = "Введите время: 4 цифры (ЧЧММ) или 3 цифры для утра (810 → 08:10)"
                        return@TextButton
                    }
                    onSave(
                        record.copy(
                            arrivalMinutes = arrivalMinutes,
                            departureMinutes = departureMinutes,
                            note = note.trim(),
                            excludedMinutes = excluded.toIntOrNull() ?: 0,
                        ),
                    )
                },
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun minutesToDigits(minutes: Int): String {
    val hours = (minutes / 60).coerceIn(0, 23)
    val mins = (minutes % 60).coerceIn(0, 59)
    return "%02d%02d".format(hours, mins)
}