package com.example.getjob.presentation.screens.onroute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import com.example.getjob.presentation.viewmodel.JobDetailViewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.OSMMapView
import com.example.getjob.presentation.components.EnhancedOSMMapView
import com.example.getjob.presentation.components.LocationPermissionHandler
import com.example.getjob.utils.LocationService
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.utils.ProximityNotifier
import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.api.LocationUpdateRequest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.location.Location

@Deprecated(
    message = "Usar JobFlowScreen en su lugar",
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnRouteScreen(
    jobId: Int,
    onPause: () -> Unit,
    onConfirmArrival: () -> Unit,
    onNavigateBack: () -> Unit,
    onChat: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCompleteProfile: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    viewModel: JobDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var progress by remember { mutableStateOf(0.6f) } // Progreso del viaje (0.0 a 1.0)
    var isPaused by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val locationService = remember { LocationService.getInstance(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val proximityNotifier = remember { ProximityNotifier(context) }
    val locationApi = remember { ApiClient.locationApi }
    val isWorker = preferencesManager.getUserRole() == "worker"
    
    var workerLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var distanceRemaining by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }
    
    // Solicitar permisos y obtener ubicación si es trabajador
    if (isWorker) {
        // Función para iniciar actualización de ubicación
        val startLocationUpdates = {
            // Obtener ubicación inicial
            coroutineScope.launch {
                workerLocation = locationService.getCurrentLocation()
            }
            // Iniciar actualización continua
            locationService.startLocationUpdates(
                updateIntervalMillis = 10000L // Cada 10 segundos
            ) { location ->
                workerLocation = location
                // Calcular distancia si tenemos el destino
                uiState.job?.let { job ->
                    val lat = job.latitude?.toDouble()
                    val lng = job.longitude?.toDouble()
                    if (lat != null && lng != null) {
                        distanceRemaining = locationService.calculateDistance(
                            location.latitude,
                            location.longitude,
                            lat,
                            lng
                        )
                        
                        // Verificar proximidad y notificar
                        proximityNotifier.checkProximity(
                            location.latitude,
                            location.longitude,
                            lat,
                            lng
                        )
                        
                        // Guardar ubicación en backend
                        coroutineScope.launch {
                            try {
                                locationApi.updateJobLocation(
                                    jobId = jobId,
                                    request = LocationUpdateRequest(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        accuracy = location.accuracy,
                                        speed = location.speed
                                    )
                                )
                            } catch (e: Exception) {
                                // Loggear errores de red para debugging
                                android.util.Log.e("OnRouteScreen", "Error al actualizar ubicación del trabajo: ${e.message}", e)
                            }
                        }
                    }
                }
            }
        }
        
        // Verificar permisos primero
        LaunchedEffect(Unit) {
            hasLocationPermission = locationService.hasLocationPermission()
            if (hasLocationPermission && locationService.isGpsEnabled()) {
                startLocationUpdates()
            } else {
                showPermissionDialog = true
            }
        }
        
        LocationPermissionHandler(
            onPermissionGranted = {
                hasLocationPermission = true
                showPermissionDialog = false
                startLocationUpdates()
            },
            onPermissionDenied = {
                hasLocationPermission = false
            },
            showDialog = showPermissionDialog
        )
    }
    
    // Detener actualización cuando se desmonte
    DisposableEffect(Unit) {
        onDispose {
            if (isWorker) {
                locationService.stopLocationUpdates()
            }
        }
    }
    
    Scaffold(
        containerColor = RegisterColors.BackgroundColor,
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
                            "En ruta al cliente",
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
            com.example.getjob.presentation.components.WorkerBottomNavigationBar(
                isProfileComplete = true,
                onProfileClick = {
                    onNavigateToProfile()
                },
                onNavigateToRequests = onNavigateToRequests,
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToCommissions = onNavigateToCommissions,
                currentRoute = "requests"
            )
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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RegisterColors.BackgroundColor)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de resumen del trabajo con barra de progreso (sin montos)
                JobSummaryCardWithProgress(
                    job = job,
                    client = job.client,
                    progress = progress
                )
                
                // Tarjeta de ruta y llegada
                RouteAndArrivalCard(
                    address = job.address,
                    latitude = job.latitude?.toDouble(),
                    longitude = job.longitude?.toDouble(),
                    phoneNumber = job.client?.phone,
                    workerLocation = workerLocation,
                    distanceRemaining = distanceRemaining,
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
                        // TODO: Implementar chat - navegar a pantalla de chat
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
                
                // Tarjeta de resumen financiero
                PaymentSummaryCard(job = job)
                
                // Botones de acción: Pausar y Confirmar llegada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Pausar
                    OutlinedButton(
                        onClick = {
                            isPaused = !isPaused
                            onPause()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = RegisterColors.White,
                            contentColor = if (isPaused) RegisterColors.PrimaryOrange else RegisterColors.DarkGray
                        ),
                        border = BorderStroke(1.dp, RegisterColors.BorderGray)
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPaused) "Reanudar" else "Pausar")
                    }
                    
                    // Botón Confirmar llegada
                    Button(
                        onClick = onConfirmArrival,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange,
                            contentColor = RegisterColors.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar llegada")
                    }
                }
                
                // Texto informativo debajo de los botones
                Text(
                    text = "Si no puedes continuar, cancela o reprograma desde el chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
fun JobSummaryCardWithProgress(
    job: com.example.getjob.data.models.responses.JobResponse,
    client: com.example.getjob.data.models.responses.ClientInfo?,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                // Título
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleLarge,
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
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                    ) {
                        Text(
                            text = "Cliente: ${client?.full_name?.takeIf { it.isNotBlank() } ?: "Sin nombre"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    // Calificación con estilo "8+ chars", estrella más sutil
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "4.8",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
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
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = RegisterColors.BorderGray.copy(alpha = 0.3f)
                ) {
                    if (client != null) {
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
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
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
                                text = "1.2 km",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    
                    // Tiempo - estilo "8+ chars"
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
                                Icons.Default.AccessTime,
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
                    
                    // Método de pago - estilo "8+ chars"
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
                                Icons.Default.AccountBalanceWallet,
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
                }
            }
            
            // Barra de progreso con texto informativo
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Barra de progreso
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = RegisterColors.PrimaryOrange,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
                
                // Texto informativo
                Text(
                    text = "Has iniciado el servicio. Se compartió tu ruta y hora estimada con el cliente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Función auxiliar para formatear fecha (similar a JobDetailScreen)
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

@Composable
fun RouteAndArrivalCard(
    address: String,
    latitude: Double? = null,
    longitude: Double? = null,
    phoneNumber: String? = null,
    workerLocation: android.location.Location? = null,
    distanceRemaining: Float? = null,
    isWorker: Boolean = false,
    onCall: () -> Unit = {},
    onChat: () -> Unit = {},
    onGetDirections: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White
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
                text = "Ruta y llegada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            // Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Mapa real con OSMDroid
                if (latitude != null && longitude != null) {
                    // Si es trabajador y tiene ubicación, usar mapa mejorado con ruta
                    if (isWorker && workerLocation != null) {
                        EnhancedOSMMapView(
                            workerLatitude = workerLocation.latitude,
                            workerLongitude = workerLocation.longitude,
                            clientLatitude = latitude,
                            clientLongitude = longitude,
                            clientAddress = address,
                            showRoute = true,
                            showETA = true,
                            onStartNavigation = onGetDirections,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Mapa simple si no hay ubicación del trabajador
                        OSMMapView(
                            latitude = latitude,
                            longitude = longitude,
                            address = address,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
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
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = RegisterColors.TextGray
                            )
                            Text(
                                text = "Coordenadas no disponibles",
                                color = RegisterColors.TextGray
                            )
                        }
                    }
                }
            }
            
            // Mostrar distancia restante si está disponible
            if (isWorker && distanceRemaining != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RegisterColors.BorderGray.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = RegisterColors.PrimaryOrange
                            )
                            Text(
                                text = "Distancia restante:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RegisterColors.DarkGray
                            )
                        }
                        Text(
                            text = remember(distanceRemaining) {
                                distanceRemaining?.let { distance ->
                                    when {
                                        distance < 1000 -> "${distance.toInt()} m"
                                        else -> String.format("%.2f km", distance / 1000)
                                    }
                                } ?: "--"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.PrimaryOrange
                        )
                    }
                }
            }
            
            // Sección de dirección con botón "Navegando" en la misma fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Dirección",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                }
                // Botón "Navegando" en la misma fila que la dirección
                Surface(
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9), // Gris claro
                ) {
                    Text(
                        text = "Navegando",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
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
fun PaymentSummaryCard(
    job: com.example.getjob.data.models.responses.JobResponse
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
            // Título
            Text(
                text = "Resumen de trabajo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
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
            
            // Texto informativo con icono - estilo igual que LoginScreen
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
                    text = "Al llegar, confirma tu llegada para iniciar el trabajo en sitio. No cobres hasta finalizar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
            }
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

