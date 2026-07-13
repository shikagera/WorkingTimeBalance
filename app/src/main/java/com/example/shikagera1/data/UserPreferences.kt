package com.example.shikagera1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    private val lastWarningBannerDateKey = stringPreferencesKey("last_warning_banner_date")
    private val accumulatedBalanceKey = intPreferencesKey("accumulated_balance_minutes")
    private val manualResetDateKey = stringPreferencesKey("manual_reset_date")

    val lastWarningBannerDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[lastWarningBannerDateKey]?.let(LocalDate::parse)
    }

    val accumulatedBalanceMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[accumulatedBalanceKey] ?: 0
    }

    val manualResetDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[manualResetDateKey]?.let(LocalDate::parse)
    }

    suspend fun setLastWarningBannerDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[lastWarningBannerDateKey] = date.toString()
        }
    }

    suspend fun addToAccumulatedBalance(minutes: Int) {
        if (minutes == 0) return
        context.dataStore.edit { prefs ->
            val current = prefs[accumulatedBalanceKey] ?: 0
            prefs[accumulatedBalanceKey] = current + minutes
        }
    }

    suspend fun resetWeeklyBalance(today: LocalDate = LocalDate.now()) {
        context.dataStore.edit { prefs ->
            prefs[accumulatedBalanceKey] = 0
            prefs[manualResetDateKey] = today.toString()
        }
    }
}