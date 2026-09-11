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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.shikagera1.domain.BalanceCalculator
import com.example.shikagera1.domain.DayRecord
import com.example.shikagera1.domain.TimeFieldInput
import com.example.shikagera1.domain.TimeParser
import com.example.shikagera1.domain.WorkConstants
import com.example.shikagera1.domain.WorkDayPhases
import com.example.shikagera1.ui.components.TimeInputField
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

private val PositiveGreen = Color(0xFF4CAF50)
private val NegativeRed = Color(0xFFE57373)

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
                    balanceMinutes = state.periodBalanceMinutes,
                    onResetClick = viewModel::requestResetBalance,
                )
            }

            item {
                when {
                    state.isTodayCredited -> CreditedNotice(onUncredit = viewModel::uncreditToday)
                    state.isDayFinished -> Text(
                        text = "День закрыт — измените время вручную",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> ActionButtons(
                        onClockIn = viewModel::clockIn,
                        onClockOut = viewModel::clockOut,
                        onCredit = viewModel::creditToday,
                    )
                }
            }

            if (!state.isTodayCredited) {
                item {
                    ManualTimeInputs(
                        arrival = state.manualArrival,
                        departure = state.manualDeparture,
                        onArrivalChanged = viewModel::onManualArrivalChanged,
                        onDepartureChanged = viewModel::onManualDepartureChanged,
                        onApplyArrival = viewModel::applyManualArrival,
                        onApplyDeparture = viewModel::applyManualDeparture,
                        error = state.inputError,
                    )
                }
            }

            state.predictedDeparture?.let { predicted ->
                item {
                    Text(
                        text = "Можно уйти в $predicted",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.activeArrivalMinutes?.let { arrivalMinutes ->
                item {
                    LiveWorkTimers(
                        arrivalMinutes = arrivalMinutes,
                        excludedMinutes = state.todayExcludedMinutes,
                        predictedDepartureMinutes = state.predictedDepartureMinutes,
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
            text = { Text("Баланс периода будет обнулён. Записи дней сохранятся.") },
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
    val color = if (balanceMinutes >= 0) PositiveGreen else NegativeRed
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
            Text(text = "Баланс периода", style = MaterialTheme.typography.labelLarge)
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
private fun ActionButtons(onClockIn: () -> Unit, onClockOut: () -> Unit, onCredit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        TextButton(
            onClick = onCredit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Зачёт — засчитать как полный рабочий день")
        }
    }
}

@Composable
private fun CreditedNotice(onUncredit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Зачёт: день засчитан как полный рабочий",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onUncredit) {
                Text("Отменить зачёт")
            }
        }
    }
}

@Composable
private fun ManualTimeInputs(
    arrival: TimeFieldInput,
    departure: TimeFieldInput,
    onArrivalChanged: (TimeFieldInput) -> Unit,
    onDepartureChanged: (TimeFieldInput) -> Unit,
    onApplyArrival: () -> Unit,
    onApplyDeparture: () -> Unit,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Ручной ввод времени", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TimeInputField(
                value = arrival,
                onValueChange = onArrivalChanged,
                label = "Приход",
                onDone = onApplyArrival,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onApplyArrival) { Text("OK") }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TimeInputField(
                value = departure,
                onValueChange = onDepartureChanged,
                label = "Уход",
                onDone = onApplyDeparture,
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
                    placeholder = { Text("30") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Button(onClick = onApply) { Text("OK") }
            }
            Text(
                text = "Это время не войдёт в рабочий день (обед ${WorkConstants.BREAK_MINUTES} мин вычитается сам)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Живой таймер дня по фазам: 1-я половина → перерыв → 2-я половина.
 * После расчётного времени ухода счётчик «До ухода» сменяется
 * на растущую «Переработку».
 */
@Composable
private fun LiveWorkTimers(
    arrivalMinutes: Int,
    excludedMinutes: Int,
    predictedDepartureMinutes: Int?,
) {
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(arrivalMinutes, excludedMinutes, predictedDepartureMinutes) {
        while (true) {
            nowEpochMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val zone = ZoneId.systemDefault()
    val todayStart = LocalDate.now(zone).atStartOfDay(zone)
    val now = java.time.Instant.ofEpochMilli(nowEpochMs)
    val arrivalInstant = todayStart.plusMinutes(arrivalMinutes.toLong()).toInstant()
    val presenceSeconds = (ChronoUnit.SECONDS.between(arrivalInstant, now) - excludedMinutes * 60L)
        .coerceAtLeast(0)

    val phase = WorkDayPhases.phaseSeconds(presenceSeconds)
    val workedSeconds = WorkDayPhases.workedSeconds(presenceSeconds)
    val phaseLeftSeconds = WorkDayPhases.secondsLeftInPhase(presenceSeconds)

    // Фиксированный день: 4ч + 45м + 4ч от прихода, баланс прошлых дней не учитывается.
    val normMinutes = BalanceCalculator.dailyNormMinutes(LocalDate.now(zone))
    val dayEndPresenceMinutes = WorkDayPhases.presenceForWorkMinutes(normMinutes)
    val dayEndMinutes = arrivalMinutes + dayEndPresenceMinutes + excludedMinutes
    val dayEndPresenceSeconds = dayEndPresenceMinutes * 60L
    val untilDayEndSeconds = dayEndPresenceSeconds - presenceSeconds

    // Уход с учётом баланса: раньше конца дня при плюсе, позже при долге.
    val untilLeaveSeconds = predictedDepartureMinutes?.let { leaveMinutes ->
        val leaveInstant = todayStart.plusMinutes(leaveMinutes.toLong()).toInstant()
        ChronoUnit.SECONDS.between(now, leaveInstant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimerRow(
                label = "Отработано",
                value = TimeParser.formatDurationSeconds(workedSeconds),
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
            when (phase) {
                WorkDayPhases.Phase.FIRST_HALF -> TimerRow(
                    label = "1-я половина · до перерыва",
                    value = TimeParser.formatDurationSeconds(phaseLeftSeconds),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WorkDayPhases.Phase.BREAK -> TimerRow(
                    label = "Перерыв · осталось",
                    value = TimeParser.formatDurationSeconds(phaseLeftSeconds),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                )
                WorkDayPhases.Phase.SECOND_HALF -> Text(
                    text = "2-я половина дня",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (untilDayEndSeconds > 0) {
                TimerRow(
                    label = "До конца дня",
                    value = TimeParser.formatDurationSeconds(untilDayEndSeconds),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                TimerRow(
                    label = "Переработка сегодня",
                    value = "+" + TimeParser.formatDurationSeconds(-untilDayEndSeconds),
                    valueColor = PositiveGreen,
                )
            }
            if (untilLeaveSeconds != null && predictedDepartureMinutes != dayEndMinutes) {
                if (untilLeaveSeconds > 0) {
                    TimerRow(
                        label = "До ухода · с балансом",
                        value = TimeParser.formatDurationSeconds(untilLeaveSeconds),
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Баланс закрыт — можно уходить",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Composable
private fun TodaySummary(today: DayRecord) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val arrival = today.arrivalMinutes?.let(TimeParser::formatMinutes) ?: "—"
    val departure = today.departureMinutes?.let(TimeParser::formatMinutes) ?: "—"
    val excluded = if (today.excludedMinutes > 0) ", не учтено ${today.excludedMinutes} мин" else ""
    val credited = if (today.isCredited) ", зачёт" else ""
    Text(
        text = "Сегодня (${today.date.format(formatter)}): $arrival → $departure$excluded$credited",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun WeekDayRow(day: WeekDayItem, onClick: () -> Unit) {
    val balanceColor = if (day.balanceMinutes >= 0) PositiveGreen else NegativeRed
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
                    text = if (day.isCredited) "зачёт" else TimeParser.formatBalance(day.balanceMinutes),
                    color = if (day.isCredited) MaterialTheme.colorScheme.secondary else balanceColor,
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
    var arrival by remember(record.date) {
        mutableStateOf(record.arrivalMinutes?.let(TimeFieldInput::fromMinutesOfDay) ?: TimeFieldInput.EMPTY)
    }
    var departure by remember(record.date) {
        mutableStateOf(record.departureMinutes?.let(TimeFieldInput::fromMinutesOfDay) ?: TimeFieldInput.EMPTY)
    }
    var note by remember(record.date) { mutableStateOf(record.note) }
    var excluded by remember(record.date) {
        mutableStateOf(record.excludedMinutes.toString().takeIf { record.excludedMinutes > 0 }.orEmpty())
    }
    var credited by remember(record.date) { mutableStateOf(record.isCredited) }
    var error by remember(record.date) { mutableStateOf<String?>(null) }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.date.format(formatter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeInputField(
                    value = arrival,
                    onValueChange = { arrival = it },
                    label = "Приход",
                )
                TimeInputField(
                    value = departure,
                    onValueChange = { departure = it },
                    label = "Уход",
                )
                OutlinedTextField(
                    value = excluded,
                    onValueChange = { excluded = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Не учитывать (мин)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { credited = !credited },
                ) {
                    Checkbox(checked = credited, onCheckedChange = { credited = it })
                    Text("Зачёт — полный рабочий день")
                }
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
                    val arrivalMinutes = arrival.takeIf { !it.isEmpty }?.toMinutesOfDay()
                    val departureMinutes = departure.takeIf { !it.isEmpty }?.toMinutesOfDay()
                    if ((!arrival.isEmpty && arrivalMinutes == null) ||
                        (!departure.isEmpty && departureMinutes == null)
                    ) {
                        error = TimeParser.TIME_INPUT_HINT
                        return@TextButton
                    }
                    onSave(
                        record.copy(
                            arrivalMinutes = arrivalMinutes,
                            departureMinutes = departureMinutes,
                            note = note.trim(),
                            excludedMinutes = excluded.toIntOrNull() ?: 0,
                            isCredited = credited,
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
