package com.example.getjob.presentation.screens.service

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import kotlin.DeprecationLevel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.OSMMapView
import com.example.getjob.presentation.components.EnhancedOSMMapView
import com.example.getjob.presentation.components.LocationPermissionHandler
import com.example.getjob.utils.LocationService
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.presentation.viewmodel.JobDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.location.Location
import java.util.concurrent.TimeUnit

@Deprecated(
    message = "Usar JobFlowScreen en su lugar",
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceInProgressScreen(
    jobId: Int,
    jobTitle: String = "Fuga de agua en baño",
    clientName: String = "Ana",
    clientRating: Float = 4.8f,
    paymentMethod: String = "Yape",
    estimatedDuration: String = "45-60 min",
    onComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onPause: () -> Unit = {},
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
    var showAddExtraDialog by remember { mutableStateOf(false) }
    var extraAmountText by remember { mutableStateOf("") }
    var extraDescription by remember { mutableStateOf("") }
    
    // Cargar el job desde la base de datos
    LaunchedEffect(jobId) {
        if (jobId > 0) {
            viewModel.loadJob(jobId)
        }
    }
    
    // Obtener datos del job cargado - solo usar si se cargó exitosamente
    val job = uiState.job
    val hasValidJob = job != null && uiState.errorMessage == null && jobId > 0
    
    val actualJobTitle = if (hasValidJob) job?.title ?: jobTitle else jobTitle
    val actualClientName = if (hasValidJob) job?.client?.full_name ?: clientName else clientName
    val actualPaymentMethod = if (hasValidJob) {
        job?.payment_method?.let { 
            when (it.lowercase()) {
                "cash" -> "Efectivo"
                "yape" -> "Yape"
                else -> it
            }
        } ?: paymentMethod
    } else paymentMethod
    // IMPORTANTE: Usar base_fee de la BD solo si el job se cargó exitosamente
    val actualBaseFee = if (hasValidJob) job?.base_fee?.toDouble() ?: 80.00 else 80.00
    val actualExtras = if (hasValidJob) job?.extras?.toDouble() ?: 0.00 else 0.00
    val actualTotalAmount = if (hasValidJob) job?.total_amount?.toDouble() ?: 80.00 else 80.00
    val actualLatitude = if (hasValidJob) job?.latitude?.toDouble() else null
    val actualLongitude = if (hasValidJob) job?.longitude?.toDouble() else null
    val actualAddress = if (hasValidJob) job?.address ?: "" else ""
    
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
    
    // Timer del servicio
    var elapsedTime by remember { mutableStateOf(0L) } // en segundos
    var isRunning by remember { mutableStateOf(true) }
    
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedTime++
        }
    }
    
    // Formatear tiempo transcurrido
    val hours = TimeUnit.SECONDS.toHours(elapsedTime)
    val minutes = TimeUnit.SECONDS.toMinutes(elapsedTime) % 60
    val seconds = elapsedTime % 60
    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    
    // Calcular progreso (asumiendo 60 minutos estimados)
    val estimatedMinutes = 60
    val elapsedMinutes = TimeUnit.SECONDS.toMinutes(elapsedTime)
    // Usar 0.5f (50%) como valor fijo para la barra de progreso
    val progress = 0.5f
    
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
                            "Trabajo en curso",
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
            JobSummaryCardInProgress(
                jobTitle = actualJobTitle,
                clientName = actualClientName,
                clientRating = clientRating,
                paymentMethod = actualPaymentMethod,
                elapsedTime = timeString,
                estimatedTime = estimatedDuration,
                progress = progress
            )
            
            // Cost Summary Card
            CostSummaryCard(
                baseFee = actualBaseFee, // Usar base_fee de la base de datos
                extras = actualExtras, // Usar extras de la base de datos
                onAddExtra = { showAddExtraDialog = true },
                onViewDetail = { /* TODO: Implementar ver detalle */ }
            )
            
            // Diálogo para agregar extra
            if (showAddExtraDialog) {
                AlertDialog(
                    onDismissRequest = { showAddExtraDialog = false },
                    title = {
                        Text(
                            "Agregar Extra",
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = extraAmountText,
                                onValueChange = { 
                                    // Solo permitir números y punto decimal
                                    if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        extraAmountText = it
                                    }
                                },
                                label = { Text("Monto (S/)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = RegisterColors.IconGray
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = RegisterColors.BorderGray,
                                    focusedBorderColor = RegisterColors.PrimaryBlue
                                )
                            )
                            
                            OutlinedTextField(
                                value = extraDescription,
                                onValueChange = { extraDescription = it },
                                label = { Text("Descripción (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = RegisterColors.IconGray
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = RegisterColors.BorderGray,
                                    focusedBorderColor = RegisterColors.PrimaryBlue
                                )
                            )
                            
                            if (uiState.errorMessage != null) {
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = extraAmountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    val bigDecimalAmount = java.math.BigDecimal(amount.toString())
                                    viewModel.addExtra(
                                        jobId,
                                        bigDecimalAmount,
                                        extraDescription.takeIf { it.isNotBlank() }
                                    ) {
                                        showAddExtraDialog = false
                                        extraAmountText = ""
                                        extraDescription = ""
                                    }
                                }
                            },
                            enabled = extraAmountText.isNotBlank() && 
                                     (extraAmountText.toDoubleOrNull() ?: 0.0) > 0 &&
                                     !uiState.isStarting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RegisterColors.PrimaryOrange,
                                contentColor = RegisterColors.White
                            )
                        ) {
                            if (uiState.isStarting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = RegisterColors.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Agregar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAddExtraDialog = false
                                extraAmountText = ""
                                extraDescription = ""
                            }
                        ) {
                            Text("Cancelar", color = RegisterColors.DarkGray)
                        }
                    }
                )
            }
            
            // Evidence Card
            EvidenceCard(
                onImageClick = { /* TODO: Implementar selección de imagen */ }
            )
            
            // Notes Card
            NotesCard(
                onDescriptionChange = { /* TODO: Guardar descripción */ },
                onMaterialsChange = { /* TODO: Guardar materiales */ }
            )
            
            // Location Card con mapa
            if (actualLatitude != null && actualLongitude != null) {
                LocationCardInProgress(
                    address = actualAddress,
                    latitude = actualLatitude,
                    longitude = actualLongitude,
                    isWorker = isWorker,
                    workerLocation = workerLocation,
                    onGetDirections = {
                        // Abrir Google Maps
                        val uri = android.net.Uri.parse("google.navigation:q=$actualLatitude,$actualLongitude")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val geoUri = android.net.Uri.parse("geo:$actualLatitude,$actualLongitude?q=$actualLatitude,$actualLongitude")
                            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                            context.startActivity(fallbackIntent)
                        }
                    }
                )
            }
            
            // Action Buttons
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
                    Text(
                        "Mensajes", 
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                
                // Botón Pausar
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        onPause()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp), // Altura fija para consistencia
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = RegisterColors.White,
                        contentColor = RegisterColors.DarkGray
                    ),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                    Text(
                        "Pausar",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                
                // Botón Finalizar servicio
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp), // Altura fija para consistencia
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange,
                        contentColor = RegisterColors.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp) // Padding reducido
                ) {
                    Icon(
                        Icons.Default.Flag, // Icono de bandera
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // Espacio reducido
                    Text(
                        "Finalizar servicio",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
            
            // Texto informativo debajo de los botones
            Text(
                text = "Al finalizar, podrás confirmar pago (Yape o efectivo) y solicitar calificación del cliente.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun JobSummaryCardInProgress(
    jobTitle: String,
    clientName: String,
    clientRating: Float,
    paymentMethod: String,
    elapsedTime: String,
    estimatedTime: String,
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
            // Título del trabajo y Cliente/Calificación en la misma fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Título del trabajo
                Text(
                    text = jobTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                
                // Cliente y calificación en la esquina derecha (formato 8+ chars)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Cliente: nombre (formato similar a "8+ chars")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9) // Fondo gris claro - rgb(241, 245, 249)
                    ) {
                        Text(
                            text = "Cliente: $clientName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF), // Texto gris más claro - rgb(156, 163, 175)
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    
                    // Calificación con estrella
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700), // Amarillo para la estrella
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
            
            // Pago y En sitio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pago
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
                
                // En sitio
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
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RegisterColors.TextGray
                        )
                        Text(
                            text = "En sitio",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
            
            // Barra de progreso naranja (centrada)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = RegisterColors.PrimaryOrange,
                    trackColor = RegisterColors.BorderGray.copy(alpha = 0.3f)
                )
            }
            
            // Sección de tiempo transcurrido y estimado (dentro de la misma tarjeta, con borde)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RegisterColors.BorderGray.copy(alpha = 0.5f)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tiempo transcurrido
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = RegisterColors.PrimaryOrange
                        )
                        Column {
                            Text(
                                text = "Tiempo transcurrido",
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.TextGray
                            )
                            Text(
                                text = elapsedTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RegisterColors.DarkGray
                            )
                        }
                    }
                    
                    // Tiempo estimado
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Estimado",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                        Text(
                            text = estimatedTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
            }
            
            // Texto informativo (dentro de la misma tarjeta)
            Text(
                text = "Has iniciado el servicio. Mantén comunicación con el cliente y registra evidencia si es necesario.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CostSummaryCard(
    baseFee: Double, // base_fee de la base de datos
    extras: Double, // extras de la base de datos
    onAddExtra: () -> Unit,
    onViewDetail: () -> Unit
) {
    // Calcular total
    val total = baseFee + extras
    
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
                text = "Resumen de costos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            // Tarifa base y Extras en la misma fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tarifa base",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", baseFee)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Extras",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", extras)}",
                        style = MaterialTheme.typography.bodyLarge,
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
                    text = "S/ ${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            }
            
            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddExtra,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF9CA3AF)
                    ),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar extra", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF9CA3AF)
                    ),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver detalle", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun EvidenceCard(
    onImageClick: () -> Unit
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
            Text(
                text = "Evidencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable(onClick = onImageClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9) // Mismo fondo que "8+ chars" de LoginScreen.kt
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Agregar evidencia",
                                    modifier = Modifier.size(32.dp),
                                    tint = RegisterColors.TextGray
                                )
                            }
                        }
                    }
                }
            }
            
            // Texto descriptivo
            Text(
                text = "Sube fotos del antes y después para respaldo y garantía.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun NotesCard(
    onDescriptionChange: (String) -> Unit,
    onMaterialsChange: (String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    
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
                text = "Notas para el cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo: Describe lo realizado
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onDescriptionChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe lo realizado...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.GridOn, // Icono de 4 cuadrados (grid)
                            contentDescription = null,
                            tint = RegisterColors.TextGray
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegisterColors.PrimaryOrange,
                        unfocusedBorderColor = RegisterColors.BorderGray,
                        unfocusedContainerColor = RegisterColors.White,
                        focusedContainerColor = RegisterColors.White
                    )
                )
                
                // Campo: Materiales utilizados (opcional)
                OutlinedTextField(
                    value = materials,
                    onValueChange = {
                        materials = it
                        onMaterialsChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Materiales utilizados (opcional)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Inventory, // Icono de caja
                            contentDescription = null,
                            tint = RegisterColors.TextGray
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegisterColors.PrimaryOrange,
                        unfocusedBorderColor = RegisterColors.BorderGray,
                        unfocusedContainerColor = RegisterColors.White,
                        focusedContainerColor = RegisterColors.White
                    )
                )
            }
        }
    }
}


@Composable
fun LocationCardInProgress(
    address: String,
    latitude: Double,
    longitude: Double,
    isWorker: Boolean = false,
    workerLocation: Location? = null,
    onGetDirections: () -> Unit = {}
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
            Text(
                text = "Ubicación y contacto",
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
            
            // Dirección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dirección",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = RegisterColors.DarkGray
                    )
                }
                TextButton(
                    onClick = onGetDirections,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Ruta")
                }
            }
        }
    }
}
