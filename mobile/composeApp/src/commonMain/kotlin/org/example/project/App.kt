package org.example.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.data.EventoRepository
import org.example.project.data.VentaRepository
import org.example.project.model.Asiento
import org.example.project.model.EventoResumido
import org.example.project.model.LoginRequest
import org.example.project.model.RegisterRequest
import org.example.project.di.koinInject
import org.example.project.ui.*

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    object History : Screen()
    data class EventDetail(val event: EventoResumido) : Screen()
    data class ConfirmPurchase(val event: EventoResumido, val selectedSeats: List<String>, val sessionId: String) : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        val authRepository: AuthRepository = koinInject()

        val initialScreen = Screen.Login
        var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
        val backstack = remember { mutableStateListOf<Screen>(initialScreen) }

        // Asegurar que al inicio o reconexión se limpie el backstack y se vaya a Login
        LaunchedEffect(Unit) {
            if (currentScreen != Screen.Login) {
                backstack.clear()
                backstack.add(Screen.Login)
                currentScreen = Screen.Login
            }
        }

        fun navigateTo(screen: Screen) {
            backstack.add(screen)
            currentScreen = screen
        }

        fun goBack() {
            if (backstack.size > 1) {
                backstack.removeAt(backstack.size - 1)
                currentScreen = backstack.last()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Login -> LoginScreen(
                    viewModel = koinInject(),
                    onLoginSuccess = { 
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    },
                    onRegisterClick = { navigateTo(Screen.Register) }
                )
                is Screen.Register -> RegisterScreen(
                    viewModel = koinInject(),
                    onRegisterSuccess = { 
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    },
                    onBack = { goBack() }
                )
                is Screen.Home -> HomeScreen(
                    viewModel = koinInject(),
                    onEventClick = { navigateTo(Screen.EventDetail(it)) },
                    onHistoryClick = { navigateTo(Screen.History) },
                    onLogout = {
                        backstack.clear()
                        backstack.add(Screen.Login)
                        currentScreen = Screen.Login
                    }
                )
                is Screen.History -> HistoryScreen(
                    viewModel = koinInject(),
                    onBack = { goBack() }
                )
                is Screen.EventDetail -> EventDetailScreen(
                    viewModel = koinInject(),
                    event = screen.event,
                    onBack = { goBack() },
                    onConfirmSeats = { seats, sessionId ->
                        navigateTo(Screen.ConfirmPurchase(screen.event, seats, sessionId))
                    }
                )
                is Screen.ConfirmPurchase -> ConfirmPurchaseScreen(
                    viewModel = koinInject(),
                    event = screen.event,
                    seats = screen.selectedSeats,
                    sessionId = screen.sessionId,
                    onBack = { goBack() },
                    onNavigateToHome = {
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit, onRegisterClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.username, 
            onValueChange = { viewModel.username = it }, 
            label = { Text("Username") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = viewModel.password, 
            onValueChange = { viewModel.password = it }, 
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (viewModel.loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login(scope, onLoginSuccess) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿No tienes cuenta? Regístrate aquí", color = MaterialTheme.colors.primary)
            }
        }
        
        viewModel.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Red)
        }
    }
}

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onRegisterSuccess: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = viewModel.username, onValueChange = { viewModel.username = it }, label = { Text("Usuario") })
        OutlinedTextField(value = viewModel.password, onValueChange = { viewModel.password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = viewModel.firstName, onValueChange = { viewModel.firstName = it }, label = { Text("Nombre") })
        OutlinedTextField(value = viewModel.lastName, onValueChange = { viewModel.lastName = it }, label = { Text("Apellido") })
        OutlinedTextField(value = viewModel.email, onValueChange = { viewModel.email = it }, label = { Text("Email") })
        OutlinedTextField(value = viewModel.nombreAlumno, onValueChange = { viewModel.nombreAlumno = it }, label = { Text("Nombre Alumno") })
        OutlinedTextField(value = viewModel.descripcionProyecto, onValueChange = { viewModel.descripcionProyecto = it }, label = { Text("Proyecto") })

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.register(scope, onRegisterSuccess) }) {
                Text("Registrarse")
            }
            TextButton(onClick = onBack) {
                Text("Volver al Login")
            }
        }

        viewModel.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Red)
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel, onEventClick: (EventoResumido) -> Unit, onHistoryClick: () -> Unit, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.loadEvents(scope)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onHistoryClick) {
                Text("Mis Compras")
            }
            Button(onClick = {
                viewModel.logout()
                onLogout()
            }) {
                Text("Logout")
            }
        }
        Text("Eventos Disponibles", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator()
        }

        viewModel.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        LazyColumn {
            items(viewModel.events) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onEventClick(event) },
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(event.nombre, style = MaterialTheme.typography.h6)
                        Text(event.descripcion, style = MaterialTheme.typography.body2)
                        Text("Precio: ${event.precio}", style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    event: EventoResumido,
    onBack: () -> Unit,
    onConfirmSeats: (List<String>, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(event.id) {
        viewModel.loadAsientos(scope, event.id)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text(event.nombre, style = MaterialTheme.typography.h4)
        Text(event.descripcion)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (viewModel.error != null) {
            Text("Error: ${viewModel.error}", color = Color.Red)
        } else {
            Text("Selecciona tus asientos (máximo 4):", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            // Leyenda de colores
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Libre", Color(0xFF4CAF50))
                LegendItem("Seleccionado", Color(0xFF2196F3))
                LegendItem("Bloqueado", Color(0xFFFFC107))
                LegendItem("Vendido", Color.Red)
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(columns = GridCells.Adaptive(80.dp)) {
                    items(viewModel.asientos) { asiento ->
                        val isSelected = viewModel.selectedSeats.contains(asiento.id)
                        val isOccupied = asiento.estado == "Vendido" || asiento.estado == "Bloqueado"

                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable(enabled = asiento.estado == "LIBRE") {
                                    viewModel.toggleSeat(asiento.id)
                                },
                            backgroundColor = when {
                                isSelected -> Color(0xFF2196F3) // Azul (Seleccionado)
                                asiento.estado == "Vendido" -> Color.Red // Rojo (Vendido)
                                asiento.estado == "Bloqueado" -> Color(0xFFFFC107) // Amarillo (Bloqueado)
                                asiento.estado == "LIBRE" -> Color(0xFF4CAF50) // Verde (Libre)
                                else -> Color.Gray
                            }
                        ) {
                            Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(asiento.id, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.selectedSeats.isNotEmpty() && viewModel.sessionId != null,
                onClick = {
                    viewModel.confirmSelectedSeats(scope, event.externalId ?: "", onConfirmSeats)
                }
            ) {
                Text("Continuar con ${viewModel.selectedSeats.size} asientos")
            }
        }
    }
}

@Composable
fun ConfirmPurchaseScreen(
    viewModel: ConfirmPurchaseViewModel,
    event: EventoResumido,
    seats: List<String>,
    sessionId: String,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Confirmar Compra", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Evento: ${event.nombre}")
        Text("Asientos: ${seats.joinToString()}")
        Text("Precio Unitario: $${event.precio}")
        Text("Total: $${event.precio * seats.size}", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Datos de los ocupantes:", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        seats.forEach { seatId ->
            Text("Asiento $seatId", style = MaterialTheme.typography.subtitle2)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.occupantNames[seatId] ?: "",
                    onValueChange = { viewModel.occupantNames[seatId] = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                OutlinedTextField(
                    value = viewModel.occupantLastNames[seatId] ?: "",
                    onValueChange = { viewModel.occupantLastNames[seatId] = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val canConfirm = seats.all { 
                (viewModel.occupantNames[it]?.isNotBlank() ?: false) && 
                (viewModel.occupantLastNames[it]?.isNotBlank() ?: false)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canConfirm,
                onClick = { showConfirmDialog = true }
            ) {
                Text("Finalizar Compra")
            }
        }

        if (showConfirmDialog) {
            androidx.compose.material.AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirmar Compra") },
                text = { Text("¿Estás seguro de que deseas comprar ${seats.size} asiento(s) para el evento ${event.nombre}?") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        viewModel.confirmPurchase(scope, sessionId, seats, onSuccess = {
                            showSuccessDialog = true
                        })
                    }) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showSuccessDialog) {
            androidx.compose.material.AlertDialog(
                onDismissRequest = { },
                title = { Text("¡Venta Exitosa!") },
                text = { Text("Tu compra se ha realizado correctamente. Puedes verla en la sección 'Mis Compras'.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSuccessDialog = false
                        onNavigateToHome()
                    }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        viewModel.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Red)
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.caption)
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.loadHistory(scope)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Mis Compras", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (viewModel.error != null) {
            Text("Error: ${viewModel.error}", color = Color.Red)
        } else if (viewModel.ventas.isEmpty()) {
            Text("No tienes compras realizadas.")
        } else {
            LazyColumn {
                items(viewModel.ventas) { venta ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = 4.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Compra #${venta.id}", style = MaterialTheme.typography.h6)
                            Text("Evento: ${venta.eventoNombre ?: venta.evento?.nombre ?: venta.externalEventId}")
                            Text("Asientos: ${venta.asientos.joinToString()}")
                            Text("Ocupantes: ${venta.ocupantes.joinToString()}")
                            Text("Estado: ${venta.estado ?: ""}")
                            Text("Email: ${venta.compradorEmail ?: ""}")
                        }
                    }
                }
            }
        }
    }
}
