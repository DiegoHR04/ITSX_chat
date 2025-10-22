package com.example.interfaces

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.* import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.interfaces.ui.theme.InterfacesTheme

// ESTRUCTURA Y COLORES
// Definición de Colores
val DarkGrayBar = Color(0xFF333333)
val LightGrayBackground = Color(0xFFF5F5F5)
val WhiteBackground = Color(0xFFFFFFFF)
val BlueButton = Color(0xFF4285F4)

// Definición de las pantallas para la navegación (El Mapa)
enum class Screen {
    CHATS,    // Menú Principal
    PROFILE,  // Pantalla de Perfil
    SETTINGS  // Pantalla de Configuración
}

class MainActivity : ComponentActivity() { // Clase principal
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InterfacesTheme {
                AppNavigator() // Inicia el navegador principal
            }
        }
    }
}

// NAVEGACIÓN (AppNavigator)
@Composable
fun AppNavigator() {
    // Guarda la pantalla actual. Inicia en CHATS.
    var currentScreen by remember { mutableStateOf(Screen.CHATS) }

    // Función simple que se pasa a los hijos para cambiar el estado.
    val navigateTo: (Screen) -> Unit = { screen -> currentScreen = screen }

    when (currentScreen) {
        // En ChatsScreen, le decimos a dónde ir al hacer click
        Screen.CHATS -> ChatsScreen(
            onProfileClick = { navigateTo(Screen.PROFILE) }, // Va a Perfil
            onSettingsClick = { navigateTo(Screen.SETTINGS) } // Va a Configuración
        )
        // En ProfileScreen y SettingsScreen, solo le decimos que al "volver" regrese a Chats
        Screen.PROFILE -> ProfileScreen(
            onBackClick = { navigateTo(Screen.CHATS) } // Vuelve a Chats
        )
        Screen.SETTINGS -> SettingsScreen(
            onBackClick = { navigateTo(Screen.CHATS) } // Vuelve a Chats
        )
    }
}

// PANTALLA PRINCIPAL: CHATS
@Composable
fun ChatsScreen(onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = { ChatsTopBar(onProfileClick, onSettingsClick) },
        containerColor = LightGrayBackground
    ) { paddingValues ->
        EmptyChatsContent(paddingValues)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTopBar(onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Chats") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkGrayBar,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        navigationIcon = {
            // Ícono de Perfil
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Filled.Person, contentDescription = "Perfil")
            }
        },
        actions = {
            // Ícono de Configuración
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Configuración")
            }
            // Ícono de Encontrar Dispositivos
            IconButton(onClick = { /* Acción para Encontrar Dispositivos */ }) {
                Text(
                    text = "📡",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Color.White
                )
            }
        }
    )
}

@Composable
fun EmptyChatsContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Aún no tienes chats", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Usa el ícono de 'Encontrar Dispositivos' para empezar.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

// SEGUNDA INTERFAZ: PANTALLA DE CONFIGURACIÓN
@Composable
fun SettingsScreen(onBackClick: () -> Unit) { // Pide la acción de volver
    Scaffold(
        topBar = { SettingsTopBar(onBackClick) },
        containerColor = LightGrayBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 8.dp)
        ) {
            Divider(modifier = Modifier.padding(start = 56.dp), color = Color.LightGray, thickness = 0.5.dp)
            SettingsItem(title = "Notificaciones", iconChar = "🔔", onClick = { /* ... */ })
            Divider(modifier = Modifier.padding(start = 56.dp), color = Color.LightGray, thickness = 0.5.dp)
            SettingsItem(title = "Privacidad", iconChar = "🔒", onClick = { /* ... */ })
            Divider(modifier = Modifier.padding(start = 56.dp), color = Color.LightGray, thickness = 0.5.dp)
            SettingsItem(title = "Acerca de la aplicación", iconChar = "ℹ️", onClick = { /* ... */ })
            Divider(modifier = Modifier.padding(start = 56.dp), color = Color.LightGray, thickness = 0.5.dp)
            SettingsItem(title = "Ayuda", iconChar = "❓", onClick = { /* ... */ })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onBackClick: () -> Unit) { // Recibe la acción de volver
    CenterAlignedTopAppBar(
        title = { Text("Configuración") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkGrayBar,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        navigationIcon = {
            // Ícono de Volver
            IconButton(onClick = onBackClick) {
                Text(text = "←", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp), color = Color.White)
            }
        }
    )
}

// lista de Configuración.
@Composable
fun SettingsItem(
    title: String,
    iconChar: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(WhiteBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = iconChar, fontSize = 20.sp, modifier = Modifier.size(24.dp).wrapContentSize(Alignment.Center))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = ">", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 4.dp), color = Color.Gray)
        }
        Divider(modifier = Modifier.padding(start = 56.dp), color = Color.LightGray, thickness = 0.5.dp)
    }
}

// TERCERA INTERFAZ: PANTALLA DE PERFIL
@Composable
fun ProfileScreen(onBackClick: () -> Unit) { // Pide la acción de volver
    Scaffold(
        topBar = { ProfileTopBar(onBackClick) },
        containerColor = LightGrayBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto de Perfil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(BlueButton, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(text = "👤", fontSize = 72.sp, color = Color.White) }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Juan Manuel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Estado: Disponible", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            // Botón "Editar Perfil"
            Button(
                onClick = { /* Acción para Editar Perfil */ },
                colors = ButtonDefaults.buttonColors(containerColor = BlueButton),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)
            ) { Text("Editar Perfil", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(32.dp))

            // Opciones de la Lista
            ProfileItem(title = "Historial de Conexiones", iconChar = "⌚", onClick = { /* ... */ })
            ProfileItem(title = "Información", iconChar = "ℹ️", onClick = { /* ... */ })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(onBackClick: () -> Unit) { // Recibe la acción de volver
    CenterAlignedTopAppBar(
        title = { Text("Mi perfil") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkGrayBar,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        navigationIcon = {
            // Ícono de Volver (Click llama a onBackClick, que cambia el estado a CHATS)
            IconButton(onClick = onBackClick) {
                Text(text = "←", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp), color = Color.White)
            }
        }
    )
}

// lista de Perfil.
@Composable
fun ProfileItem(title: String, iconChar: String, onClick: () -> Unit) {
    SettingsItem(title, iconChar, onClick) // Reutilizamos la misma estructura que SettingsItem
}