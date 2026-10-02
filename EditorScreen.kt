package it.scadenziario.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: AppViewModel, ed: EditorState) {
    val p0 = ed.product
    val isNew = p0.id == 0L

    var name by remember(ed) { mutableStateOf(p0.name) }
    var brand by remember(ed) { mutableStateOf(p0.brand) }
    var qty by remember(ed) { mutableIntStateOf(p0.quantity) }
    var size by remember(ed) { mutableStateOf(p0.size) }
    var cat by remember(ed) { mutableStateOf(Category.from(p0.category)) }
    var day by remember(ed) { mutableLongStateOf(p0.expiryDay) }
    var kcal by remember(ed) { mutableStateOf(p0.kcal) }
    var fat by remember(ed) { mutableStateOf(p0.fat) }
    var sat by remember(ed) { mutableStateOf(p0.sat) }
    var carb by remember(ed) { mutableStateOf(p0.carb) }
    var sugar by remember(ed) { mutableStateOf(p0.sugar) }
    var protein by remember(ed) { mutableStateOf(p0.protein) }
    var salt by remember(ed) { mutableStateOf(p0.salt) }
    var note by remember(ed) { mutableStateOf(p0.note) }

    var showNut by remember(ed) { mutableStateOf(listOf(p0.kcal, p0.fat, p0.carb, p0.protein).any { it.isNotBlank() }) }
    var catOpen by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun build() = p0.copy(
        name = name.trim(), brand = brand.trim(), quantity = qty, size = size.trim(),
        category = cat.name, expiryDay = day, kcal = kcal.trim(), fat = fat.trim(), sat = sat.trim(),
        carb = carb.trim(), sugar = sugar.trim(), protein = protein.trim(), salt = salt.trim(), note = note.trim()
    )

    fun trySave() {
        when {
            name.isBlank() -> vm.message = "Scrivi il nome del prodotto"
            day == 0L -> vm.message = "Inserisci la data di scadenza"
            else -> vm.save(build())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Nuovo prodotto" else "Dettagli prodotto") },
                navigationIcon = {
                    IconButton(onClick = { vm.close() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ed.banner?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(it, Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                if (p0.barcode.isNotBlank()) "Codice: ${p0.barcode}" else "Senza codice a barre",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(name, { name = it }, label = { Text("Nome del prodotto *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // Quantità con + e −
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantità (pezzi)", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { if (qty > 0) qty-- }, modifier = Modifier.size(48.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−") }
                Text("$qty", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedButton(onClick = { qty++ }, modifier = Modifier.size(48.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+") }
            }
            OutlinedTextField(size, { size = it }, label = { Text("Formato / peso (es. 500 g)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                OutlinedTextField(
                    value = "${cat.emoji} ${cat.label} — IVA ${cat.iva}%",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo di prodotto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catOpen) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                    Category.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${c.emoji} ${c.label} — IVA ${c.iva}%") },
                            onClick = { cat = c; catOpen = false }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDate = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (day > 0) "📅  Scadenza: ${Dates.format(day)}" else "📅  Scegli la data di scadenza *") }

            TextButton(onClick = { showNut = !showNut }) {
                Text(if (showNut) "Nascondi valori nutrizionali" else "Valori nutrizionali (per 100 g)")
            }
            if (showNut) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutField("Calorie (kcal)", kcal, { kcal = it }, Modifier.weight(1f))
                    NutField("Grassi (g)", fat, { fat = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutField("di cui saturi (g)", sat, { sat = it }, Modifier.weight(1f))
                    NutField("Carboidrati (g)", carb, { carb = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutField("di cui zuccheri (g)", sugar, { sugar = it }, Modifier.weight(1f))
                    NutField("Proteine (g)", protein, { protein = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutField("Sale (g)", salt, { salt = it }, Modifier.weight(1f))
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }

            OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Button(onClick = { trySave() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (isNew) "Salva prodotto" else "Salva modifiche", fontWeight = FontWeight.Bold)
            }
            if (!isNew) {
                OutlinedButton(
                    onClick = { vm.newExpiryFor(build(), null) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Aggiungi un'altra scadenza") }
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Elimina prodotto", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showDate) {
        val ps = rememberDatePickerState(initialSelectedDateMillis = if (day > 0) day * 86_400_000L else null)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    ps.selectedDateMillis?.let { day = it / 86_400_000L }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Annulla") } }
        ) { DatePicker(state = ps) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare “${p0.name}”?") },
            text = { Text("L'operazione non si può annullare.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(p0) }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun NutField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
