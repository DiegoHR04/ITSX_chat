package com.example.wifidirectchat

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var wifiService: WifiDirectService
    private var bound = false

    private lateinit var txtStatus: TextView
    private lateinit var txtChat: TextView
    private lateinit var etMessage: EditText

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            wifiService = (binder as WifiDirectService.LocalBinder).getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra("data") ?: return
            when (intent.action) {
                "STATUS" -> txtStatus.text = msg
                "MESSAGE_RECEIVED" -> txtChat.append("\n$ msg")
                "PEERS_UPDATED" -> {
                    Toast.makeText(this@MainActivity, "Dispositivos encontrados: $msg", Toast.LENGTH_LONG).show()
                    // Conéctate automáticamente al primero
                    wifiService.getPeers().firstOrNull()?.let { device ->
                        wifiService.connectTo(device)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        txtChat = findViewById(R.id.txtChat)
        etMessage = findViewById(R.id.etMessage)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnSend = findViewById<Button>(R.id.btnSend)

        checkPermissions()

        btnSearch.setOnClickListener {
            if (bound) wifiService.discoverPeers()
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty()) {
                // Enviar al último conectado (modificar si quieres lista)
                wifiService.getPeers().firstOrNull()?.deviceAddress?.let {
                    wifiService.sendMessage(it, msg)
                    txtChat.append("\nYo: $msg")
                    etMessage.text.clear()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, WifiDirectService::class.java)
        startService(intent)
        bindService(intent, conn, BIND_AUTO_CREATE)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
            IntentFilter().apply {
                addAction("STATUS")
                addAction("MESSAGE_RECEIVED")
                addAction("PEERS_UPDATED")
            })
    }

    override fun onStop() {
        super.onStop()
        if (bound) unbindService(conn)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun checkPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (perms.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 1001)
        }
    }
}
