

````markdown
# 📱 Código de Sincronización Bluetooth entre Dispositivos de Forma Directa

Este proyecto implementa una interfaz en **Jetpack Compose** para el **escaneo, detección y simulación de sincronización directa** de dispositivos Bluetooth.  
El objetivo es facilitar la conexión **Peer-to-Peer** entre teléfonos inteligentes.

---

## ✨ ¿Qué hace este código?

✅ **Verifica si el Bluetooth está encendido**  
Si no lo está, solicita al usuario activarlo o lo dirige directamente a la configuración del dispositivo.

🔍 **Muestra todos los dispositivos detectados**  
Durante el escaneo, se enlistan todos los teléfonos Bluetooth disponibles.

💬 **Da mensajes de estado claros**  
El usuario ve mensajes explícitos del proceso:
- 🔄 “Sincronizando…”
- ✅ “Conectado correctamente”
- ❌ “Falló la sincronización”

🧹 **Limpieza de estados al detener escaneo**  
Al detener la búsqueda, se cancela el escaneo y se limpia el estado de conexión.

📲 **Compatible con Android 12+ (manejo de permisos moderno)**  
Usa permisos como `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` con `ActivityResultContracts`.

---

## 🧩 Estructura del Código y Explicación

A continuación se explica el código por secciones, mostrando los fragmentos más relevantes y su función dentro del proyecto.

---

### 1️⃣ **Inicialización y Configuración de la Actividad Principal**

Encapsula la lógica principal de la app.  
Aquí se obtiene el adaptador Bluetooth y se renderiza la pantalla principal `FindDevicesScreen`.

```kotlin
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
````

🧠 **Qué hace:**

* Obtiene el **BluetoothManager** del sistema.
* Configura la UI con **Jetpack Compose**.
* Llama a `FindDevicesScreen()` para manejar la lógica de escaneo.

---

### 2️⃣ **Pantalla Principal: `FindDevicesScreen`**

Contiene toda la interfaz y la lógica reactiva del escaneo y conexión.

```kotlin
@SuppressLint("MissingPermission")
@Composable
fun FindDevicesScreen(bluetoothAdapter: BluetoothAdapter?) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var showAlreadyConnectedAlert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
```

🧠 **Qué hace:**

* Define **estados reactivos** para escaneo, conexión, mensajes, etc.
* Usa **corutinas** (`scope.launch`) para simular procesos asíncronos.
* Guarda los dispositivos detectados en una lista dinámica.

---

### 3️⃣ **Manejo de Permisos Bluetooth (Android 12+)**

Controla los permisos modernos requeridos para escanear y conectarse vía Bluetooth.

```kotlin
val requestBluetoothPermissions =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

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
```

🧠 **Qué hace:**

* Solicita permisos Bluetooth y ubicación dependiendo de la versión de Android.
* Asegura compatibilidad con Android 12+ y versiones anteriores.

---

### 4️⃣ **Detección de Dispositivos Bluetooth**

Usa un `BroadcastReceiver` para escuchar los dispositivos detectados durante el escaneo.

```kotlin
DisposableEffect(key1 = bluetoothAdapter) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                device?.let { foundDevice ->
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
    onDispose { context.unregisterReceiver(receiver) }
}
```

🧠 **Qué hace:**

* Registra un **BroadcastReceiver** para escuchar los eventos de detección (`ACTION_FOUND`).
* Filtra los resultados para mostrar **solo teléfonos inteligentes**.
* Agrega los dispositivos encontrados a la lista visible en pantalla.

---

### 5️⃣ **Interfaz de Usuario (UI)**

Diseña la pantalla principal para escanear y listar los dispositivos Bluetooth disponibles.

```kotlin
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
}
```

🧠 **Qué hace:**

* Muestra el botón **“Buscar teléfonos”**.
* Si el Bluetooth está apagado, abre la **configuración del dispositivo**.
* Si está activo, inicia la búsqueda de teléfonos cercanos.

---

### 6️⃣ **Listado y Sincronización Simulada**

Presenta los dispositivos detectados y simula una conexión al hacer clic en uno.

```kotlin
LazyColumn(modifier = Modifier.weight(1f)) {
    items(discoveredDevices) { device ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    if (connectedDevice != null && connectedDevice != device) {
                        showAlreadyConnectedAlert = true
                    } else {
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
```

🧠 **Qué hace:**

* Muestra una lista dinámica de dispositivos detectados.
* Al tocar uno, **simula la sincronización Bluetooth** y muestra el resultado.
* Controla si ya hay otro dispositivo conectado.

---

### 7️⃣ **Controles de Escaneo y Desconexión**

Permite detener el escaneo o desconectarse de un dispositivo activo.

```kotlin
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

    if (connectedDevice != null) {
        Button(onClick = {
            connectionStatus = "🔌 Desconectado de ${connectedDevice?.name ?: "dispositivo"}"
            connectedDevice = null
        }) {
            Text("DESCONECTAR")
        }
    }
}
```

🧠 **Qué hace:**

* **Detiene el escaneo** de dispositivos activos.
* **Limpia el estado** de conexión y muestra mensaje de desconexión.

---

## 🧱 Conclusión

Este código demuestra una implementación práctica de **escaneo y conexión Bluetooth simulada** en Android con **Jetpack Compose**, aplicando buenas prácticas como:

* 📶 **Permisos dinámicos modernos**
* ⚙️ **Estados reactivos y UI declarativa**
* 💬 **Mensajes de usuario claros y visuales**
* 🧩 **Compatibilidad con Android 12+ (S)**

---

👨‍💻 *Desarrollado por SirBlaster — Proyecto Sprint 2 (ITSX_Chat)*
📆 *2025*




## 🚀 Propuesta a Futuro: Sincronización Avanzada (BLE + Wi-Fi Direct)

Para una solución robusta de transferencia de archivos **Peer-to-Peer** (P2P), la siguiente evolución consiste en un flujo híbrido que utiliza **Bluetooth Low Energy (BLE)** para el descubrimiento de corto alcance y **Wi-Fi Direct** para la conexión de alta velocidad.

### Flujo de Implementación Propuesto

El proceso opera en cuatro etapas:

1.  **Descubrimiento (BLE Advertising):** La aplicación se anuncia usando un **Service UUID** propio para identificarse.
2.  **Escaneo (BLE Scanning):** La aplicación escanea, filtrando únicamente por el Service UUID conocido, extrayendo un **token/nonce** (datos del fabricante).
3.  **Negociación (Opcional):** Se realiza un *handshake* rápido para confirmar la intención de conexión.
4.  **Conexión de Alta Velocidad (Wi-Fi Direct):** Uno de los dispositivos crea un grupo (Group Owner - GO). Se obtiene la IP del GO y se abre un **socket TCP** para la transferencia de datos.

### 1\) BLE Advertising (Anunciarse)

```kotlin
// Dependencias: android.bluetooth.le
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import java.util.UUID

fun startBleAdvertise(adapter: BluetoothAdapter, appUuid: UUID, tokenBytes: ByteArray) {
    val advertiser: BluetoothLeAdvertiser? = adapter.bluetoothLeAdvertiser
    if (advertiser == null) {
        // no support
        return
    }

    val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(false) // solo discovery; para handshake usar GATT o sockets
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    val dataBuilder = AdvertiseData.Builder()
        // Publicar el service UUID (identifica apps Mesh Chat)
        .addServiceUuid(ParcelUuid(appUuid))
        // Manufacturer data es ideal para tokens cortos (1-4 bytes)
        .addManufacturerData(0xFFFF, tokenBytes) 
        .setIncludeDeviceName(false)

    val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) { /* ok */ }
        override fun onStartFailure(errorCode: Int) { /* manejar error */ }
    }

    advertiser.startAdvertising(settings, dataBuilder.build(), advertiseCallback)
}
```

### 2\) BLE Scanning (Filtrar por Service UUID)

```kotlin
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
// ...

fun startBleScan(adapter: BluetoothAdapter, appUuid: UUID, onFound: (ScanResult)->Unit) {
    val scanner = adapter.bluetoothLeScanner ?: return

    val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(appUuid)) // Solo escanear por nuestro Service UUID
        .build()

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onFound(result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onFound(it) }
        }
    }

    scanner.startScan(listOf(filter), settings, scanCallback)

    // Para detener:
    // scanner.stopScan(scanCallback)
}
```

### 3\) Intercambio Rápido y Negociación de Wi-Fi Direct

```kotlin
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import java.net.ServerSocket
import java.net.Socket
// ...

// Obtener info de la conexión (cuando se notifica)
// En WifiP2pManager.ConnectionInfoListener
override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
    if (info.groupFormed) {
        val PORT = 8888 // Puerto a usar para la comunicación TCP

        if (info.isGroupOwner) {
            // Soy GO: abrir ServerSocket y esperar conexiones
            val serverSocket = ServerSocket(PORT)
            val clientSocket = serverSocket.accept()
            // Manejar streams para recibir/enviar archivos
            // ...
        } else {
            // Soy cliente: conectar al GO por info.groupOwnerAddress.hostAddress
            val ip = info.groupOwnerAddress.hostAddress
            val socket = Socket(ip, PORT)
            // Enviar/recibir archivos por streams
            // ...
        }
    }
}
```

