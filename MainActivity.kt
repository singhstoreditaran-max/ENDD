package it.scadenziario.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            ScadenziarioTheme(settings) { AppRoot(vm) }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun AppRoot(vm: AppViewModel) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(1) } // 0 Prodotti, 1 Home, 2 Impostazioni
    val ed = vm.editor

    BackHandler(enabled = ed != null) { vm.close() }
    BackHandler(enabled = ed == null && tab != 1) { tab = 1 }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            vm.message = null
        }
    }

    val tabs = listOf(
        Tab("Prodotti", Icons.AutoMirrored.Filled.List),
        Tab("Home", Icons.Default.Home),
        Tab("Impostazioni", Icons.Default.Settings)
    )

    if (ed != null) {
        EditorScreen(vm, ed)
    } else {
        // La NavigationBar rispetta in automatico la barra di sistema in basso
        // (gesti oppure i 3 pulsanti), quindi i tasti non coprono mai le opzioni.
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        ) { pad ->
            when (tab) {
                0 -> ProductsScreen(vm, pad)
                1 -> HomeScreen(vm, pad)
                else -> SettingsScreen(vm, pad)
            }
        }
    }

    vm.existing?.let { ex ->
        val p = ex.list.first()
        AlertDialog(
            onDismissRequest = { vm.existing = null },
            title = { Text("Già registrato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${p.name}\n${ex.list.size} scadenza/e registrata/e. Prossima: ${Dates.format(p.expiryDay)}")
                    Button(onClick = { vm.newExpiryFor(p, ex.expiryDay) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Registra nuova scadenza")
                    }
                    OutlinedButton(onClick = { vm.existing = null; vm.open(p) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Apri e modifica")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { vm.existing = null }) { Text("Annulla") } }
        )
    }

    if (vm.loading) {
        Dialog(onDismissRequest = {}) {
            Card {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Cerco i dettagli del prodotto…")
                }
            }
        }
    }
}
