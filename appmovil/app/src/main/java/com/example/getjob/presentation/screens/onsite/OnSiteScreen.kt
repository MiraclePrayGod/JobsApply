package com.example.getjob.presentation.screens.onsite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.DeprecationLevel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.OSMMapView
import com.example.getjob.presentation.components.EnhancedOSMMapView
import com.example.getjob.presentation.components.LocationPermissionHandler
import com.example.getjob.utils.LocationService
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.presentation.viewmodel.JobDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.location.Location
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Deprecated(
    message = "Usar JobFlowScreen en su lugar",
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnSiteScreen(
    jobId: Int,
    jobTitle: String = "Fuga de agua en baño",
    clientName: String = "Ana",
    clientRating: Float = 4.8f,
    paymentMethod: String = "Yape",
    estimatedDuration: String = "45-60 min",
    latitude: Double? = null,
    longitude: Double? = null,
    address: String = "Calle Las Flores 123, San Borja",
    onStartService: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onReschedule: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    viewModel: JobDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locationService = remember { LocationService.getInstance(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val isWorker = preferencesManager.getUserRole() == "worker"
    var workerLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Cargar el job desde la base de datos
    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }
    
    // Obtener datos del job cargado o usar valores por defecto
    val job = uiState.job
    val actualJobTitle = job?.title ?: jobTitle
    val actualClientName = job?.client?.full_name ?: clientName
    val actualPaymentMethod = job?.payment_method?.let { 
        when (it.lowercase()) {
            "cash" -> "Efectivo"
            "yape" -> "Yape"
            else -> it
        }
    } ?: paymentMethod
    val actualTotalAmount = job?.total_amount?.toDouble() ?: 80.00
    val actualLatitude = job?.latitude?.toDouble() ?: latitude
    val actualLongitude = job?.longitude?.toDouble() ?: longitude
    val actualAddress = job?.address ?: address
    
    // Solicitar permisos y obtener ubicación si es trabajador
    if (isWorker && actualLatitude != null && actualLongitude != null) {
        val locationApi = remember { com.example.getjob.data.api.ApiClient.locationApi }
        
        LaunchedEffect(Unit) {
            hasLocationPermission = locationService.hasLocationPermission()
            if (hasLocationPermission && locationService.isGpsEnabled()) {
                coroutineScope.launch {
                    workerLocation = locationService.getCurrentLocation()
                    // Actualizar ubicación en backend
                    workerLocation?.let { location ->
                        try {
                            locationApi.updateJobLocation(
                                jobId = jobId,
                                request = com.example.getjob.data.api.LocationUpdateRequest(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    speed = location.speed
                                )
                            )
                        } catch (e: Exception) {
                            // Ignorar errores de red
                        }
                    }
                }
            } else {
                showPermissionDialog = true
            }
        }
        
        LocationPermissionHandler(
            onPermissionGranted = {
                hasLocationPermission = true
                showPermissionDialog = false
                coroutineScope.launch {
                    workerLocation = locationService.getCurrentLocation()
                    // Actualizar ubicación en backend
                    workerLocation?.let { location ->
                        try {
                            locationApi.updateJobLocation(
                                jobId = jobId,
                                request = com.example.getjob.data.api.LocationUpdateRequest(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    speed = location.speed
                                )
                            )
                        } catch (e: Exception) {
                            // Ignorar errores de red
                        }
                    }
                }
            },
            onPermissionDenied = {
                hasLocationPermission = false
            },
            showDialog = showPermissionDialog
        )
    }
    
    // Calcular tiempo de llegada (simulado: hace 2 minutos)
    val arrivalTime = remember { LocalTime.now().minusMinutes(2) }
    val arrivalText = "Llegaste hace 2 min"
    
    // Calcular tiempo estimado de finalización
    val startTime = LocalTime.now()
    val estimatedEndTime = startTime.plusMinutes(60) // 60 minutos desde ahora
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
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
                            "En sitio con el cliente",
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
                onProfileClick = onNavigateToProfile,
                onNavigateToRequests = {},
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToCommissions = onNavigateToCommissions,
                currentRoute = "requests"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RegisterColors.BackgroundColor)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job Summary Card
            JobSummaryCardOnSite(
                jobTitle = actualJobTitle,
                clientName = actualClientName,
                clientRating = clientRating,
                arrivalText = arrivalText,
                paymentMethod = actualPaymentMethod
            )
            
            // Service Confirmation Card - usar total_amount de la base de datos
            ServiceConfirmationCard(
                baseFee = actualTotalAmount,
                estimatedDuration = estimatedDuration
            )
            
            // Scheduling Card
            SchedulingCard(
                startTime = startTime.format(timeFormatter),
                estimatedEndTime = estimatedEndTime.format(timeFormatter),
                latitude = actualLatitude,
                longitude = actualLongitude,
                address = actualAddress,
                isWorker = isWorker,
                workerLocation = workerLocation,
                onGetDirections = {
                    // Abrir Google Maps
                    val lat = actualLatitude
                    val lng = actualLongitude
                    if (lat != null && lng != null) {
                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val geoUri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                            context.startActivity(fallbackIntent)
                        }
                    }
                }
            )
            
            // Action Buttons - Primera línea: Mensajes y Reprogramar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Mensajes
                OutlinedButton(
                    onClick = onNavigateToMessages,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = RegisterColors.White,
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
                    Text("Mensajes", style = MaterialTheme.typography.bodySmall)
                }
                
                // Botón Reprogramar
                OutlinedButton(
                    onClick = onReschedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = RegisterColors.White,
                        contentColor = RegisterColors.DarkGray
                    ),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reprogramar", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Segunda línea: Botón Iniciar trabajo
            Button(
                onClick = onStartService,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange,
                    contentColor = RegisterColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Iniciar trabajo", style = MaterialTheme.typography.bodySmall)
            }
            
            // Texto informativo
            Text(
                text = "Inicia el trabajo solo si el cliente está de acuerdo con la tarifa y el alcance.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun JobSummaryCardOnSite(
    jobTitle: String,
    clientName: String,
    clientRating: Float,
    arrivalText: String,
    paymentMethod: String
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
            // Título del trabajo
            Text(
                text = jobTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            // Cliente con foto y calificación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto de perfil circular
                Surface(
                    modifier = Modifier.size(40.dp),
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
                            modifier = Modifier.size(24.dp),
                            tint = RegisterColors.TextGray
                        )
                    }
                }
                
                // Cliente y calificación
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "Cliente: $clientName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9)
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
                                text = "$clientRating",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
            
            // Estado: Llegaste hace X min y Pago
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
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
                            text = arrivalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
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
                            text = "Pago: $paymentMethod",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
            
            // Barra de progreso naranja
            LinearProgressIndicator(
                progress = { 1.0f }, // 100% completado
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = RegisterColors.PrimaryOrange,
                trackColor = RegisterColors.BorderGray.copy(alpha = 0.3f)
            )
            
            // Texto informativo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = RegisterColors.BorderGray.copy(alpha = 0.3f),
                shadowElevation = 0.dp
            ) {
                Text(
                    text = "Has confirmado tu llegada. Revisa el problema con el cliente y confirma el inicio del trabajo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ServiceConfirmationCard(
    baseFee: Double,
    estimatedDuration: String
) {
    // Total a cobrar (sin comisión)
    
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
            Text(
                text = "Confirmación del servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            // Finanzas
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            text = "S/ ${String.format("%.2f", baseFee)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
                
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
                        text = "S/ ${String.format("%.2f", baseFee)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                }
            }
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            // Checklist
            Text(
                text = "Detalles del trabajo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            // Item 1: Cliente presente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Cliente presente y acceso confirmado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray
                )
            }
            
            // Item 2: Problema verificado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Problema verificado en el lugar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray
                )
            }
            
            // Item 3: Tiempo estimado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = RegisterColors.TextGray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Tiempo estimado: $estimatedDuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray
                )
            }
            
            // Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "Incluye repuestos",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "Garantía 7 días",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SchedulingCard(
    startTime: String,
    estimatedEndTime: String,
    latitude: Double?,
    longitude: Double?,
    address: String,
    isWorker: Boolean = false,
    workerLocation: Location? = null,
    onGetDirections: () -> Unit = {}
) {
    val context = LocalContext.current
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
            Text(
                text = "Programación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            // Tiempos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Inicio",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "Ahora ($startTime)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = RegisterColors.DarkGray
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Fin estimado",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = estimatedEndTime,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = RegisterColors.DarkGray
                    )
                }
            }
            
            // Mapa
            if (latitude != null && longitude != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Si es trabajador y tiene ubicación, usar mapa mejorado con ETA
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
                        // Mapa simple para clientes o si no hay ubicación del trabajador
                        OSMMapView(
                            latitude = latitude,
                            longitude = longitude,
                            address = address,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
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
            
            // Nota sobre ubicación
            Text(
                text = "Tu ubicación queda registrada para respaldo y seguridad.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray
            )
        }
    }
}

