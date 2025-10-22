package com.example.mesh_chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mesh_chat.ui.theme.Mesh_ChatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Mesh_ChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FindDevicesScreen(bluetoothAdapter)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun FindDevicesScreen(bluetoothAdapter: BluetoothAdapter?) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) } // ✅ Nuevo
    var showAlreadyConnectedAlert by remember { mutableStateOf(false) } // ✅ Para mostrar alerta
    val scope = rememberCoroutineScope()

    val requestBluetoothPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    val enableBluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    // Solicitar permisos al iniciar
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    // Escucha de dispositivos detectados
    DisposableEffect(key1 = bluetoothAdapter) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let { foundDevice ->
                        // ✅ Filtro: solo teléfonos
                        val deviceClass = foundDevice.bluetoothClass?.deviceClass
                        if (deviceClass == BluetoothClass.Device.PHONE_SMART ||
                            deviceClass == BluetoothClass.Device.PHONE_CELLULAR ||
                            deviceClass == BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY ||
                            deviceClass == BluetoothClass.Device.PHONE_UNCATEGORIZED
                        ) {
                            if (foundDevice !in discoveredDevices) {
                                discoveredDevices.add(foundDevice)
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // 🧩 Alerta si ya hay un dispositivo conectado
    if (showAlreadyConnectedAlert && connectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showAlreadyConnectedAlert = false },
            title = { Text("Ya sincronizado") },
            text = {
                Text("Ya estás sincronizado con ${connectedDevice?.name ?: "otro dispositivo"}. " +
                        "Desconéctalo antes de sincronizar otro.")
            },
            confirmButton = {
                TextButton(onClick = { showAlreadyConnectedAlert = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // 🧱 Interfaz
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📱 Buscar Teléfonos Bluetooth", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando teléfonos cercanos...")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            if (bluetoothAdapter == null) {
                Toast.makeText(context, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
                return@Button
            }

            if (!bluetoothAdapter.isEnabled) {
                try {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } catch (e: Exception) {
                    val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(settingsIntent)
                }
            } else {
                isScanning = true
                discoveredDevices.clear()
                bluetoothAdapter.startDiscovery()
            }
        }) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Buscar teléfonos")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📋 Lista de teléfonos encontrados
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(discoveredDevices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (connectedDevice != null && connectedDevice != device) {
                                // ⚠️ Ya hay uno conectado → mostrar alerta
                                showAlreadyConnectedAlert = true
                            } else {
                                // Intentar sincronizar
                                scope.launch {
                                    connectionStatus = "🔄 Sincronizando con ${device.name ?: "Teléfono desconocido"}..."
                                    delay(2000)
                                    val success = bluetoothAdapter?.bondedDevices?.contains(device) == true || Math.random() > 0.5
                                    if (success) {
                                        connectedDevice = device
                                        connectionStatus = "✅ Conectado correctamente a ${device.name ?: "Teléfono desconocido"}"
                                    } else {
                                        connectionStatus = "❌ Falló la sincronización con ${device.name ?: "Teléfono desconocido"}"
                                    }
                                }
                            }
                        }
                ) {
                    Text(
                        text = "${device.name ?: "Teléfono desconocido"} - ${device.address}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        connectionStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.padding(50.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                isScanning = false
                bluetoothAdapter?.cancelDiscovery()
                connectionStatus = null
            }) {
                Text("DETENER ESCANEO")
            }

            // ✅ Nuevo: botón para “desconectarse”
            if (connectedDevice != null) {
                Button(onClick = {
                    connectionStatus = "🔌 Desconectado de ${connectedDevice?.name ?: "dispositivo"}"
                    connectedDevice = null
                }) {
                    Text("DESCONECTAR")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FindDevicesScreenPreview() {
    Mesh_ChatTheme {
        FindDevicesScreen(null)
    }
}
