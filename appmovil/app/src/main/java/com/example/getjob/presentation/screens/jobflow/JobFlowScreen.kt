package com.example.getjob.presentation.screens.jobflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.getjob.utils.responsiveHorizontalPadding
import com.example.getjob.utils.responsiveVerticalPadding
import com.example.getjob.utils.responsiveSpacing
import com.example.getjob.utils.responsiveIconSize
import com.example.getjob.utils.responsiveCardCornerRadius
import com.example.getjob.presentation.viewmodel.JobFlowViewModel
import com.example.getjob.presentation.viewmodel.JobStage
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.OSMMapView
import com.example.getjob.presentation.components.EnhancedOSMMapView
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.utils.GeocodingService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.location.Location
import androidx.compose.ui.unit.Dp
import java.util.concurrent.TimeUnit

// Función auxiliar para formatear fecha (compatible con API 24+)
fun formatCreatedDate(createdAt: String): String {
    return try {
        // Parsear fecha usando SimpleDateFormat (compatible con API 24+)
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = inputFormat.parse(createdAt.replace(" ", "T").substringBefore(".")) ?: return "Ahora"
        
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        
        val today = java.util.Calendar.getInstance()
        val yesterday = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        
        val createdCalendar = java.util.Calendar.getInstance().apply {
            time = date
        }
        
        when {
            createdCalendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            createdCalendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> "Ahora"
            createdCalendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
            createdCalendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> "Ayer"
            else -> {
                val day = createdCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                val month = createdCalendar.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH es 0-based
                "$day/$month"
            }
        }
    } catch (_: Exception) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFlowScreen(
    jobId: Int,
    viewModel: JobFlowViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val isWorker = preferencesManager.getUserRole() == "worker"
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }
    
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
    
    // Timer para IN_SERVICE - se reinicia cuando entra a IN_SERVICE
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartTime by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(uiState.stage) {
        when (uiState.stage) {
            JobStage.IN_SERVICE -> {
                // Si acabamos de entrar a IN_SERVICE, reiniciar el timer
                if (timerStartTime == null) {
                    timerStartTime = System.currentTimeMillis()
                    elapsedTime = 0L
                    isTimerRunning = true
                } else {
                    // Si ya estaba en IN_SERVICE, continuar desde donde estaba
                    isTimerRunning = true
                }
            }
            JobStage.COMPLETED, JobStage.REVIEWED -> {
                // Detener el timer cuando se completa
                isTimerRunning = false
            }
            else -> {
                // Pausar el timer en otros estados
                isTimerRunning = false
            }
        }
    }
    
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning && timerStartTime != null) {
            while (isTimerRunning) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                elapsedTime = (currentTime - timerStartTime!!) / 1000
            }
        }
    }
    
    val hours = TimeUnit.SECONDS.toHours(elapsedTime)
    val minutes = TimeUnit.SECONDS.toMinutes(elapsedTime) % 60
    val seconds = elapsedTime % 60
    val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    
    // Manejar botón de atrás del sistema - debe estar siempre activo
    BackHandler(enabled = true) {
        onNavigateBack()
    }
    
    // Si está cargando o no hay job, mostrar contenido mínimo con indicador
    val isLoading = uiState.isLoading || uiState.job == null
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val baseWidth = 360.dp
    val scaleFactor = (screenWidth / baseWidth).coerceIn(0.8f, 1.5f)
    val horizontalPadding = responsiveHorizontalPadding()
    val verticalPadding = responsiveVerticalPadding()
    val spacing = responsiveSpacing()
    val iconSize = responsiveIconSize()
    val cardRadius = responsiveCardCornerRadius()
    val maxContentWidth = if (screenWidth > 600.dp) 800.dp else Dp.Unspecified
    
    Scaffold(
        containerColor = RegisterColors.BackgroundColor,
        topBar = {
            if (isLoading) {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.height(48.dp),
                    title = { 
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cargando...", color = RegisterColors.DarkGray)
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
                                    "Atrás", 
                                    tint = RegisterColors.DarkGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RegisterColors.White,
                        titleContentColor = RegisterColors.DarkGray
                    )
                )
            } else {
                val currentStage = uiState.stage
                val title = when (currentStage) {
                    JobStage.ACCEPTED -> "Trabajo aceptado"
                    JobStage.ON_ROUTE -> "En ruta"
                    JobStage.ARRIVED -> "En sitio"
                    JobStage.IN_SERVICE -> "Trabajo en curso"
                    JobStage.COMPLETED -> "Servicio completado"
                    JobStage.REVIEWED -> "Trabajo finalizado"
                }
                TopAppBar(
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.height(48.dp),
                    title = {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
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
            }
        },
        bottomBar = {
            // Solo mostrar bottomBar en etapas activas (COMPLETED y REVIEWED tienen sus propios controles)
            if (!isLoading && uiState.stage in listOf(JobStage.ACCEPTED, JobStage.ON_ROUTE, JobStage.ARRIVED, JobStage.IN_SERVICE)) {
                JobFlowActionBar(
                    stage = uiState.stage,
                    onPrimaryAction = {
                        when (uiState.stage) {
                            JobStage.ACCEPTED -> viewModel.startRoute(jobId) { }
                            JobStage.ON_ROUTE -> viewModel.confirmArrival(jobId) { }
                            JobStage.ARRIVED -> viewModel.startService(jobId) { }
                            JobStage.IN_SERVICE -> viewModel.completeService(jobId) { }
                            else -> { /* No debería llegar aquí */ }
                        }
                    },
                    onChat = { onNavigateToChat(jobId) },
                    isPrimaryActionLoading = uiState.isStarting || uiState.isSubmitting
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                // Mostrar solo indicador de carga, sin bloquear la navegación
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RegisterColors.PrimaryOrange)
                }
            } else {
                val job = uiState.job!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (maxContentWidth != Dp.Unspecified) {
                                Modifier.widthIn(max = maxContentWidth)
                            } else {
                                Modifier
                            }
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // Card principal (se reutiliza SIEMPRE)
                    JobHeaderCard(job = job, iconSize = iconSize, cardRadius = cardRadius)
                    
                    // Mapa
                    JobMapSection(
                        job = job,
                        stage = uiState.stage,
                        isWorker = isWorker,
                        iconSize = iconSize,
                        cardRadius = cardRadius
                    )
                    
                    // Resumen de costos SIN comisión
                    CostSummarySection(
                        job = job,
                        isPlusActive = uiState.isPlusActive,
                        iconSize = iconSize,
                        cardRadius = cardRadius
                    )
                    
                    // Secciones según el stage
                    when (uiState.stage) {
                        JobStage.ACCEPTED -> AcceptedInfoSection(job = job, iconSize = iconSize, cardRadius = cardRadius)
                        JobStage.ON_ROUTE -> OnRouteInfoSection(cardRadius = cardRadius)
                        JobStage.ARRIVED -> ArrivedChecklistSection(job = job, iconSize = iconSize, cardRadius = cardRadius)
                        JobStage.IN_SERVICE -> {
                            // Mostrar timer en la card principal
                            InServiceInfoSection(
                                elapsedTime = timeString,
                                estimatedTime = "45-60 min",
                                iconSize = iconSize,
                                cardRadius = cardRadius
                            )
                            // Sección de extras con botón para agregar
                            ExtrasSection(
                                job = job,
                                onAddExtra = { amount: java.math.BigDecimal, description: String? ->
                                    viewModel.addExtra(jobId, amount, description) { }
                                },
                                isAdding = uiState.isStarting,
                                iconSize = iconSize,
                                cardRadius = cardRadius
                            )
                            EvidenceSection(
                                onImageClick = { /* TODO: Implementar selección de imagen */ },
                                iconSize = iconSize,
                                cardRadius = cardRadius
                            )
                            NotesSection(
                                onDescriptionChange = { /* TODO: Guardar descripción */ },
                                onMaterialsChange = { /* TODO: Guardar materiales */ },
                                iconSize = iconSize,
                                cardRadius = cardRadius
                            )
                        }
                        JobStage.COMPLETED -> PaymentAndRatingSection(
                            job = job,
                            onSubmitRating = { rating: Int, comment: String? ->
                                viewModel.submitRating(jobId, rating, comment) { }
                            },
                            isSubmitting = uiState.isSubmitting,
                            iconSize = iconSize,
                            cardRadius = cardRadius
                        )
                        JobStage.REVIEWED -> ReviewedSummarySection(
                            onGoHome = onNavigateToDashboard,
                            iconSize = iconSize,
                            cardRadius = cardRadius
                        )
                    }
                    
                    Spacer(Modifier.height(verticalPadding))
                }
            }
        }
    }
}

@Composable
fun JobHeaderCard(
    job: com.example.getjob.data.models.responses.JobResponse,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            // Título y cliente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "Cliente: ${job.client?.full_name?.takeIf { it.isNotBlank() } ?: "Sin nombre"}",
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
                            Icon(Icons.Default.Star, null, tint = RegisterColors.PrimaryOrange, modifier = Modifier.size(iconSize * 0.7f))
                            Text("4.8", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                        }
                    }
                }
            }
            
            // Foto de perfil e info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(iconSize * 2.3f), tint = RegisterColors.PrimaryBlue)
                
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE91E63).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(iconSize * 0.7f), tint = Color(0xFFE91E63))
                            Text("1.2 km", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE91E63), fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RegisterColors.PrimaryOrange.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, RegisterColors.PrimaryOrange.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(iconSize * 0.7f), tint = RegisterColors.PrimaryOrange)
                            Text(formatCreatedDate(job.created_at), style = MaterialTheme.typography.bodySmall, color = RegisterColors.PrimaryOrange, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RegisterColors.YapePurple.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, RegisterColors.YapePurple.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(iconSize * 0.7f), tint = RegisterColors.YapePurple)
                            Text(formatPaymentMethod(job.payment_method), style = MaterialTheme.typography.bodySmall, color = RegisterColors.YapePurple, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobMapSection(
    job: com.example.getjob.data.models.responses.JobResponse,
    stage: JobStage,
    isWorker: Boolean,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    val context = LocalContext.current
    val geocodingService = remember { GeocodingService() }
    val coroutineScope = rememberCoroutineScope()
    var resolvedLatitude by remember { mutableStateOf<Double?>(job.latitude?.toDouble()) }
    var resolvedLongitude by remember { mutableStateOf<Double?>(job.longitude?.toDouble()) }
    var workerLocation by remember { mutableStateOf<Location?>(null) }
    
    LaunchedEffect(job.address, job.latitude, job.longitude) {
        if (resolvedLatitude == null || resolvedLongitude == null) {
            if (job.address.isNotBlank()) {
                coroutineScope.launch {
                    val coords = geocodingService.geocodeAddress(job.address)
                    if (coords != null) {
                        resolvedLatitude = coords.first
                        resolvedLongitude = coords.second
                    }
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Ubicación y contacto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (resolvedLatitude != null && resolvedLongitude != null) {
                    if (isWorker && workerLocation != null && stage == JobStage.ON_ROUTE) {
                        EnhancedOSMMapView(
                            workerLatitude = workerLocation!!.latitude,
                            workerLongitude = workerLocation!!.longitude,
                            clientLatitude = resolvedLatitude!!,
                            clientLongitude = resolvedLongitude!!,
                            clientAddress = job.address,
                            showRoute = true,
                            showETA = true,
                            onStartNavigation = {
                                val uri = android.net.Uri.parse("google.navigation:q=${resolvedLatitude},${resolvedLongitude}")
                                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val geoUri = android.net.Uri.parse("geo:${resolvedLatitude},${resolvedLongitude}?q=${job.address}")
                                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        OSMMapView(
                            latitude = resolvedLatitude!!,
                            longitude = resolvedLongitude!!,
                            address = job.address,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
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
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(iconSize * 2f), tint = RegisterColors.PrimaryBlue)
                            Text("Coordenadas no disponibles", color = RegisterColors.TextGray)
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Dirección", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                Text(job.address, style = MaterialTheme.typography.bodyMedium, color = RegisterColors.DarkGray)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val phone = job.client?.phone
                        if (phone != null) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RegisterColors.DarkGray),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(iconSize * 0.75f), tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(4.dp))
                    Text("Llamar", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedButton(
                    onClick = {
                        val lat = resolvedLatitude
                        val lng = resolvedLongitude
                        if (lat != null && lng != null) {
                            val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                val geoUri = android.net.Uri.parse("geo:$lat,$lng?q=${job.address}")
                                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                                context.startActivity(fallbackIntent)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RegisterColors.DarkGray),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(iconSize * 0.75f), tint = RegisterColors.PrimaryBlue)
                    Spacer(Modifier.width(4.dp))
                    Text("Ruta", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CostSummarySection(
    job: com.example.getjob.data.models.responses.JobResponse,
    isPlusActive: Boolean,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    val baseFee = job.base_fee.toDouble()
    val extras = job.extras.toDouble()
    val total = job.total_amount.toDouble()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resumen de trabajo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tarifa base", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                Text("S/ ${String.format(java.util.Locale.getDefault(), "%.2f", baseFee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = RegisterColors.DarkGray)
            }
            
            if (extras > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Extras", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                    Text("S/ ${String.format(java.util.Locale.getDefault(), "%.2f", extras)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = RegisterColors.DarkGray)
                }
            }
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total a cobrar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RegisterColors.DarkGray)
                Text("S/ ${String.format(java.util.Locale.getDefault(), "%.2f", total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RegisterColors.PrimaryOrange)
            }
            
            if (isPlusActive) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(iconSize * 0.85f))
                        Text(
                            text = "Modo Plus activo: 0% comisión en este servicio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptedInfoSection(
    job: com.example.getjob.data.models.responses.JobResponse,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Detalles del servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Info, null, modifier = Modifier.size(iconSize * 0.75f), tint = RegisterColors.PrimaryBlue)
                Text(
                    text = job.description ?: "El cliente reportó ${job.title.lowercase()}. Revisar detalles del servicio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = "Urgente",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = job.service_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = "Apartamento",
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
fun OnRouteInfoSection(cardRadius: androidx.compose.ui.unit.Dp = 32.dp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            LinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = RegisterColors.PrimaryOrange,
                trackColor = Color.Gray.copy(alpha = 0.3f)
            )
            
            Text(
                text = "Has iniciado el servicio. Se compartió tu ruta y hora estimada con el cliente.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.DarkGray
            )
        }
    }
}

@Composable
fun ArrivedChecklistSection(
    job: com.example.getjob.data.models.responses.JobResponse,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Confirmación del servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tarifa acordada", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                    Text("S/ ${String.format(java.util.Locale.getDefault(), "%.2f", job.total_amount.toDouble())}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RegisterColors.DarkGray)
                }
            }
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            Text(
                text = "Detalles del trabajo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(iconSize))
                Text("Cliente presente y acceso confirmado", style = MaterialTheme.typography.bodyMedium, color = RegisterColors.DarkGray)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(iconSize))
                Text("Problema verificado en el lugar", style = MaterialTheme.typography.bodyMedium, color = RegisterColors.DarkGray)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CheckBoxOutlineBlank, null, tint = RegisterColors.PrimaryOrange, modifier = Modifier.size(iconSize))
                Text("Tiempo estimado: 45-60 min", style = MaterialTheme.typography.bodyMedium, color = RegisterColors.DarkGray)
            }
        }
    }
}

@Composable
fun InServiceInfoSection(
    elapsedTime: String,
    estimatedTime: String,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = RegisterColors.PrimaryOrange
                )
                Text(
                    text = "Trabajo en curso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, RegisterColors.PrimaryOrange.copy(alpha = 0.3f)),
                color = RegisterColors.PrimaryOrange.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = RegisterColors.PrimaryOrange
                        )
                        Column {
                            Text("Tiempo transcurrido", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                            Text(elapsedTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RegisterColors.DarkGray)
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimado", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                        Text(estimatedTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RegisterColors.DarkGray)
                    }
                }
            }
            
            Text(
                text = "Has iniciado el servicio. Mantén comunicación con el cliente y registra evidencia si es necesario.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExtrasSection(
    job: com.example.getjob.data.models.responses.JobResponse,
    onAddExtra: (java.math.BigDecimal, String?) -> Unit,
    isAdding: Boolean,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    var showAddExtraDialog by remember { mutableStateOf(false) }
    var extraAmountText by remember { mutableStateOf("") }
    var extraDescription by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
                
                OutlinedButton(
                    onClick = { showAddExtraDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF9CA3AF)
                    ),
                    border = BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(iconSize * 0.75f), tint = RegisterColors.PrimaryOrange)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar extra", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            if (job.extras.toDouble() > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total extras", style = MaterialTheme.typography.bodySmall, color = RegisterColors.TextGray)
                    Text("S/ ${String.format(java.util.Locale.getDefault(), "%.2f", job.extras.toDouble())}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = RegisterColors.DarkGray)
                }
            } else {
                Text(
                    text = "No hay extras agregados",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // Diálogo para agregar extra
    if (showAddExtraDialog) {
        AlertDialog(
            onDismissRequest = { showAddExtraDialog = false },
            title = {
                Text("Agregar Extra", fontWeight = FontWeight.Bold, color = RegisterColors.DarkGray)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = extraAmountText,
                        onValueChange = {
                            if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                extraAmountText = it
                            }
                        },
                        label = { Text("Monto (S/)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(iconSize * 0.75f))
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
                            Icon(Icons.Outlined.Description, null, tint = RegisterColors.PrimaryBlue, modifier = Modifier.size(iconSize * 0.75f))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = RegisterColors.BorderGray,
                            focusedBorderColor = RegisterColors.PrimaryBlue
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = extraAmountText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val bigDecimalAmount = java.math.BigDecimal(amount.toString())
                            onAddExtra(bigDecimalAmount, extraDescription.takeIf { it.isNotBlank() })
                            showAddExtraDialog = false
                            extraAmountText = ""
                            extraDescription = ""
                        }
                    },
                    enabled = extraAmountText.isNotBlank() && 
                             (extraAmountText.toDoubleOrNull() ?: 0.0) > 0 &&
                             !isAdding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange,
                        contentColor = RegisterColors.White
                    )
                ) {
                    if (isAdding) {
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
}

@Composable
fun EvidenceSection(
    onImageClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Evidencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            OutlinedButton(
                onClick = onImageClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = RegisterColors.DarkGray
                ),
                border = BorderStroke(1.dp, RegisterColors.BorderGray)
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(iconSize * 0.85f), tint = RegisterColors.PrimaryOrange)
                Spacer(Modifier.width(8.dp))
                Text("Agregar foto", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun NotesSection(
    onDescriptionChange: (String) -> Unit,
    onMaterialsChange: (String) -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    var description by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Notas para el cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onDescriptionChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe lo realizado...") },
                leadingIcon = {
                    Icon(Icons.Default.GridOn, null, tint = RegisterColors.PrimaryBlue, modifier = Modifier.size(iconSize * 0.75f))
                },
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegisterColors.PrimaryOrange,
                    unfocusedBorderColor = RegisterColors.BorderGray
                )
            )
            
            OutlinedTextField(
                value = materials,
                onValueChange = {
                    materials = it
                    onMaterialsChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Materiales utilizados (opcional)") },
                leadingIcon = {
                    Icon(Icons.Default.Inventory, null, tint = RegisterColors.PrimaryOrange, modifier = Modifier.size(iconSize * 0.75f))
                },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegisterColors.PrimaryOrange,
                    unfocusedBorderColor = RegisterColors.BorderGray
                )
            )
        }
    }
}

@Composable
fun PaymentAndRatingSection(
    job: com.example.getjob.data.models.responses.JobResponse,
    onSubmitRating: (Int, String?) -> Unit,
    isSubmitting: Boolean,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    var paymentMethod by remember { mutableStateOf("yape") }
    var paymentConfirmed by remember { mutableStateOf(false) }
    var clientRating by remember { mutableIntStateOf(4) }
    var reviewComment by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val total = job.total_amount.toDouble()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Text(
                text = "Método de pago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { paymentMethod = "yape" }
                    .then(
                        if (paymentMethod == "yape") {
                            Modifier.border(2.dp, RegisterColors.PrimaryOrange, RoundedCornerShape(30.dp))
                        } else {
                            Modifier.border(1.dp, RegisterColors.BorderGray, RoundedCornerShape(30.dp))
                        }
                    ),
                shape = RoundedCornerShape(30.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(responsiveVerticalPadding()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Smartphone, null, tint = RegisterColors.YapePurple, modifier = Modifier.size(iconSize))
                        Text("Yape", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = RegisterColors.DarkGray)
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
                        Text("Recomendado", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                    }
                }
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { paymentMethod = "cash" }
                    .then(
                        if (paymentMethod == "cash") {
                            Modifier.border(2.dp, RegisterColors.PrimaryOrange, RoundedCornerShape(30.dp))
                        } else {
                            Modifier.border(1.dp, RegisterColors.BorderGray, RoundedCornerShape(30.dp))
                        }
                    ),
                shape = RoundedCornerShape(30.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(responsiveVerticalPadding()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.AttachMoney, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(iconSize))
                    Text("Efectivo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = RegisterColors.DarkGray)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val formattedTotalForCopy = String.format(java.util.Locale.getDefault(), "%.2f", total)
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Monto a recibir", formattedTotalForCopy)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast
                                .makeText(context, "Monto copiado al portapapeles", android.widget.Toast.LENGTH_SHORT)
                                .show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(iconSize * 0.7f), tint = RegisterColors.PrimaryBlue)
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar monto", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { paymentConfirmed = !paymentConfirmed },
                    shape = RoundedCornerShape(16.dp),
                    color = if (paymentConfirmed) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(iconSize * 0.7f), tint = if (paymentConfirmed) Color(0xFF4CAF50) else Color(0xFF9CA3AF))
                        Spacer(Modifier.width(4.dp))
                        Text("Confirmar recibido", style = MaterialTheme.typography.bodySmall, color = if (paymentConfirmed) Color(0xFF4CAF50) else Color(0xFF9CA3AF))
                    }
                }
            }
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            Text(
                text = "Calificar cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val isSelected = i <= clientRating
                    if (i > 1) Spacer(Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { clientRating = i },
                        shape = CircleShape,
                        color = if (isSelected) RegisterColors.PrimaryOrange else Color(0xFFF1F5F9),
                        border = if (isSelected) BorderStroke(1.5.dp, RegisterColors.PrimaryOrange.copy(alpha = 0.7f)) else null
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Star, "$i estrellas", tint = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Outlined.StarBorder, "$i estrellas", tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            
            OutlinedTextField(
                value = reviewComment,
                onValueChange = { reviewComment = it },
                placeholder = { Text("Escribe un comentario (opcional)...", color = RegisterColors.TextGray) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegisterColors.PrimaryOrange,
                    unfocusedBorderColor = RegisterColors.BorderGray
                )
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Botón Finalizar y enviar
            Button(
                onClick = {
                    onSubmitRating(clientRating, reviewComment.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = paymentConfirmed && clientRating > 0 && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange,
                    contentColor = RegisterColors.White,
                    disabledContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.6f),
                    disabledContentColor = RegisterColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = RegisterColors.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isSubmitting) "Guardando..." else "Finalizar y enviar")
            }
            
            Text(
                text = "Al enviar, se cerrará la solicitud y se notificará al cliente.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ReviewedSummarySection(
    onGoHome: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cardRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsiveVerticalPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(iconSize * 2.7f),
                tint = Color(0xFF4CAF50)
            )
            
            Text(
                text = "Trabajo finalizado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            Text(
                text = "Gracias por completar el servicio. El cliente ha sido notificado.",
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Botón para volver al dashboard
            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange,
                    contentColor = RegisterColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al inicio")
            }
        }
    }
}

