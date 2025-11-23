package com.example.getjob.presentation.screens.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.presentation.viewmodel.ClientDashboardViewModel
import com.example.getjob.presentation.viewmodel.JobWithApplications
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.utils.PreferencesManager
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDashboardScreen(
    onNavigateToCreateJob: () -> Unit,
    onNavigateToJobDetail: (Int) -> Unit,
    onNavigateToChat: (Int, Int) -> Unit = { _, _ -> }, // jobId, applicationId
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ClientDashboardViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Refrescar cuando la pantalla se vuelve a mostrar (usando el lifecycle)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isScreenActive by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isScreenActive = true
                    viewModel.refreshJobs()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isScreenActive = false
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    
    // WebSocket para actualizaciones en tiempo real (solución profesional)
    // El polling ya no es necesario porque WebSocket actualiza automáticamente
    // Solo mantener un polling de respaldo cada 60 segundos por si el WebSocket falla
    LaunchedEffect(isScreenActive) {
        while (isScreenActive) {
            delay(60000) // 60 segundos - solo como respaldo si WebSocket falla
            
            // Solo refrescar si WebSocket no está conectado y la pantalla está activa
            if (isScreenActive && !uiState.isWebSocketConnected && !uiState.isLoading) {
                viewModel.refreshJobs()
            }
        }
    }
    
    // Función para cerrar sesión
    val logout: () -> Unit = {
        val preferencesManager = PreferencesManager(context)
        preferencesManager.clearAuthData()
        onLogout()
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
                            "Mis Trabajos",
                            color = RegisterColors.DarkGray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RegisterColors.White,
                    titleContentColor = RegisterColors.DarkGray
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = logout,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Cerrar sesión",
                                tint = RegisterColors.PrimaryOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            com.example.getjob.presentation.components.ClientBottomNavigationBar(
                onNavigateToDashboard = { /* Ya estamos aquí */ },
                onNavigateToCreateJob = onNavigateToCreateJob,
                onNavigateToProfile = onNavigateToProfile,
                currentRoute = "dashboard"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading && uiState.jobsWithApplications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.jobsWithApplications.isEmpty()) {
                item {
                    EmptyJobsState(onCreateJob = onNavigateToCreateJob)
                }
            } else {
                items(uiState.jobsWithApplications, key = { it.job.id }) { jobWithApps ->
                    JobWithApplicationsCard(
                        jobWithApps = jobWithApps,
                        onJobClick = { onNavigateToJobDetail(jobWithApps.job.id) },
                        onChatClick = { applicationId ->
                            onNavigateToChat(jobWithApps.job.id, applicationId)
                        }
                    )
                }
            }
            
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = RegisterColors.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Error: ${uiState.errorMessage}",
                            modifier = Modifier.padding(16.dp),
                            color = RegisterColors.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobWithApplicationsCard(
    jobWithApps: JobWithApplications,
    onJobClick: () -> Unit,
    onChatClick: (Int) -> Unit // applicationId
) {
    val job = jobWithApps.job
    val applications = jobWithApps.applications
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Información del trabajo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onJobClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.service_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.PrimaryBlue
                    )
                }
                StatusChip(status = job.status)
            }
            
            if (!job.description.isNullOrEmpty()) {
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = RegisterColors.PrimaryBlue
                )
                Text(
                    text = job.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.DarkGray
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "S/ ${job.total_amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.PrimaryOrange
                )
            }
            
            // Lista de aplicaciones (chats con trabajadores)
            if (applications.isNotEmpty()) {
                HorizontalDivider(
                    color = RegisterColors.BorderGray,
                    thickness = 1.dp
                )
                
                Text(
                    text = "Trabajadores que aplicaron (${applications.size})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    applications.forEach { application ->
                        ApplicationChatCard(
                            application = application,
                            onClick = { onChatClick(application.id) }
                        )
                    }
                }
            } else {
                HorizontalDivider(
                    color = RegisterColors.BorderGray,
                    thickness = 1.dp
                )
                Text(
                    text = "Aún no hay trabajadores que hayan aplicado",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.MediumGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ApplicationChatCard(
    application: JobApplicationResponse,
    onClick: () -> Unit,
    unreadCount: Int = 0 // TODO: Implementar contador de mensajes no leídos
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unreadCount > 0) {
                RegisterColors.PrimaryBlue.copy(alpha = 0.05f)
            } else {
                RegisterColors.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del trabajador
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(RegisterColors.PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (application.worker?.profile_image_url != null) {
                    // TODO: Cargar imagen con Coil
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = RegisterColors.PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = RegisterColors.PrimaryBlue,
                        modifier = Modifier.size(24.dp)
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
                        text = application.worker?.full_name ?: "Trabajador",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    if (application.worker?.is_verified == true) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verificado",
                            modifier = Modifier.size(16.dp),
                            tint = RegisterColors.PrimaryBlue
                        )
                    }
                    if (application.is_accepted) {
                        Surface(
                            color = RegisterColors.PrimaryOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aceptado",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = RegisterColors.PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Text(
                    text = "Toca para chatear",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.MediumGray
                )
            }
            
            // Indicador de mensajes nuevos
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RegisterColors.PrimaryOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = RegisterColors.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = RegisterColors.MediumGray
            )
        }
    }
}

@Composable
fun JobCard(
    job: JobResponse,
    onClick: () -> Unit,
    onChatClick: (Int) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = job.status)
            }
            
            if (!job.description.isNullOrEmpty()) {
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = job.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "S/ ${job.total_amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.PrimaryOrange
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.service_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Botón de chat
                    IconButton(
                        onClick = { onChatClick(job.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = RegisterColors.PrimaryOrange
                        )
                    }
                }
            }
        }
    }
}

private data class StatusInfo(
    val color: Color,
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color
)

@Composable
fun StatusChip(status: String) {
    val statusLower = status.lowercase()
    val statusInfo = when (statusLower) {
        "pending" -> StatusInfo(Color(0xFFFFA726), "Pendiente", Icons.Default.AccessTime, Color(0xFFFFA726))
        "accepted" -> StatusInfo(Color(0xFF42A5F5), "Aceptado", Icons.Default.CheckCircle, Color(0xFF42A5F5))
        "in_route" -> StatusInfo(Color(0xFF66BB6A), "En ruta", Icons.Default.Navigation, Color(0xFF66BB6A))
        "on_site" -> StatusInfo(Color(0xFF26A69A), "En sitio", Icons.Default.LocationOn, Color(0xFF26A69A))
        "in_progress" -> StatusInfo(Color(0xFFAB47BC), "En progreso", Icons.Default.Build, Color(0xFFAB47BC))
        "completed" -> StatusInfo(Color(0xFF66BB6A), "Completado", Icons.Default.CheckCircle, Color(0xFF66BB6A))
        "cancelled" -> StatusInfo(Color(0xFFEF5350), "Cancelado", Icons.Default.Cancel, Color(0xFFEF5350))
        else -> StatusInfo(Color.Gray, status, Icons.Default.Info, Color.Gray)
    }
    val (color, text, icon, iconColor) = statusInfo
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyJobsState(onCreateJob: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Work,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No tienes trabajos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Crea tu primera solicitud de servicio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onCreateJob,
            colors = ButtonDefaults.buttonColors(containerColor = RegisterColors.PrimaryOrange)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Trabajo")
        }
    }
}

// ClientBottomNavigationBar movido a BottomNavigationBar.kt para evitar duplicación

