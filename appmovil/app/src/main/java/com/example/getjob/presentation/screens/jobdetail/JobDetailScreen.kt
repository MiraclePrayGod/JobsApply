package com.example.getjob.presentation.screens.jobdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.getjob.presentation.viewmodel.JobDetailViewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.OSMMapView
import com.example.getjob.presentation.components.EnhancedOSMMapView
import com.example.getjob.presentation.components.LocationPermissionHandler
import com.example.getjob.ui.theme.OrangePrimary
import com.example.getjob.utils.LocationService
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.utils.GeocodingService
import kotlinx.coroutines.launch

// Función auxiliar para formatear fecha (similar a DashboardScreen)
fun formatCreatedDate(createdAt: String): String {
    return try {
        val dateTime = java.time.LocalDateTime.parse(
            createdAt.replace(" ", "T").substringBefore("."),
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val createdDate = dateTime.toLocalDate()
        
        when {
            createdDate == today -> "Ahora"
            createdDate == today.minusDays(1) -> "Ayer"
            else -> {
                val day = dateTime.dayOfMonth
                val month = dateTime.monthValue
                "${day}/${month}"
            }
        }
    } catch (e: Exception) {
        "Ahora"
    }
}

// Función auxiliar para formatear método de pago
fun formatPaymentMethod(paymentMethod: String): String {
    return when (paymentMethod.lowercase()) {
        "cash" -> "Efectivo"
        "yape" -> "Yape"
        "plin" -> "Plin"
        else -> paymentMethod
    }
}

@Composable
fun ServiceDetailsCard(job: com.example.getjob.data.models.responses.JobResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Igual que las tarjetas del login
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White // Fondo blanco como las tarjetas del login
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Igual que login
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Text(
                text = "Detalles del servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            // Información con icono - estilo igual que LoginScreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icono outlined al estilo de LoginScreen
                Icon(
                    Icons.Outlined.Info, // Icono outlined (solo contorno) como en LoginScreen
                    contentDescription = "Información",
                    modifier = Modifier.size(18.dp), // Tamaño similar a fieldIconSize en LoginScreen
                    tint = RegisterColors.IconGray // Plomo #94A3B8 - igual que LoginScreen
                )
                
                // Texto descriptivo
                Text(
                    text = job.description ?: "El cliente reportó ${job.title.lowercase()}. Revisar detalles del servicio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Chips/Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip "Urgente" (si el trabajo es urgente o tiene prioridad)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9), // Gris claro
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Urgente",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // Chip con el tipo de servicio (ej: "Baño")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9), // Gris claro
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = job.service_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // Chip "Apartamento" (o tipo de propiedad)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9), // Gris claro
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Apartamento", // TODO: Obtener tipo de propiedad del backend
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: Int,
    applicationId: Int? = null,
    onNavigateBack: () -> Unit,
    onStartService: () -> Unit,
    onChat: (Int, Int?) -> Unit = { _, _ -> },
    onNavigateToOnRoute: (Int) -> Unit = {}, // Callback para navegar a OnRouteScreen
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCompleteProfile: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    onJobCancelled: () -> Unit = {}, // Callback para cuando se cancela un trabajo (actualizar lista de solicitudes)
    onNavigateToRateWorker: (Int) -> Unit = {}, // Callback para navegar a pantalla de calificar trabajador
    onNavigateToClientDashboard: () -> Unit = {}, // Callback para navegar al dashboard del cliente
    onNavigateToClientProfile: () -> Unit = {}, // Callback para navegar al perfil del cliente
    onNavigateToCreateJob: () -> Unit = {}, // Callback para navegar a crear trabajo
    viewModel: JobDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val isWorker = preferencesManager.getUserRole() == "worker"
    val isClient = preferencesManager.getUserRole() == "client"
    val geocodingService = remember { GeocodingService() }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }
    
    // Redirigir automáticamente a la pantalla correcta según el estado del trabajo
    // Si el trabajo ya está en progreso (no es "pending" ni "accepted"), ir directamente a JobFlowScreen
    LaunchedEffect(uiState.job?.status) {
        val job = uiState.job
        if (job != null && !isClient) { // Solo para trabajadores
            val status = job.status.lowercase()
            // Redirigir a JobFlowScreen para todos los estados activos (excepto pending y accepted)
            when (status) {
                "in_route", "on_site", "in_progress", "completed", "reviewed" -> {
                    // Navegar directamente a JobFlowScreen que maneja todos estos estados
                    onNavigateToOnRoute(jobId)
                }
            }
        }
    }
    
    // Intentar obtener coordenadas si el trabajo no las tiene
    LaunchedEffect(uiState.job) {
        val job = uiState.job
        if (job != null && job.latitude == null && job.longitude == null && job.address.isNotBlank()) {
            // Intentar obtener coordenadas desde la dirección
            coroutineScope.launch {
                val coords = geocodingService.geocodeAddress(job.address)
                if (coords != null) {
                    // Las coordenadas se obtuvieron, pero no las podemos guardar aquí
                    // Solo las usamos para mostrar el mapa
                    // TODO: Opcionalmente, actualizar el trabajo en el backend con las coordenadas
                }
            }
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar Snackbar de error
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    Scaffold(
        containerColor = RegisterColors.BackgroundColor, // Fondo igual que login/register
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier.height(48.dp),
                title = { 
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (uiState.job?.status?.lowercase() == "pending" || uiState.job?.status?.lowercase() == "open") {
                                "Detalles de Solicitud"
                            } else {
                                "Trabajo aceptado"
                            },
                            color = RegisterColors.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Atrás",
                                tint = RegisterColors.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RegisterColors.White,
                    titleContentColor = RegisterColors.DarkGray,
                    navigationIconContentColor = RegisterColors.DarkGray
                )
            )
        },
        bottomBar = {
            // Mostrar barra de navegación solo para clientes (la barra de worker está centralizada en NavGraph)
            if (isClient) {
                com.example.getjob.presentation.components.ClientBottomNavigationBar(
                    onNavigateToDashboard = onNavigateToClientDashboard,
                    onNavigateToCreateJob = onNavigateToCreateJob,
                    onNavigateToProfile = onNavigateToClientProfile,
                    currentRoute = "dashboard"
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.job != null) {
            val context = LocalContext.current
            uiState.job?.let { job ->
                val preferencesManager = remember { PreferencesManager(context) }
                val isWorker = preferencesManager.getUserRole() == "worker"
                val isClient = !isWorker
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RegisterColors.BackgroundColor)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Tarjeta principal con toda la información - mostrar info según rol
                if (isClient) {
                    // Si es cliente, mostrar información del trabajador
                    ClientJobSummaryCard(
                        job = job,
                        worker = job.worker
                    )
                } else {
                    // Si es trabajador, mostrar información del cliente
                    JobSummaryCard(
                        job = job,
                        client = job.client
                    )
                }
                
                // Tarjeta de ubicación y contacto
                LocationCard(
                    address = job.address,
                    latitude = job.latitude?.toDouble(),
                    longitude = job.longitude?.toDouble(),
                    phoneNumber = job.client?.phone,
                    isWorker = isWorker,
                    onCall = {
                        val phone = job.client?.phone
                        if (phone != null) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        }
                    },
                    onChat = {
                        onChat(jobId, applicationId)
                    },
                    onGetDirections = {
                        // Abrir Google Maps o navegación con las coordenadas
                        val lat = job.latitude?.toDouble()
                        val lng = job.longitude?.toDouble()
                        if (lat != null && lng != null) {
                            val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                // Si no está instalado Google Maps, usar URI genérico
                                val geoUri = android.net.Uri.parse("geo:$lat,$lng?q=${job.address}")
                                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                                context.startActivity(fallbackIntent)
                            }
                        }
                    }
                )
                
                // Sección de detalles del servicio
                ServiceDetailsCard(job = job)
                
                uiState.errorMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // Botones de acción - diferentes según rol
                if (isClient) {
                    // Botones para CLIENTE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Si el trabajo está completado y no tiene rating del cliente, mostrar botón para calificar
                        if (job.status.lowercase() == "completed" && job.worker_id != null) {
                            Button(
                                onClick = {
                                    onNavigateToRateWorker(jobId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RegisterColors.PrimaryOrange,
                                    contentColor = RegisterColors.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Calificar Trabajador")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Botón Cancelar trabajo (solo si no está completado ni cancelado)
                            // El cliente puede cancelar en: pending, accepted, in_route, on_site, in_progress
                            val canCancel = job.status.lowercase() in listOf(
                                "pending", 
                                "accepted", 
                                "in_route", 
                                "on_site", 
                                "in_progress"
                            )
                            
                            if (canCancel) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.cancelJob(jobId) {
                                            onJobCancelled()
                                            // Si es cliente, navegar al dashboard del cliente
                                            if (isClient) {
                                                onNavigateToClientDashboard()
                                            } else {
                                                onNavigateBack()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp), // Altura fija para consistencia
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = RegisterColors.White,
                                        contentColor = RegisterColors.DarkGray
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        RegisterColors.BorderGray
                                    ),
                                    enabled = !uiState.isCancelling,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                                ) {
                                    if (uiState.isCancelling) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                                        Text(
                                            "Cancelar",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            
                            // Botón Chat
                            Button(
                                onClick = { onChat(jobId, applicationId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp), // Altura fija para consistencia
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RegisterColors.PrimaryBlue,
                                    contentColor = RegisterColors.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                                Text(
                                    "Chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Botones para TRABAJADOR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón Cancelar - cambia el trabajo a estado "pending" y actualiza la lista de solicitudes
                        OutlinedButton(
                            onClick = {
                                viewModel.cancelJob(jobId) {
                                    onJobCancelled() // Actualizar lista de solicitudes
                                    // Si es trabajador, navegar al dashboard
                                    if (isWorker) {
                                        onNavigateToDashboard()
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp), // Altura fija para consistencia
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RegisterColors.White,
                                contentColor = RegisterColors.DarkGray
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                RegisterColors.BorderGray
                            ),
                            enabled = !uiState.isCancelling && job.status.lowercase() in listOf("pending", "accepted"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                        ) {
                            if (uiState.isCancelling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                                Text(
                                    "Cancelar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // Botón Iniciar servicio - solo si está aceptado
                        if (job.status.lowercase() == "accepted") {
                            Button(
                                onClick = {
                                    viewModel.startRoute(jobId) {
                                        onNavigateToOnRoute(jobId)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp), // Altura fija para consistencia
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RegisterColors.PrimaryOrange,
                                    contentColor = RegisterColors.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !uiState.isStarting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                            ) {
                                if (uiState.isStarting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Work,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp) // Icono ligeramente más pequeño
                                    )
                                    Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                                    Text(
                                        "Iniciar servicio",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    
                    // Texto informativo (solo para trabajador)
                    if (job.status.lowercase() == "accepted") {
                        Text(
                            text = "Al iniciar, se compartirá tu ruta y tiempo estimado con el cliente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = uiState.errorMessage ?: "No se pudo cargar el trabajo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun JobSummaryCard(
    job: com.example.getjob.data.models.responses.JobResponse,
    client: com.example.getjob.data.models.responses.ClientInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Igual que las tarjetas del login
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White // Fondo blanco como las tarjetas del login
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Igual que login
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Título y cliente con calificación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Título - más pequeño
                Text(
                    text = job.title, // Datos de la base de datos
                    style = MaterialTheme.typography.titleLarge, // Reducido de titleLarge a titleMedium
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                
                // Cliente y calificación - en un solo contenedor estilo "8+ chars"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nombre del cliente con estilo "8+ chars"
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Igual que "8+ chars"
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Text(
                            text = "Cliente: ${client?.full_name?.takeIf { it.isNotBlank() } ?: "Sin nombre"}", // Solo nombre del cliente que creó el trabajo, no correo
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF), // Texto gris más claro - rgb(156, 163, 175)
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    // Calificación con estilo "8+ chars", estrella más sutil
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Igual que "8+ chars"
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF), // Color más sutil (gris) en lugar de amarillo brillante
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "4.8", // TODO: Agregar campo 'rating' a ClientInfo en la BD y usar client?.rating?.toString() ?: "4.8"
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF) // Texto gris más claro - rgb(156, 163, 175)
                            )
                        }
                    }
                }
            }
            
            // Información del cliente con foto de perfil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto de perfil circular
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = RegisterColors.BorderGray.copy(alpha = 0.3f)
                ) {
                    if (client != null) {
                        // TODO: Cargar imagen real del cliente desde la BD si está disponible
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = RegisterColors.TextGray
                            )
                        }
                    }
                }
                
                // Información: distancia, tiempo, método de pago - todos en el mismo nivel horizontal con estilo "8+ chars"
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distancia - estilo "8+ chars"
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Igual que "8+ chars"
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RegisterColors.TextGray
                            )
                            Text(
                                text = "1.2 km", // TODO: Calcular distancia real desde la ubicación del trabajador
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF) // Texto gris más claro
                            )
                        }
                    }
                    
                    // Tiempo - estilo "8+ chars"
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Igual que "8+ chars"
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RegisterColors.TextGray
                            )
                            Text(
                                text = formatCreatedDate(job.created_at), // Datos de la base de datos
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF) // Texto gris más claro
                            )
                        }
                    }
                    
                    // Método de pago - estilo "8+ chars"
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Igual que "8+ chars"
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RegisterColors.TextGray
                            )
                            Text(
                                text = formatPaymentMethod(job.payment_method), // Datos de la base de datos
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF) // Texto gris más claro
                            )
                        }
                    }
                }
            }
            
            // Línea separadora
            HorizontalDivider(
                color = RegisterColors.BorderGray,
                thickness = 1.dp
            )
            
            // Sección de finanzas
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarifa acordada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tarifa acordada",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                        Text(
                            text = "S/ ${job.total_amount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
                
                // Total a cobrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cobrarás directamente al cliente",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "S/ ${job.total_amount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                }
            }
            
            // Texto informativo sobre el pago
            Text(
                text = "El pago se realizará por ${formatPaymentMethod(job.payment_method)} al finalizar el servicio. Mantén comunicación por seguridad.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.DarkGray,
                modifier = Modifier.padding(12.dp),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ClientJobSummaryCard(
    job: com.example.getjob.data.models.responses.JobResponse,
    worker: com.example.getjob.data.models.responses.WorkerInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Igual que las tarjetas del login
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White // Fondo blanco como las tarjetas del login
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Igual que login
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Título y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Título
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                
                // Estado del trabajo
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                ) {
                    Text(
                        text = when (job.status.lowercase()) {
                            "pending" -> "Pendiente"
                            "accepted" -> "Aceptado"
                            "in_route" -> "En ruta"
                            "on_site" -> "En sitio"
                            "in_progress" -> "En progreso"
                            "completed" -> "Completado"
                            "cancelled" -> "Cancelado"
                            else -> job.status
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            
            // Información del trabajador (si está asignado)
            // Verificar si hay worker_id aunque worker sea null (puede ser problema de carga)
            if (worker != null || job.worker_id != null) {
                // Si worker es null pero worker_id existe, mostrar mensaje de carga
                if (worker == null && job.worker_id != null) {
                    Text(
                        text = "Cargando información del trabajador...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else if (worker != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foto de perfil circular del trabajador
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = RegisterColors.BorderGray.copy(alpha = 0.3f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = RegisterColors.TextGray
                            )
                        }
                    }
                    
                    // Información del trabajador
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = worker.full_name ?: "Trabajador",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = RegisterColors.DarkGray
                            )
                            if (worker.is_verified) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Verificado",
                                    modifier = Modifier.size(16.dp),
                                    tint = RegisterColors.PrimaryBlue
                                )
                            }
                        }
                        if (worker.phone != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = RegisterColors.TextGray
                                )
                                Text(
                                    text = worker.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegisterColors.TextGray
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(
                    color = RegisterColors.BorderGray,
                    thickness = 1.dp
                )
                }
            } else {
                // Si no hay trabajador asignado (ni worker_id ni worker)
                Text(
                    text = "Esperando trabajador...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(
                    color = RegisterColors.BorderGray,
                    thickness = 1.dp
                )
            }
            
            // Información del trabajo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Método de pago
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RegisterColors.TextGray
                        )
                        Text(
                            text = formatPaymentMethod(job.payment_method),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                
                // Fecha
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RegisterColors.TextGray
                        )
                        Text(
                            text = formatCreatedDate(job.created_at),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
            
            // Línea separadora
            HorizontalDivider(
                color = RegisterColors.BorderGray,
                thickness = 1.dp
            )
            
            // Sección de finanzas
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total a pagar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "S/ ${job.total_amount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.PrimaryOrange
                    )
                }
                
                if (job.extras.toDouble() > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tarifa base",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                        Text(
                            text = "S/ ${job.base_fee}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RegisterColors.DarkGray
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Extras",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                        Text(
                            text = "S/ ${job.extras}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientInfoCard(client: com.example.getjob.data.models.responses.ClientInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White // Fondo blanco igual que login/register
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Información del cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray // Gris oscuro igual que login/register
            )
            
            if (client != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = RegisterColors.PrimaryBlue // Azul igual que login/register
                    )
                    Column {
                        Text(
                            text = client.full_name ?: client.email,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = RegisterColors.DarkGray // Gris oscuro igual que login/register
                        )
                        Text(
                            text = "Cliente verificado",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray // Gris igual que login/register
                        )
                    }
                }
                
                if (client.phone != null) {
                    HorizontalDivider(color = RegisterColors.BorderGray)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = RegisterColors.TextGray, // Gris igual que login/register
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = client.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RegisterColors.DarkGray // Gris oscuro igual que login/register
                        )
                    }
                }
            } else {
                Text(
                    text = "Información del cliente no disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray // Gris igual que login/register
                )
            }
        }
    }
}

@Composable
fun LocationCard(
    address: String,
    latitude: Double? = null,
    longitude: Double? = null,
    phoneNumber: String? = null,
    isWorker: Boolean = false,
    onCall: () -> Unit = {},
    onChat: () -> Unit = {},
    onGetDirections: () -> Unit
) {
    val context = LocalContext.current
    val geocodingService = remember { GeocodingService() }

    // Función auxiliar para validar coordenadas
    fun isValidLatitude(lat: Double?): Boolean {
        return lat != null && lat != 0.0 && lat >= -90.0 && lat <= 90.0
    }
    
    fun isValidLongitude(lng: Double?): Boolean {
        return lng != null && lng != 0.0 && lng >= -180.0 && lng <= 180.0
    }

    var resolvedLatitude by remember { mutableStateOf<Double?>(null) }
    var resolvedLongitude by remember { mutableStateOf<Double?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Validar y resolver coordenadas
    LaunchedEffect(address, latitude, longitude) {
        android.util.Log.d("LocationCard", "=== INICIO VALIDACIÓN ===")
        android.util.Log.d("LocationCard", "Address: $address")
        android.util.Log.d("LocationCard", "Latitude recibida: $latitude")
        android.util.Log.d("LocationCard", "Longitude recibida: $longitude")
        
        // Primero verificar si las coordenadas existentes son válidas
        val latValid = isValidLatitude(latitude)
        val lngValid = isValidLongitude(longitude)
        android.util.Log.d("LocationCard", "Lat válida: $latValid, Lng válida: $lngValid")
        
        if (latValid && lngValid) {
            resolvedLatitude = latitude
            resolvedLongitude = longitude
            android.util.Log.d("LocationCard", "✓ Coordenadas válidas asignadas: lat=$latitude, lng=$longitude")
        } else if (address.isNotBlank()) {
            // Si no hay coordenadas válidas, intentar geocodificar
            android.util.Log.d("LocationCard", "→ Iniciando geocodificación para: $address")
            isGeocoding = true
            coroutineScope.launch {
                try {
                    val coords = geocodingService.geocodeAddress(address)
                    android.util.Log.d("LocationCard", "Geocodificación resultado: $coords")
                    if (coords != null) {
                        val latValidGeocoded = isValidLatitude(coords.first)
                        val lngValidGeocoded = isValidLongitude(coords.second)
                        android.util.Log.d("LocationCard", "Coordenadas geocodificadas - Lat válida: $latValidGeocoded, Lng válida: $lngValidGeocoded")
                        
                        if (latValidGeocoded && lngValidGeocoded) {
                            resolvedLatitude = coords.first
                            resolvedLongitude = coords.second
                            android.util.Log.d("LocationCard", "✓ Coordenadas geocodificadas asignadas: lat=${coords.first}, lng=${coords.second}")
                        } else {
                            android.util.Log.w("LocationCard", "✗ Coordenadas geocodificadas inválidas: lat=${coords.first}, lng=${coords.second}")
                        }
                    } else {
                        android.util.Log.w("LocationCard", "✗ Geocodificación retornó null")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocationCard", "✗ Error en geocodificación: ${e.message}", e)
                } finally {
                    isGeocoding = false
                    android.util.Log.d("LocationCard", "=== FIN VALIDACIÓN ===")
                }
            }
        } else {
            android.util.Log.w("LocationCard", "✗ No hay dirección para geocodificar")
        }
    }

    // TODO: Lógica de ubicación del trabajador se agregará después
    // Por ahora solo mostramos el mapa básico
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White // Fondo blanco igual que login/register
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título centrado
            Text(
                text = "Ubicación y contacto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Mapa real con OSMDroid (OpenStreetMap - gratuito)
                android.util.Log.d("LocationCard", "Renderizando mapa - resolvedLat: $resolvedLatitude, resolvedLng: $resolvedLongitude")
                val lat = resolvedLatitude
                val lng = resolvedLongitude
                if (lat != null && lng != null) {
                    android.util.Log.d("LocationCard", "✓ Mostrando mapa con coordenadas")
                    // Mapa simple - primero mostramos el mapa básico
                    OSMMapView(
                        latitude = lat,
                        longitude = lng,
                        address = address,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    android.util.Log.w("LocationCard", "✗ NO HAY COORDENADAS - Mostrando placeholder")
                    // Mapa placeholder si no hay coordenadas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(RegisterColors.BorderGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isGeocoding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = OrangePrimary
                                )
                                Text(
                                    text = "Obteniendo coordenadas...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegisterColors.DarkGray
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = RegisterColors.DarkGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Coordenadas no disponibles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegisterColors.DarkGray.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                // Botón para expandir mapa (solo si hay coordenadas)
                if (lat != null && lng != null) {
                    FloatingActionButton(
                        onClick = { isMapExpanded = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        containerColor = RegisterColors.White,
                        contentColor = RegisterColors.DarkGray
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expandir mapa",
                            tint = RegisterColors.DarkGray
                        )
                    }
                }
            }
            
            // Diálogo de mapa expandido (pantalla completa)
            val expandedLat = resolvedLatitude
            val expandedLng = resolvedLongitude
            if (isMapExpanded && expandedLat != null && expandedLng != null) {
                Dialog(
                    onDismissRequest = { isMapExpanded = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(RegisterColors.White)
                    ) {
                        // Mapa en pantalla completa
                        OSMMapView(
                            latitude = expandedLat,
                            longitude = expandedLng,
                            address = address,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Barra superior con título y botón cerrar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = RegisterColors.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ubicación",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RegisterColors.DarkGray
                                )
                                IconButton(onClick = { isMapExpanded = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = RegisterColors.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Sección de dirección (FUERA del Box del mapa)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Dirección",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Botones de acción: Llamar, Chat, Ruta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    // Botón Llamar
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RegisterColors.DarkGray
                        ),
                        border = BorderStroke(1.dp, RegisterColors.BorderGray)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Llamar", style = MaterialTheme.typography.bodySmall)
                    }

                    // Botón Chat
                    OutlinedButton(
                        onClick = onChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RegisterColors.DarkGray
                        ),
                        border = BorderStroke(1.dp, RegisterColors.BorderGray)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", style = MaterialTheme.typography.bodySmall)
                    }

                    // Botón Ruta
                    OutlinedButton(
                        onClick = onGetDirections,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RegisterColors.DarkGray
                        ),
                        border = BorderStroke(1.dp, RegisterColors.BorderGray)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ruta", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }



@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = RegisterColors.TextGray, // Gris igual que login/register
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray // Gris igual que login/register
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = RegisterColors.DarkGray // Gris oscuro igual que login/register
            )
        }
    }
}

private data class StatusInfo(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color
)

@Composable
fun StatusChip(status: String) {
    val statusLower = status.lowercase()
    val statusInfo = when (statusLower) {
        "open", "pending" -> StatusInfo(Color(0xFFF1F5F9), Color(0xFF9CA3AF), Icons.Default.AccessTime, Color(0xFF9CA3AF))
        "accepted" -> StatusInfo(Color(0xFFE3F2FD), Color(0xFF1976D2), Icons.Default.CheckCircle, Color(0xFF1976D2))
        "in_route" -> StatusInfo(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.Navigation, Color(0xFF2E7D32))
        "on_site" -> StatusInfo(Color(0xFFE0F2F1), Color(0xFF00695C), Icons.Default.LocationOn, Color(0xFF00695C))
        "in_progress" -> StatusInfo(Color(0xFFFFF3E0), Color(0xFFFF6B35), Icons.Default.Build, Color(0xFFFF6B35))
        "completed" -> StatusInfo(Color(0xFFE8F5E9), Color(0xFF4CAF50), Icons.Default.CheckCircle, Color(0xFF4CAF50))
        else -> StatusInfo(Color(0xFFF1F5F9), Color(0xFF9CA3AF), Icons.Default.Info, Color(0xFF9CA3AF))
    }
    val (backgroundColor, textColor, icon, iconColor) = statusInfo

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(32.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconColor
            )
            Text(
                text = when (statusLower) {
                    "open" -> "Disponible"
                    "pending" -> "Pendiente"
                    "accepted" -> "Aceptado"
                    "in_route" -> "En ruta"
                    "on_site" -> "En sitio"
                    "in_progress" -> "En progreso"
                    "completed" -> "Completado"
                    else -> status
                },
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    isProfileComplete: Boolean = true,
    onProfileClick: () -> Unit,
    onNavigateToRequests: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    currentRoute: String = "dashboard" // Ruta actual para determinar qué icono está activo
) {
    NavigationBar(
        containerColor = RegisterColors.White,
        contentColor = RegisterColors.DarkGray
    ) {
        // Inicio
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onNavigateToDashboard,
            icon = {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = "Inicio",
                        tint = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray
                    )
            },
            label = {
                Text(
                    "Inicio",
                    color = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RegisterColors.DarkGray,
                    selectedTextColor = RegisterColors.DarkGray,
                    unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent // Sin fondo de color
            )
        )
        // Solicitudes
        NavigationBarItem(
            selected = currentRoute == "requests",
            onClick = onNavigateToRequests,
            icon = {
                    Icon(
                        Icons.Outlined.Assignment,
                        contentDescription = "Solicitudes",
                        tint = if (currentRoute == "requests") RegisterColors.DarkGray else RegisterColors.TextGray
                    )
            },
            label = {
                Text(
                    "Solicitudes",
                    color = if (currentRoute == "requests") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent // Sin fondo de color
            )
        )
        // Perfil
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfileClick,
            icon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Perfil",
                        tint = if (isProfileComplete && currentRoute == "profile") {
                            RegisterColors.DarkGray
                        } else if (!isProfileComplete) {
                            RegisterColors.PrimaryOrange
                        } else {
                            RegisterColors.TextGray
                        }
                    )
            },
            label = {
                Text(
                    "Perfil",
                    color = if (isProfileComplete && currentRoute == "profile") {
                        RegisterColors.DarkGray
                    } else if (!isProfileComplete) {
                        RegisterColors.PrimaryOrange
                    } else {
                        RegisterColors.TextGray
                    }
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = if (!isProfileComplete) RegisterColors.PrimaryOrange else RegisterColors.TextGray,
                unselectedTextColor = if (!isProfileComplete) RegisterColors.PrimaryOrange else RegisterColors.TextGray,
                indicatorColor = Color.Transparent // Sin fondo de color
            )
        )
        // Comisiones
        NavigationBarItem(
            selected = currentRoute == "commissions",
            onClick = onNavigateToCommissions,
            icon = {
                    Icon(
                        Icons.Outlined.Percent,
                        contentDescription = "Comisiones",
                        tint = if (currentRoute == "commissions") RegisterColors.DarkGray else RegisterColors.TextGray
                    )
            },
            label = {
                Text(
                    "Comisiones",
                    color = if (currentRoute == "commissions") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent // Sin fondo de color
            )
        )
    }
}


