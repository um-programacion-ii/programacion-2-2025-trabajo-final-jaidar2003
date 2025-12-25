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
        val eventRepo: EventoRepository = koinInject()
        val ventaRepo: VentaRepository = koinInject()

        val initialScreen = if (authRepository.isLoggedIn()) Screen.Home else Screen.Login
        var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
        val backstack = remember { mutableStateListOf<Screen>(initialScreen) }

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
                    onLoginSuccess = { 
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    },
                    onRegisterClick = { navigateTo(Screen.Register) },
                    authRepository = authRepository
                )
                is Screen.Register -> RegisterScreen(
                    onRegisterSuccess = { 
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    },
                    onBack = { goBack() },
                    authRepository = authRepository
                )
                is Screen.Home -> HomeScreen(
                    onEventClick = { navigateTo(Screen.EventDetail(it)) },
                    onHistoryClick = { navigateTo(Screen.History) },
                    onLogout = {
                        authRepository.logout()
                        backstack.clear()
                        backstack.add(Screen.Login)
                        currentScreen = Screen.Login
                    },
                    repo = eventRepo
                )
                is Screen.History -> HistoryScreen(
                    onBack = { goBack() },
                    ventaRepo = ventaRepo
                )
                is Screen.EventDetail -> EventDetailScreen(
                    event = screen.event,
                    onBack = { goBack() },
                    onConfirmSeats = { seats, sessionId ->
                        navigateTo(Screen.ConfirmPurchase(screen.event, seats, sessionId))
                    },
                    ventaRepo = ventaRepo
                )
                is Screen.ConfirmPurchase -> ConfirmPurchaseScreen(
                    event = screen.event,
                    seats = screen.selectedSeats,
                    sessionId = screen.sessionId,
                    onBack = { goBack() },
                    onSuccess = {
                        backstack.clear()
                        backstack.add(Screen.Home)
                        currentScreen = Screen.Home
                    },
                    ventaRepo = ventaRepo
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onRegisterClick: () -> Unit, authRepository: AuthRepository) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
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
        
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val result = authRepository.login(LoginRequest(username, password))
                        loading = false
                        if (result.isSuccess) {
                            onLoginSuccess()
                        } else {
                            error = "Login failed: ${result.exceptionOrNull()?.message}"
                        }
                    }
                },
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
        
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBack: () -> Unit, authRepository: AuthRepository) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nombreAlumno by remember { mutableStateOf("Juan Manuel Aidar") }
    var descripcionProyecto by remember { mutableStateOf("TF25") }
    
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombre") })
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellido") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = nombreAlumno, onValueChange = { nombreAlumno = it }, label = { Text("Nombre Alumno") })
        OutlinedTextField(value = descripcionProyecto, onValueChange = { descripcionProyecto = it }, label = { Text("Proyecto") })

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                loading = true
                error = null
                scope.launch {
                    val result = authRepository.register(
                        RegisterRequest(username, password, firstName, lastName, email, nombreAlumno, descripcionProyecto)
                    )
                    loading = false
                    if (result.isSuccess) {
                        onRegisterSuccess()
                    } else {
                        error = "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            }) {
                Text("Registrarse")
            }
            TextButton(onClick = onBack) {
                Text("Volver al Login")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
    }
}

@Composable
fun HomeScreen(onEventClick: (EventoResumido) -> Unit, onHistoryClick: () -> Unit, onLogout: () -> Unit, repo: EventoRepository) {
    var events by remember { mutableStateOf<List<EventoResumido>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            events = repo.getEventos()
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onHistoryClick) {
                Text("Mis Compras")
            }
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }
        Text("Eventos Disponibles", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        }

        if (error != null) {
            Text("Error: $error", color = Color.Red)
        }

        LazyColumn {
            items(events) { event ->
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
    event: EventoResumido,
    onBack: () -> Unit,
    onConfirmSeats: (List<String>, String) -> Unit,
    ventaRepo: VentaRepository
) {
    var asientos by remember { mutableStateOf<List<Asiento>>(emptyList()) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var selectedSeats by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(event.id) {
        try {
            val (ocupados, sesion) = ventaRepo.getAsientos(event.id)

            // Mapear ocupados por id para lookup rápido
            val ocupadosPorId = ocupados.associateBy { it.id }

            // Inferir tamaño de la sala desde los ids con formato r{fila}c{col}
            val regex = Regex("""r(\d+)c(\d+)""")
            var maxFila = 0
            var maxCol = 0
            for (a in ocupados) {
                val m = regex.matchEntire(a.id)
                if (m != null) {
                    val f = m.groupValues[1].toInt()
                    val c = m.groupValues[2].toInt()
                    if (f > maxFila) maxFila = f
                    if (c > maxCol) maxCol = c
                }
            }
            // Valores mínimos razonables si no hay datos suficientes
            if (maxFila < 9) maxFila = 9
            if (maxCol < 6) maxCol = 6

            // Reconstruir grilla completa: lo que no venga = LIBRE
            val todos = mutableListOf<Asiento>()
            for (fila in 1..maxFila) {
                for (col in 1..maxCol) {
                    val id = "r${fila}c${col}"
                    val existente = ocupadosPorId[id]
                    if (existente != null) {
                        todos += existente
                    } else {
                        todos += Asiento(
                            id = id,
                            estado = "LIBRE"
                        )
                    }
                }
            }

            asientos = todos
            sessionId = sesion
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text(event.nombre, style = MaterialTheme.typography.h4)
        Text(event.descripcion)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (error != null) {
            Text("Error: $error", color = Color.Red)
        } else {
            Text("Selecciona tus asientos:", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(columns = GridCells.Adaptive(80.dp)) {
                    items(asientos) { asiento ->
                        val isSelected = selectedSeats.contains(asiento.id)
                        val isOccupied = asiento.estado != "LIBRE"

                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable(enabled = !isOccupied) {
                                    if (isSelected) selectedSeats -= asiento.id
                                    else selectedSeats += asiento.id
                                },
                            backgroundColor = when {
                                isOccupied -> Color.Gray
                                isSelected -> Color.Green
                                else -> Color.LightGray
                            }
                        ) {
                            Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(asiento.id)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedSeats.isNotEmpty() && sessionId != null,
                onClick = {
                    scope.launch {
                        try {
                            ventaRepo.bloquearAsientos(event.externalId!!, sessionId!!, selectedSeats.toList())
                            onConfirmSeats(selectedSeats.toList(), sessionId!!)
                        } catch (e: Exception) {
                            error = "Error al bloquear: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Continuar con ${selectedSeats.size} asientos")
            }
        }
    }
}

@Composable
fun ConfirmPurchaseScreen(
    event: EventoResumido,
    seats: List<String>,
    sessionId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    ventaRepo: VentaRepository
) {
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Confirmar Compra", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Evento: ${event.nombre}")
        Text("Asientos: ${seats.joinToString()}")
        Text("Precio Unitario: $${event.precio}")
        Text("Total: $${event.precio * seats.size}", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email del comprador") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank(),
                onClick = {
                    loading = true
                    scope.launch {
                        try {
                            ventaRepo.confirmarVenta(sessionId, email)
                            onSuccess()
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text("Finalizar Compra")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, ventaRepo: VentaRepository) {
    var ventas by remember { mutableStateOf<List<org.example.project.model.Venta>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            ventas = ventaRepo.getVentas()
        } catch (e: Exception) {
            error = e.message ?: "Error al cargar historial"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Volver") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Mis Compras", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (error != null) {
            Text("Error: $error", color = Color.Red)
        } else if (ventas.isEmpty()) {
            Text("No tienes compras realizadas.")
        } else {
            LazyColumn {
                items(ventas) { venta ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = 4.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Compra #${venta.id}", style = MaterialTheme.typography.h6)
                            Text("Evento: ${venta.evento?.nombre ?: venta.externalEventId}")
                            Text("Asientos: ${venta.asientos.joinToString()}")
                            Text("Estado: ${venta.estado}")
                            Text("Email: ${venta.compradorEmail}")
                        }
                    }
                }
            }
        }
    }
}
