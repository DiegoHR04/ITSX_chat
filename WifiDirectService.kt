package com.example.wifidirectchat

import android.app.Service
import android.content.*
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class WifiDirectService : Service() {

    private val binder = LocalBinder()
    private val executor = Executors.newCachedThreadPool()
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver

    private val peers = mutableListOf<WifiP2pDevice>()
    private val PORT = 8988

    inner class LocalBinder : Binder() {
        fun getService(): WifiDirectService = this@WifiDirectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        setupReceiver()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        })

        // inicia servidor
        executor.submit { startServer() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        executor.shutdownNow()
    }

    private fun setupReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        manager.requestPeers(channel) { list ->
                            peers.clear()
                            peers.addAll(list.deviceList)
                            sendLocal("PEERS_UPDATED", peers.joinToString { it.deviceName })
                        }
                    }

                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            manager.requestConnectionInfo(channel) { info ->
                                if (!info.isGroupOwner && info.groupOwnerAddress != null) {
                                    executor.submit { connectToOwner(info.groupOwnerAddress.hostAddress) }
                                } else if (info.isGroupOwner) {
                                    sendLocal("STATUS", "Eres el HOST")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sendLocal("STATUS", "Buscando dispositivos…")
            }

            override fun onFailure(reason: Int) {
                sendLocal("STATUS", "Error al buscar ($reason)")
            }
        })
    }

    fun connectTo(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sendLocal("STATUS", "Conectando a ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                sendLocal("STATUS", "Fallo de conexión ($reason)")
            }
        })
    }

    private fun startServer() {
        try {
            val server = ServerSocket(PORT)
            Log.d("WiFiDirect", "Servidor escuchando en $PORT")
            while (true) {
                val client = server.accept()
                handleSocket(client)
            }
        } catch (e: Exception) {
            Log.e("WiFiDirect", "Error servidor: ${e.message}")
        }
    }

    private fun connectToOwner(host: String) {
        try {
            val socket = Socket(host, PORT)
            handleSocket(socket)
        } catch (e: Exception) {
            Log.e("WiFiDirect", "Error cliente: ${e.message}")
        }
    }

    private fun handleSocket(socket: Socket) {
        executor.submit {
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                var line: String?
                while (socket.isConnected && input.readLine().also { line = it } != null) {
                    sendLocal("MESSAGE_RECEIVED", line!!)
                }

                input.close()
                output.close()
                socket.close()
            } catch (e: Exception) {
                Log.e("WiFiDirect", "Socket cerrado: ${e.message}")
            }
        }
    }

    fun sendMessage(ip: String, message: String) {
        executor.submit {
            try {
                val socket = Socket(ip, PORT)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                writer.write(message + "\n")
                writer.flush()
                writer.close()
                socket.close()
                sendLocal("STATUS", "Mensaje enviado a $ip")
            } catch (e: Exception) {
                sendLocal("STATUS", "Error al enviar: ${e.message}")
            }
        }
    }

    private fun sendLocal(action: String, message: String) {
        val intent = Intent(action)
        intent.putExtra("data", message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun getPeers(): List<WifiP2pDevice> = peers
}
