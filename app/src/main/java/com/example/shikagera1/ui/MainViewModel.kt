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
import com.example.shikagera1.domain.TimeFieldInput
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
    /** Заполнено, только если уход позже 20:00 и засчитано меньше введённого. */
    val countedDeparture: String? = null,
    val isToday: Boolean,
    val excludedMinutes: Int = 0,
    val isCredited: Boolean = false,
    val isPreviousWeek: Boolean = false,
)

data class MainUiState(
    val todayLabel: String = "",
    val periodBalanceMinutes: Int = 0,
    val todayRecord: DayRecord? = null,
    /** День закрыт по времени или зачтён — кнопки прихода/ухода не нужны. */
    val isDayFinished: Boolean = false,
    val isTodayCredited: Boolean = false,
    val predictedDeparture: String? = null,
    /** Minutes from midnight for predicted leave; used by live countdown timer. */
    val predictedDepartureMinutes: Int? = null,
    /** Arrival minutes from midnight when day is open; drives live elapsed timer. */
    val activeArrivalMinutes: Int? = null,
    val todayExcludedMinutes: Int = 0,
    val currentWeekDays: List<WeekDayItem> = emptyList(),
    val previousWeekDays: List<WeekDayItem> = emptyList(),
    val showResetWarning: Boolean = false,
    val resetWarningMessage: String = "",
    val showResetConfirmDialog: Boolean = false,
    val manualArrival: TimeFieldInput = TimeFieldInput.EMPTY,
    val manualDeparture: TimeFieldInput = TimeFieldInput.EMPTY,
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
    )

    private val today = MutableStateFlow(LocalDate.now())
    private val manualArrival = MutableStateFlow(TimeFieldInput.EMPTY)
    private val manualDeparture = MutableStateFlow(TimeFieldInput.EMPTY)
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

    private val dataState = combine(
        recordsFlow,
        preferences.lastWarningBannerDate,
        preferences.manualResetDate,
        today,
    ) { records, lastBannerDate, manualResetDate, currentDate ->
        DataSlice(
            records = records,
            lastBannerDate = lastBannerDate,
            manualResetDate = manualResetDate,
            currentDate = currentDate,
        )
    }

    private val manualInputs = combine(
        manualArrival,
        manualDeparture,
    ) { arrival, departure ->
        arrival to departure
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
            arrival = manual.first,
            departure = manual.second,
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

    /** «Зачёт»: сегодняшний день засчитывается как полный рабочий. */
    fun creditToday() {
        setTodayCredited(true)
    }

    fun uncreditToday() {
        setTodayCredited(false)
    }

    private fun setTodayCredited(credited: Boolean) {
        viewModelScope.launch {
            try {
                repository.setCredited(LocalDate.now(), credited)
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set credited", e)
                inputError.value = "Не удалось сохранить зачёт"
            }
        }
    }

    fun onManualArrivalChanged(value: TimeFieldInput) {
        manualArrival.value = value
    }

    fun onManualDepartureChanged(value: TimeFieldInput) {
        manualDeparture.value = value
    }

    fun applyManualArrival() {
        val minutes = manualArrival.value.toMinutesOfDay()
        if (minutes == null) {
            inputError.value = TimeParser.TIME_INPUT_HINT
            return
        }
        viewModelScope.launch {
            try {
                repository.updateArrival(LocalDate.now(), minutes)
                manualArrival.value = TimeFieldInput.EMPTY
                inputError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply manual arrival", e)
                inputError.value = "Не удалось сохранить время прихода"
            }
        }
    }

    fun applyManualDeparture() {
        val minutes = manualDeparture.value.toMinutesOfDay()
        if (minutes == null) {
            inputError.value = TimeParser.TIME_INPUT_HINT
            return
        }
        viewModelScope.launch {
            try {
                repository.updateDeparture(LocalDate.now(), minutes)
                manualDeparture.value = TimeFieldInput.EMPTY
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
                preferences.resetBalance(LocalDate.now())
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
        val isDayFinished = todayRecord?.isFinished == true
        val isTodayCredited = todayRecord?.isCredited == true
        val todayExcluded = todayRecord?.excludedMinutes ?: 0
        val periodBalance = BalanceCalculator.periodBalance(
            records = records,
            today = currentDate,
            manualResetDate = manualResetDate,
        )
        val balanceBeforeToday = BalanceCalculator.balanceBeforeDate(
            records = records,
            date = currentDate,
            today = currentDate,
            manualResetDate = manualResetDate,
        )

        val predictedDepartureMinutes = if (!isDayFinished) {
            todayRecord?.arrivalMinutes?.let { arrival ->
                LeaveTimePredictor.predictedDepartureMinutes(
                    arrivalMinutes = arrival,
                    balanceBeforeToday = balanceBeforeToday,
                    excludedMinutes = todayExcluded,
                    date = currentDate,
                )
            }
        } else {
            null
        }
        val predictedDeparture = predictedDepartureMinutes?.let(TimeParser::formatMinutes)
        val activeArrivalMinutes = if (!isDayFinished) todayRecord?.arrivalMinutes else null

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
            periodBalanceMinutes = periodBalance,
            todayRecord = todayRecord,
            isDayFinished = isDayFinished,
            isTodayCredited = isTodayCredited,
            predictedDeparture = predictedDeparture,
            predictedDepartureMinutes = predictedDepartureMinutes,
            activeArrivalMinutes = activeArrivalMinutes,
            todayExcludedMinutes = todayExcluded,
            currentWeekDays = currentWeekDays,
            previousWeekDays = previousWeekDays,
            showResetWarning = showBanner,
            resetWarningMessage = PeriodCalculator.resetWarningMessage(currentDate),
            showResetConfirmDialog = inputs.resetDialog,
            manualArrival = inputs.arrival,
            manualDeparture = inputs.departure,
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
            countedDeparture = record?.departureMinutes
                ?.let(BalanceCalculator::countedDepartureMinutes)
                ?.takeIf { it != record.departureMinutes }
                ?.let(TimeParser::formatMinutes),
            isToday = date == currentDate,
            excludedMinutes = record?.excludedMinutes ?: 0,
            isCredited = record?.isCredited == true,
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

    private data class DataSlice(
        val records: List<DayRecord>,
        val lastBannerDate: LocalDate?,
        val manualResetDate: LocalDate?,
        val currentDate: LocalDate,
    )

    private data class DialogSlice(
        val editing: DayRecord?,
        val error: String?,
        val resetDialog: Boolean,
    )

    private data class UiInputSlice(
        val arrival: TimeFieldInput,
        val departure: TimeFieldInput,
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
