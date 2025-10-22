# Código de sincronización Bluetooth entre dispositivos de forma directa 📱
---

## 💡 Descripción General
Este proyecto implementa una pantalla básica en **Jetpack Compose** para el **escaneo, detección, y simulación de sincronización** directa de dispositivos Bluetooth. Está enfocado en encontrar y gestionar la conexión con otros **teléfonos inteligentes** para una comunicación directa (Peer-to-Peer).

---

## ✨ Funcionalidades Clave

* **Verificación de Bluetooth:** Verifica si el Bluetooth está encendido y, si no lo está, solicita al usuario activarlo o lo dirige a la configuración.
* **Muestra de Dispositivos Detectados:** Muestra una lista dinámica de todos los dispositivos Bluetooth detectados durante el escaneo.
* **Mensajes de Estado Claros:** Proporciona mensajes de estado explícitos para la experiencia del usuario:
    * “🔄 Sincronizando…”
    * “✅ Conectado correctamente”
    * “❌ Falló la sincronización”
* **Limpieza de Estados:** Al detener el escaneo, cancela la búsqueda de dispositivos y limpia el estado de conexión actual.
* **Compatibilidad con Android 12+ (S):** Manejo de permisos moderno (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`) a través de `ActivityResultContracts`.

### 2. FindDevicesScreen (Lógica de Escaneo y UI)

Este Composable contiene la mayor parte de la lógica de estado y control de Bluetooth.

#### **A. Gestión de Permisos al Iniciar**

El **`LaunchedEffect`** garantiza la solicitud correcta de permisos al cargar la pantalla, diferenciando los requerimientos de **Android 12 (API 31, versión S)** y anteriores para obtener acceso al escaneo y la conexión.

```kotlin
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
            // Permisos para versiones anteriores a Android 12
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

#### **B. Detección de Dispositivos (BroadcastReceiver)**

Se utiliza un **`BroadcastReceiver`** registrado dentro de un **`DisposableEffect`** para escuchar la acción **`BluetoothDevice.ACTION_FOUND`** del sistema. Este método es estándar para la detección de dispositivos.

Es crucial notar el **filtro** que solo añade a la lista aquellos dispositivos cuya clase de Bluetooth coincida con un **teléfono inteligente** (`BluetoothClass.Device.PHONE_...`), asegurando que solo se muestren los dispositivos relevantes para la comunicación *Peer-to-Peer*.

```kotlin
// Escucha de dispositivos detectados
    DisposableEffect(key1 = bluetoothAdapter) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // ... (lógica de obtención de dispositivo)
                    device?.let { foundDevice ->
                        // ✅ Filtro: solo teléfonos
                        val deviceClass = foundDevice.bluetoothClass?.deviceClass
                        if (deviceClass == BluetoothClass.Device.PHONE_SMART ||
                            deviceClass == BluetoothClass.Device.PHONE_CELLULAR ||
                            // ... (otros tipos)
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
            context.unregisterReceiver(receiver) // Se limpia el receptor al destruir el Composable
        }
    }

#### **C. Botón "Buscar teléfonos"**

Este botón es el punto de control para iniciar el escaneo. Comprueba si el **Bluetooth está encendido** y, si no lo está, lanza la `Intent` para solicitar su activación. Si ya está encendido y los permisos están dados, limpia la lista de resultados anteriores e inicia el descubrimiento.

```kotlin
        Button(onClick = {
            // ... (Verificación de soporte Bluetooth)

            if (!bluetoothAdapter.isEnabled) {
                try {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } catch (e: Exception) {
                    // Si falla el launcher, redirige directamente a la configuración
                    val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(settingsIntent)
                }
            } else {
                isScanning = true
                discoveredDevices.clear()
                bluetoothAdapter.startDiscovery() // Función clave para iniciar la búsqueda
            }
        }) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Buscar teléfonos")
        }

#### **D. Lista de Dispositivos y Sincronización (Simulada)**

La **`LazyColumn`** presenta los resultados del escaneo. La acción **`clickable`** en cada tarjeta simula un intento de sincronización. Esta lógica:

1.  Verifica si ya hay otro dispositivo conectado, mostrando una alerta para evitar múltiples conexiones.
2.  Si no hay conexión activa, inicia una **corrutina** (`scope.launch`) para simular el proceso de sincronización con un mensaje de estado dinámico y un tiempo de espera de 2 segundos.
3.  Determina el éxito o fracaso de la conexión con una condición booleana simulada (`Math.random() > 0.5`).

```kotlin
// 📋 Lista de teléfonos encontrados
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(discoveredDevices) { device ->
                Card(
                    // ...
                        .clickable {
                            if (connectedDevice != null && connectedDevice != device) {
                                // Alerta si ya hay uno conectado
                                showAlreadyConnectedAlert = true
                            } else {
                                scope.launch {
                                    connectionStatus = "🔄 Sincronizando con ${device.name ?: "Teléfono desconocido"}..."
                                    delay(2000) // Simulación de tiempo
                                    
                                    // Simulación de éxito
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

#### **E. Botones de Control**

Estos botones proporcionan la funcionalidad para detener el escaneo y para simular la desconexión manual del dispositivo emparejado.

| Botón | Función |
| :--- | :--- |
| **DETENER ESCANEO** | Detiene la búsqueda activa (`cancelDiscovery()`) y limpia los estados de escaneo y conexión. |
| **DESCONECTAR** | Simula la desconexión del dispositivo activo, limpiando la variable `connectedDevice`. |

```kotlin
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                isScanning = false
                bluetoothAdapter?.cancelDiscovery() // Detiene la búsqueda activa
                connectionStatus = null
            }) {
                Text("DETENER ESCANEO")
            }

            // Botón para "desconectarse" (solo visible si hay conexión)
            if (connectedDevice != null) {
                Button(onClick = {
                    connectionStatus = "🔌 Desconectado de ${connectedDevice?.name ?: "dispositivo"}"
                    connectedDevice = null
                }) {
                    Text("DESCONECTAR")
                }
            }
        }
