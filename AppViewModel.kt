package it.scadenziario.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorState(val product: Product, val banner: String?)
data class ExistingState(val list: List<Product>, val expiryDay: Long?)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val store = SettingsStore(app)

    val products: StateFlow<List<Product>> =
        dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<AppSettings> =
        store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    var editor by mutableStateOf<EditorState?>(null)
    var existing by mutableStateOf<ExistingState?>(null)
    var loading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    init {
        // Ripianifica gli avvisi solo quando cambiano acceso/spento o l'orario
        viewModelScope.launch {
            store.flow.map { Triple(it.notif, it.hour, it.minute) }.distinctUntilChanged().collect { (on, h, m) ->
                if (on) Reminders.schedule(app, h, m) else Reminders.cancel(app)
            }
        }
    }

    fun open(p: Product) { editor = EditorState(p, null) }
    fun openNew() { editor = EditorState(Product(), null) }
    fun close() { editor = null }

    fun onBarcode(raw: String) {
        viewModelScope.launch {
            val sc = parseScan(raw)
            if (sc.code.isBlank()) return@launch
            val found = dao.byBarcode(sc.code)
            if (found.isNotEmpty()) {
                existing = ExistingState(found, sc.expiryDay)
                return@launch
            }
            loading = true
            val info = Off.lookup(sc.code)
            loading = false
            val base = Product(barcode = sc.code, expiryDay = sc.expiryDay ?: 0L)
            editor = if (info != null) {
                EditorState(
                    base.copy(
                        name = info.name, brand = info.brand, size = info.size, category = info.category.name,
                        kcal = info.kcal, fat = info.fat, sat = info.sat, carb = info.carb,
                        sugar = info.sugar, protein = info.protein, salt = info.salt
                    ),
                    "Dettagli compilati automaticamente" +
                        if (sc.expiryDay != null) " (anche la scadenza)." else ". Inserisci la data di scadenza."
                )
            } else {
                EditorState(base, "Prodotto non trovato online: inserisci nome e data di scadenza.")
            }
        }
    }

    fun newExpiryFor(p: Product, day: Long?) {
        existing = null
        editor = EditorState(
            p.copy(id = 0, quantity = 1, expiryDay = day ?: 0L, note = ""),
            "Stesso prodotto: inserisci la nuova data di scadenza."
        )
    }

    fun save(p: Product) {
        viewModelScope.launch {
            val isNew = p.id == 0L
            if (isNew) dao.insert(p) else dao.update(p)
            editor = null
            message = if (isNew) "Prodotto salvato" else "Modifiche salvate"
        }
    }

    fun delete(p: Product) {
        viewModelScope.launch {
            dao.delete(p)
            editor = null
            message = "Prodotto eliminato"
        }
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { store.save(change(settings.value)) }
    }
}
