package com.example.shikagera1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    private val lastWarningBannerDateKey = stringPreferencesKey("last_warning_banner_date")
    private val manualResetDateKey = stringPreferencesKey("manual_reset_date")

    val lastWarningBannerDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[lastWarningBannerDateKey]?.let(LocalDate::parse)
    }

    val manualResetDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[manualResetDateKey]?.let(LocalDate::parse)
    }

    suspend fun setLastWarningBannerDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[lastWarningBannerDateKey] = date.toString()
        }
    }

    /** Ручной сброс: дни по [today] включительно перестают учитываться в балансе. */
    suspend fun resetBalance(today: LocalDate = LocalDate.now()) {
        context.dataStore.edit { prefs ->
            prefs[manualResetDateKey] = today.toString()
        }
    }
}
