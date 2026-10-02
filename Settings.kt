package it.scadenziario.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class AppSettings(
    val theme: Int = 0,          // 0 automatico, 1 chiaro, 2 scuro
    val colorIndex: Int = -1,    // -1 = colori del telefono (Android 12+), altrimenti indice in Seeds
    val fontScale: Float = 1f,   // dimensione del testo
    val font: Int = 0,           // indice in FontFamilies
    val notif: Boolean = true,   // avvisi di scadenza attivi
    val warnDays: Int = 7,       // avvisa quando mancano N giorni
    val hour: Int = 9,           // ora dell'avviso
    val minute: Int = 0
)

class SettingsStore(private val ctx: Context) {
    private object K {
        val theme = intPreferencesKey("theme")
        val color = intPreferencesKey("color")
        val fontScale = floatPreferencesKey("font_scale")
        val font = intPreferencesKey("font")
        val notif = booleanPreferencesKey("notif")
        val warn = intPreferencesKey("warn")
        val hour = intPreferencesKey("hour")
        val minute = intPreferencesKey("minute")
    }

    val flow: Flow<AppSettings> = ctx.dataStore.data.map { p ->
        AppSettings(
            theme = p[K.theme] ?: 0,
            colorIndex = p[K.color] ?: -1,
            fontScale = p[K.fontScale] ?: 1f,
            font = p[K.font] ?: 0,
            notif = p[K.notif] ?: true,
            warnDays = p[K.warn] ?: 7,
            hour = p[K.hour] ?: 9,
            minute = p[K.minute] ?: 0
        )
    }

    suspend fun save(s: AppSettings) {
        ctx.dataStore.edit { p ->
            p[K.theme] = s.theme
            p[K.color] = s.colorIndex
            p[K.fontScale] = s.fontScale
            p[K.font] = s.font
            p[K.notif] = s.notif
            p[K.warn] = s.warnDays
            p[K.hour] = s.hour
            p[K.minute] = s.minute
        }
    }
}
