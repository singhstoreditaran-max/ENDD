package it.scadenziario.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

fun startScan(ctx: Context, onCode: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(ctx, options).startScan()
        .addOnSuccessListener { b -> b.rawValue?.let(onCode) }
        .addOnFailureListener {
            Toast.makeText(ctx, "Scansione non riuscita. Controlla la connessione e riprova.", Toast.LENGTH_LONG).show()
        }
}

@Composable
fun HomeScreen(vm: AppViewModel, pad: PaddingValues) {
    val products by vm.products.collectAsState()
    val st by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val focus: FocusManager = LocalFocusManager.current

    var q by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val suggestions = remember(q, products) { if (q.isBlank()) emptyList() else search(products, q).take(6) }
    val results = remember(q, products, submitted) { if (submitted) search(products, q) else emptyList() }
    val today = Dates.today()
    val bad = products.count { it.expiryDay < today }
    val warn = products.count { it.expiryDay in today..(today + st.warnDays) }
    val ok = products.size - bad - warn
    val next = remember(products) { products.take(5) } // già ordinati per scadenza

    fun doSearch() { submitted = true; focus.clearFocus() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scadenziario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it; submitted = false },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Cerca un prodotto…") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (q.isNotEmpty()) IconButton(onClick = { q = ""; submitted = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancella")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() })
                    )
                    Button(onClick = { doSearch() }, modifier = Modifier.height(56.dp)) { Text("Cerca") }
                }

                // Suggerimenti mentre scrivi: un tocco apre il prodotto
                if (q.isNotBlank() && !submitted) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        if (suggestions.isEmpty()) {
                            Text("Nessun prodotto trovato", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            suggestions.forEachIndexed { i, p ->
                                ProductRow(p, st.warnDays) { vm.open(p) }
                                if (i < suggestions.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { startScan(ctx) { vm.onBarcode(it) } },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("📷  Esegui scansione", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { vm.openNew() }) { Text("+ Aggiungi senza codice a barre") }
            }
        }

        if (submitted) {
            item {
                Text(
                    "${results.size} risultati per “$q”",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(results, key = { it.id }) { p ->
                Column {
                    ProductRow(p, st.warnDays) { vm.open(p) }
                    HorizontalDivider()
                }
            }
        } else if (products.isEmpty()) {
            item {
                Text(
                    "Nessun prodotto registrato.\nPremi “Esegui scansione” e inquadra il codice a barre.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("scaduti", bad, Level.BAD, Modifier.weight(1f))
                        StatCard("entro ${st.warnDays} g", warn, Level.WARN, Modifier.weight(1f))
                        StatCard("ok", ok, Level.OK, Modifier.weight(1f))
                    }
                    Text(
                        "Prossime scadenze",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            items(next, key = { "n_${it.id}" }) { p ->
                Column {
                    ProductRow(p, st.warnDays) { vm.open(p) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, n: Int, level: Level, modifier: Modifier) {
    val (bg, fg) = levelColors(level)
    Card(modifier, colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg)) {
        Column(Modifier.padding(12.dp)) {
            Text("$n", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
