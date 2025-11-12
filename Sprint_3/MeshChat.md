# 💬 Programa MeshChat V1
Aplicación Android para comunicación **Wi-Fi Direct (P2P)** sin necesidad de Internet.  
Cada usuario puede descubrir, conectar y chatear con otros dispositivos cercanos.

---

## 🧠 Descripción general
Este proyecto crea una conexión **peer-to-peer (Wi-Fi Direct)** entre dispositivos Android para enviar y recibir mensajes en tiempo real.  
Usa `WifiP2pManager`, `Socket` y `Compose` para la interfaz.

---

## ⚙️ Archivo principal: `MainActivity.kt`
```kotlin
package com.example.chatdirect

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.chatdirect.ui.theme.ChatdirectTheme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity(), WifiP2pManager.ConnectionInfoListener {

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private var peers by mutableStateOf<List<WifiP2pDevice>>(emptyList())
    private var isConnected by mutableStateOf(false)
    private var messages by mutableStateOf<List<String>>(emptyList())
    private var currentMessage by mutableStateOf("")
    private var sendReceive: SendReceive? = null

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList.toList()
        Log.d("MainActivity", "PeerListListener found ${refreshedPeers.size} peers.")
    }

    private val serviceListener = WifiP2pManager.DnsSdServiceResponseListener { instanceName, _, srcDevice ->
        if (instanceName.equals(SERVICE_INSTANCE, ignoreCase = true)) {
            if (peers.none { it.deviceAddress == srcDevice.deviceAddress }) {
                peers = peers + srcDevice
                Log.d("MainActivity", "ChatDirect service discovered: ${srcDevice.deviceName}")
            }
        }
    }

    private val txtRecordListener = WifiP2pManager.DnsSdTxtRecordListener { _, _, _ -> }

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                messages = messages + "Amigo: $tempMsg"
                true
            }
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        receiver = WiFiDirectBroadcastReceiver(manager, channel, this, this, peerListListener)
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener)

        setContent {
            ChatdirectTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (isConnected) {
                            TopAppBar(
                                title = { Text("Chat Activo") },
                                navigationIcon = {
                                    IconButton(onClick = { disconnect() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Desconectar")
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (isConnected) {
                        ChatScreen(
                            modifier = Modifier.padding(innerPadding),
                            messages = messages,
                            currentMessage = currentMessage,
                            onCurrentMessageChange = { currentMessage = it },
                            onSendMessage = { sendMessage() }
                        )
                    } else {
                        DeviceDiscoveryScreen(
                            modifier = Modifier.padding(innerPadding),
                            peers = peers,
                            onDiscoverClick = { discoverServices() },
                            onPeerClick = { device -> connect(device) }
                        )
                    }
                }
            }
        }
    }
}
```
### 📡 Clase: WiFiDirectBroadcastReceiver
```kotlin
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity,
    private val connectionInfoListener: WifiP2pManager.ConnectionInfoListener,
    private val peerListListener: WifiP2pManager.PeerListListener
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("WiFiDirectBroadcastReceiver", "Wi-Fi P2P habilitado.")
                } else {
                    Log.e("WiFiDirectBroadcastReceiver", "Wi-Fi P2P deshabilitado.")
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return
                manager.requestPeers(channel, peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel, connectionInfoListener)
                    Log.d("WiFiDirectBroadcastReceiver", "Dispositivo conectado")
                } else {
                    (activity as MainActivity).onConnectionStateChange(false)
                    Log.d("WiFiDirectBroadcastReceiver", "Dispositivo desconectado")
                }
            }
        }
    }
}
```
### 💬 Interfaz: Pantalla de Descubrimiento
```kotlin
@Composable
fun DeviceDiscoveryScreen(
    modifier: Modifier = Modifier,
    peers: List<WifiP2pDevice>,
    onDiscoverClick: () -> Unit,
    onPeerClick: (WifiP2pDevice) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedButton(onClick = onDiscoverClick, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar usuarios de ChatDirect")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (peers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No se encontraron usuarios de ChatDirect.")
                Text("Presiona el botón para iniciar la búsqueda.")
            }
        } else {
            Text(
                "Usuarios encontrados:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(peers) { peer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onPeerClick(peer) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(peer.deviceName ?: "Dispositivo sin nombre") },
                            supportingContent = { Text(peer.deviceAddress) },
                            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}
```
### 💬 Interfaz: Pantalla de Chat
```kotlin
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messages: List<String>,
    currentMessage: String,
    onCurrentMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { message ->
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = currentMessage,
                onValueChange = onCurrentMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") }
            )
            IconButton(onClick = onSendMessage, enabled = currentMessage.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}
```
### 🧩 Vista previa de interfaces
```kotlin
@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryScreenPreview() {
    ChatdirectTheme {
        val dummyDevice = WifiP2pDevice().apply {
            deviceName = "Teléfono de Prueba"
            deviceAddress = "A1:B2:C3:D4:E5:F6"
        }
        DeviceDiscoveryScreen(peers = listOf(dummyDevice), onDiscoverClick = {}, onPeerClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    ChatdirectTheme {
        ChatScreen(
            messages = listOf("Yo: Hola!", "Amigo: ¡Qué tal!"),
            currentMessage = "Escribiendo...",
            onCurrentMessageChange = {},
            onSendMessage = {}
        )
    }
}
