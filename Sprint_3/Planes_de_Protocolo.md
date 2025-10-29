

-----

### 1\. Manifest — Permisos y Features (`AndroidManifest.xml`)

Añade los permisos y la característica Wi-Fi Direct obligatorios:

```xml
<uses-feature android:name="android.hardware.wifi.direct" android:required="false" />

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

-----

### 2\. Pedir Permisos en Tiempo de Ejecución

Implementa la solicitud dinámica de permisos. Verifica la versión de Android para solicitar los permisos de Bluetooth/Wi-Fi (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES`) o solo Ubicación, según corresponda.

#### Ejemplo de Solicitud de Permisos (Kotlin):

```kotlin
val perms = mutableListOf<String>()
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
  perms += Manifest.permission.BLUETOOTH_SCAN
  perms += Manifest.permission.BLUETOOTH_CONNECT
}
perms += Manifest.permission.ACCESS_FINE_LOCATION
requestPermissions(perms.toTypedArray(), REQUEST_CODE_PERMS)
```

#### Comandos ADB para Pruebas Rápidas:

```bash
adb shell pm grant com.tu.paquete android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.tu.paquete android.permission.BLUETOOTH_SCAN
adb shell pm grant com.tu.paquete android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.tu.paquete android.permission.NEARBY_WIFI_DEVICES
```

-----

### 3\. Declarar e Inicializar Wi-Fi P2P (`WifiP2pManager`)

Inicializa el servicio `WifiP2pManager` en tu `MainActivity` o en el componente de servicio principal:

```kotlin
val wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
val channel = wifiP2pManager.initialize(this, mainLooper, null)
```

-----

### 4\. Iniciar Descubrimiento y Recibir Peers

Llama a `discoverPeers(channel, ActionListener)` para iniciar el escaneo. Se debe registrar un `BroadcastReceiver` para la acción **`WIFI_P2P_PEERS_CHANGED_ACTION`** y, al recibir la notificación, solicitar la lista de peers con `wifiP2pManager.requestPeers(channel)`.

#### Recepción Básica de Peers:

```kotlin
val filter = IntentFilter().apply {
  addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
}
val receiver = object: BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
      wifiP2pManager.requestPeers(channel) { peerList ->
        // actualiza UI con peerList.deviceList
      }
    }
  }
}
registerReceiver(receiver, filter)
```

-----

### 5\. Servicio en Segundo Plano para Escaneos Periódicos

Para escanear aun si la app no está en primer plano, se requiere un **`ForegroundService`** que lance `discoverPeers()` periódicamente.

#### Solicitud para Ignorar Optimización de Batería:

```kotlin
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
intent.data = Uri.parse("package:$packageName")
startActivity(intent)
```

-----
