package it.scadenziario.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, pad: PaddingValues) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) vm.message = "Permesso notifiche negato: attivalo dalle impostazioni di Android"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Section("Aspetto")
        Label("Tema")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Automatico", "Chiaro", "Scuro").forEachIndexed { i, l ->
                FilterChip(selected = s.theme == i, onClick = { vm.updateSettings { it.copy(theme = i) } }, label = { Text(l) })
            }
        }

        Label("Colore")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (Build.VERSION.SDK_INT >= 31) {
                FilterChip(
                    selected = s.colorIndex < 0,
                    onClick = { vm.updateSettings { it.copy(colorIndex = -1) } },
                    label = { Text("Colori del telefono") }
                )
            }
            val selected = if (s.colorIndex < 0 && Build.VERSION.SDK_INT < 31) 0 else s.colorIndex
            Seeds.forEachIndexed { i, (name, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { vm.updateSettings { it.copy(colorIndex = i) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected == i) Icon(Icons.Default.Check, contentDescription = "Selezionato", tint = Color.White)
                    }
                    Text(name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Label("Tipo di carattere")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontFamilies.forEachIndexed { i, (name, family) ->
                FilterChip(
                    selected = s.font == i,
                    onClick = { vm.updateSettings { it.copy(font = i) } },
                    label = { Text(name, fontFamily = family) }
                )
            }
        }

        var scale by remember(s.fontScale) { mutableFloatStateOf(s.fontScale) }
        Label("Dimensione del testo: ${(scale * 100).roundToInt()}%")
        Slider(
            value = scale,
            onValueChange = { scale = it },
            onValueChangeFinished = { vm.updateSettings { it.copy(fontScale = scale) } },
            valueRange = 0.85f..1.5f
        )
        Text("Anteprima: Latte intero 1 L — scade il 15/10/2026", color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("Notifiche di scadenza")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Avvisi di scadenza", Modifier.weight(1f))
            Switch(
                checked = s.notif,
                onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.updateSettings { it.copy(notif = on) }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Avvisami quando mancano", Modifier.weight(1f))
            OutlinedButton(
                onClick = { if (s.warnDays > 1) vm.updateSettings { it.copy(warnDays = s.warnDays - 1) } },
                modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)
            ) { Text("−") }
            Text("${s.warnDays} g", fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { if (s.warnDays < 60) vm.updateSettings { it.copy(warnDays = s.warnDays + 1) } },
                modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)
            ) { Text("+") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ora dell'avviso", Modifier.weight(1f))
            OutlinedButton(onClick = {
                TimePickerDialog(
                    ctx,
                    { _, h, m -> vm.updateSettings { it.copy(hour = h, minute = m) } },
                    s.hour, s.minute, DateFormat.is24HourFormat(ctx)
                ).show()
            }) { Text("%02d:%02d".format(s.hour, s.minute)) }
        }

        Button(
            onClick = {
                val soon = vm.products.value.filter { it.expiryDay <= Dates.today() + s.warnDays }
                val (title, text) = expirySummary(soon) ?: ("Tutto in ordine" to "Nessun prodotto scaduto o in scadenza.")
                Notifier.show(ctx, title, text)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Invia una notifica di prova") }

        Text(
            "L'avviso arriva una volta al giorno all'ora scelta, solo se ci sono prodotti scaduti o in scadenza. " +
                "I giorni scelti servono anche per colorare di arancione i prodotti nelle liste.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Section(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Label(t: String) {
    Text(t, fontWeight = FontWeight.SemiBold)
}
