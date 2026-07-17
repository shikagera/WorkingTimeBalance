package com.example.shikagera1.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shikagera1.data.AppDatabase
import com.example.shikagera1.data.TimeBalanceRepository
import com.example.shikagera1.data.UserPreferences
import com.example.shikagera1.domain.BalanceCalculator
import com.example.shikagera1.domain.DayRecord
import com.example.shikagera1.domain.LeaveTimePredictor
import com.example.shikagera1.domain.PeriodCalculator
import com.example.shikagera1.domain.TimeParser
import com.example.shikagera1.domain.WorkWeekCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class WeekDayItem(
    val date: LocalDate,
    val dayLabel: String,
    val balanceMinutes: Int,
    val note: String,
    val arrival: String?,
    val departure: String?,
    val isToday: Boolean,
    val excludedMinutes: Int = 0,
    val isPreviousWeek: Boolean = false,
)

data class MainUiState(
    val todayLabel: String = "",
    val weekBalanceMinutes: Int = 0,
    val todayRecord: DayRecord? = null,
    val isDayClosed: Boolean = false,
    val predictedDeparture: String? = null,
    /** Minutes from midnight for predicted leave; used by live countdown timer. */
    val predictedDepartureMinutes: Int? = null,
    /** Arrival minutes from midnight when day is open; drives live elapsed timer. */
    val activeArrivalMinutes: Int? = null,
    val currentWeekDays: List<WeekDayItem> = emptyList(),
    val previousWeekDays: List<WeekDayItem> = emptyList(),
    val showResetWarning: Boolean = false,
    val resetWarningMessage: String = "",
    val showResetConfirmDialog: Boolean = false,
    val manualArrivalDigits: String = "",
    val manualDepartureDigits: String = "",
    val showExcludeSection: Boolean = false,
    val showPreviousWeek: Boolean = false,
    val excludeMinutesInput: String = "",
    val editingDay: DayRecord? = null,
    val inputError: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = UserPreferences(application)
    private val repository = TimeBalanceRepository(
        dao = AppDatabase.getInstance(application).dayEntryDao(),
        preferences = preferences,
    )

    private val today = MutableStateFlow(LocalDate.now())
    private val manualArrivalDigits = MutableStateFlow("")
    private val manualDepartureDigits = MutableStateFlow("")
    private val showExcludeSection = MutableStateFlow(false)
    private val excludeMinutesInput = MutableStateFlow("")
    private val editingDay = MutableStateFlow<DayRecord?>(null)
    private val inputError = MutableStateFlow<String?>(null)
    private val showResetConfirmDialog = MutableStateFlow(false)
    private val showPreviousWeek = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            try {
                repository.purgeExpiredRecords()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to purge expired records", e)
            }
        }
    }

    private val recordsFlow = today.flatMapLatest { date ->
        repository.observeRetainedRecords(date)
    }

    private val prefsState = combine(
        preferences.lastWarningBannerDate,
        preferences.accumulatedBalanceMinutes,
        preferences.manualResetDate,
    ) { lastBannerDate, accumulatedBalance, manualResetDate ->
        PrefsSlice(lastBannerDate, accumulatedBalance, manualResetDate)
    }

    private val dataState = combine(
        recordsFlow,
        prefsState,
        today,
    ) { records, prefs, currentDate ->
        DataSlice(
            records = records,
            lastBannerDate = prefs.lastBannerDate,
            accumulatedBalance = prefs.accumulatedBalance,
            manualResetDate = prefs.manualResetDate,
            currentDate = currentDate,
        )
    }

    private val manualInputs = combine(
        manualArrivalDigits,
        manualDepartureDigits,
    ) { arrivalDigits, departureDigits ->
        arrivalDigits to departureDigits
    }

    private val excludeInputs = combine(
        showExcludeSection,
        excludeMinutesInput,
    ) { visible, input ->
        visible to input
    }

    private val dialogInputs = combine(
        editingDay,
        inputError,
        showResetConfirmDialog,
    ) { editing, error, resetDialog ->
        DialogSlice(editing, error, resetDialog)
    }

    private val uiInputs = combine(
        manualInputs,
        excludeInputs,
        dialogInputs,
        showPreviousWeek,
    ) { manual, exclude, dialog, previousWeekVisible ->
        UiInputSlice(
            arrivalDigits = manual.first,
            departureDigits = manual.second,
            excludeVisible = exclude.first,
            excludeInput = exclude.second,
            editing = dialog.editing,
            error = dialog.error,
            resetDialog = dialog.resetDialog,
            showPreviousWeek = previousWeekVisible,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        dataState,
        uiInputs,
    ) { data, inputs ->
        buildUiState(data, inputs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun refreshToday() {
        viewModelScope.launch {
            val now = LocalDate.now()
            today.value = now
            try {
                repository.purgeExpiredRecords(now)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to purge expired records", e)
            }
        }
    }

    fun clockIn() {
        val now = currentMinutesOfDay()
        viewModelScope.launch {
            try {
                repository.updateArrival(LocalDate.now(), now)
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clock in", e)
                inputError.value = "Не удалось сохранить время прихода"
            }
        }
    }

    fun clockOut() {
        val now = currentMinutesOfDay()
        viewModelScope.launch {
            try {
                repository.updateDeparture(LocalDate.now(), now)
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clock out", e)
                inputError.value = "Не удалось сохранить время ухода"
            }
        }
    }

    fun onManualArrivalChanged(value: String) {
        manualArrivalDigits.value = TimeParser.sanitizeDigits(value)
    }

    fun onManualDepartureChanged(value: String) {
        manualDepartureDigits.value = TimeParser.sanitizeDigits(value)
    }

    fun applyManualArrival() {
        val digits = manualArrivalDigits.value
        if (!TimeParser.isValidTimeDigits(digits)) {
            inputError.value = "Введите время: 4 цифры (ЧЧММ) или 3 цифры для утра (810 → 08:10)"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateArrival(LocalDate.now(), TimeParser.parseCompact(digits)!!)
                manualArrivalDigits.value = ""
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply manual arrival", e)
                inputError.value = "Не удалось сохранить время прихода"
            }
        }
    }

    fun applyManualDeparture() {
        val digits = manualDepartureDigits.value
        if (!TimeParser.isValidTimeDigits(digits)) {
            inputError.value = "Введите время: 4 цифры (ЧЧММ) или 3 цифры для утра (810 → 08:10)"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateDeparture(LocalDate.now(), TimeParser.parseCompact(digits)!!)
                manualDepartureDigits.value = ""
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply manual departure", e)
                inputError.value = "Не удалось сохранить время ухода"
            }
        }
    }

    fun toggleExcludeSection() {
        showExcludeSection.value = !showExcludeSection.value
        if (!showExcludeSection.value) {
            excludeMinutesInput.value = ""
        }
    }

    fun togglePreviousWeek() {
        showPreviousWeek.value = !showPreviousWeek.value
    }

    fun onExcludeMinutesChanged(value: String) {
        excludeMinutesInput.value = value.filter { it.isDigit() }.take(4)
    }

    fun applyExcludeMinutes() {
        val minutes = excludeMinutesInput.value.toIntOrNull()
        if (minutes == null || minutes <= 0) {
            inputError.value = "Введите количество минут больше 0"
            return
        }
        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val current = repository.getDay(todayDate)?.excludedMinutes ?: 0
                repository.updateExcludedMinutes(todayDate, current + minutes)
                excludeMinutesInput.value = ""
                showExcludeSection.value = false
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply exclude minutes", e)
                inputError.value = "Не удалось сохранить минуты отсутствия"
            }
        }
    }

    fun openDayEditor(date: LocalDate) {
        if (!isEditableDate(date)) {
            inputError.value = "Этот день недоступен для редактирования"
            return
        }
        viewModelScope.launch {
            try {
                val record = repository.getDay(date) ?: DayRecord(date = date)
                editingDay.value = record
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open day editor", e)
                inputError.value = "Не удалось открыть день для редактирования"
            }
        }
    }

    fun dismissDayEditor() {
        editingDay.value = null
    }

    fun saveEditedDay(record: DayRecord) {
        if (!isEditableDate(record.date)) {
            inputError.value = "Этот день недоступен для редактирования"
            editingDay.value = null
            return
        }
        viewModelScope.launch {
            try {
                repository.saveDay(record)
                editingDay.value = null
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save edited day", e)
                inputError.value = "Не удалось сохранить изменения"
            }
        }
    }

    fun requestResetBalance() {
        showResetConfirmDialog.value = true
    }

    fun dismissResetConfirmDialog() {
        showResetConfirmDialog.value = false
    }

    fun confirmResetBalance() {
        viewModelScope.launch {
            try {
                preferences.resetWeeklyBalance(LocalDate.now())
                showResetConfirmDialog.value = false
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset balance", e)
                inputError.value = "Не удалось сбросить баланс"
            }
        }
    }

    fun dismissResetWarning() {
        viewModelScope.launch {
            try {
                preferences.setLastWarningBannerDate(LocalDate.now())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss reset warning", e)
            }
        }
    }

    private fun buildUiState(data: DataSlice, inputs: UiInputSlice): MainUiState {
        val records = data.records
        val currentDate = data.currentDate
        val manualResetDate = data.manualResetDate

        val todayRecord = records.find { it.date == currentDate }
        val isDayClosed = todayRecord?.isClosed == true
        val weekBalance = BalanceCalculator.totalBalance(
            records = records,
            accumulatedBalanceMinutes = data.accumulatedBalance,
            today = currentDate,
            manualResetDate = manualResetDate,
        )
        val balanceBeforeToday = BalanceCalculator.balanceBeforeDate(
            records = records,
            date = currentDate,
            accumulatedBalanceMinutes = data.accumulatedBalance,
            today = currentDate,
            manualResetDate = manualResetDate,
        )

        val predictedDepartureMinutes = if (!isDayClosed) {
            todayRecord?.arrivalMinutes?.let { arrival ->
                LeaveTimePredictor.predictedDepartureMinutes(
                    arrivalMinutes = arrival,
                    balanceBeforeToday = balanceBeforeToday,
                    date = currentDate,
                )
            }
        } else {
            null
        }
        val predictedDeparture = predictedDepartureMinutes?.let(TimeParser::formatMinutes)
        val activeArrivalMinutes = if (!isDayClosed) todayRecord?.arrivalMinutes else null

        val locale = Locale.forLanguageTag("ru")
        val currentWeekDays = WorkWeekCalculator.visibleWorkDays(currentDate)
            .map { date -> toWeekDayItem(date, records, currentDate, locale, isPreviousWeek = false) }
            .reversed()

        val previousWeekDays = WorkWeekCalculator.previousWeekWorkDays(currentDate)
            .map { date -> toWeekDayItem(date, records, currentDate, locale, isPreviousWeek = true) }
            .reversed()

        val shouldWarn = PeriodCalculator.shouldShowResetWarning(currentDate)
        val showBanner = shouldWarn && data.lastBannerDate != currentDate

        return MainUiState(
            todayLabel = buildTodayLabel(currentDate),
            weekBalanceMinutes = weekBalance,
            todayRecord = todayRecord,
            isDayClosed = isDayClosed,
            predictedDeparture = predictedDeparture,
            predictedDepartureMinutes = predictedDepartureMinutes,
            activeArrivalMinutes = activeArrivalMinutes,
            currentWeekDays = currentWeekDays,
            previousWeekDays = previousWeekDays,
            showResetWarning = showBanner,
            resetWarningMessage = PeriodCalculator.resetWarningMessage(currentDate),
            showResetConfirmDialog = inputs.resetDialog,
            manualArrivalDigits = inputs.arrivalDigits,
            manualDepartureDigits = inputs.departureDigits,
            showExcludeSection = inputs.excludeVisible,
            showPreviousWeek = inputs.showPreviousWeek,
            excludeMinutesInput = inputs.excludeInput,
            editingDay = inputs.editing,
            inputError = inputs.error,
        )
    }

    private fun isEditableDate(date: LocalDate): Boolean {
        val todayDate = LocalDate.now()
        return !date.isBefore(WorkWeekCalculator.retentionStartDate(todayDate)) &&
            !date.isAfter(todayDate) &&
            WorkWeekCalculator.isWorkday(date)
    }

    private fun toWeekDayItem(
        date: LocalDate,
        records: List<DayRecord>,
        currentDate: LocalDate,
        locale: Locale,
        isPreviousWeek: Boolean,
    ): WeekDayItem {
        val record = records.find { it.date == date }
        return WeekDayItem(
            date = date,
            dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            balanceMinutes = record?.let(BalanceCalculator::dailyBalance) ?: 0,
            note = record?.note.orEmpty(),
            arrival = record?.arrivalMinutes?.let(TimeParser::formatMinutes),
            departure = record?.departureMinutes?.let(TimeParser::formatMinutes),
            isToday = date == currentDate,
            excludedMinutes = record?.excludedMinutes ?: 0,
            isPreviousWeek = isPreviousWeek,
        )
    }

    private fun buildTodayLabel(date: LocalDate): String {
        val locale = Locale.forLanguageTag("ru")
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        return dayName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    private fun currentMinutesOfDay(): Int {
        val now = java.time.LocalTime.now()
        return now.hour * 60 + now.minute
    }

    private data class PrefsSlice(
        val lastBannerDate: LocalDate?,
        val accumulatedBalance: Int,
        val manualResetDate: LocalDate?,
    )

    private data class DataSlice(
        val records: List<DayRecord>,
        val lastBannerDate: LocalDate?,
        val accumulatedBalance: Int,
        val manualResetDate: LocalDate?,
        val currentDate: LocalDate,
    )

    private data class DialogSlice(
        val editing: DayRecord?,
        val error: String?,
        val resetDialog: Boolean,
    )

    private data class UiInputSlice(
        val arrivalDigits: String,
        val departureDigits: String,
        val excludeVisible: Boolean,
        val excludeInput: String,
        val editing: DayRecord?,
        val error: String?,
        val resetDialog: Boolean,
        val showPreviousWeek: Boolean,
    )

    companion object {
        private const val TAG = "MainViewModel"
    }
}