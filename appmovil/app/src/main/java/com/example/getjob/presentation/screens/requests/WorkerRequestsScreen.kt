package com.example.getjob.presentation.screens.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.presentation.screens.dashboard.JobCard
import com.example.getjob.presentation.screens.dashboard.StatusChip
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.WorkerRequestsViewModel
import com.example.getjob.presentation.viewmodel.ApplicationWithJob

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToJobDetail: (Int, Int?) -> Unit,
    onNavigateToChat: (Int, Int) -> Unit = { _, _ -> }, // jobId, applicationId
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    viewModel: WorkerRequestsViewModel // 👈 sin default - debe pasarse desde NavGraph
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Inicializar carga solo una vez cuando el composable se crea
    // El ViewModel tiene un flag interno que previene cargas múltiples
    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
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
                            "Solicitudes",
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
                                contentDescription = "Volver",
                                tint = RegisterColors.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.refresh()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = RegisterColors.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RegisterColors.White,
                    titleContentColor = RegisterColors.DarkGray,
                    actionIconContentColor = RegisterColors.DarkGray
                )
            )
        }
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(RegisterColors.BackgroundColor)
                .padding(innerPadding),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 8.dp // Solo padding básico - la barra de navegación es el límite natural
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Si hay datos, siempre mostrarlos (incluso si está cargando en segundo plano)
            val hasData = uiState.applications.isNotEmpty() || uiState.jobs.isNotEmpty()
            
            if (hasData) {
                // Mostrar todas las aplicaciones primero
                items(uiState.applications, key = { it.application.id }) { applicationWithJob ->
                    ApplicationCard(
                        applicationWithJob = applicationWithJob,
                        onChatClick = {
                            onNavigateToChat(applicationWithJob.application.job_id, applicationWithJob.application.id)
                        },
                        onJobClick = {
                            onNavigateToJobDetail(applicationWithJob.application.job_id, applicationWithJob.application.id)
                        },
                        onRetryLoadJob = {
                            viewModel.retryLoadJob(applicationWithJob.application.id)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Luego mostrar todos los trabajos aceptados
                items(uiState.jobs, key = { it.id }) { job ->
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    JobCard(
                        job = job,
                        isExpanded = isExpanded,
                        isPlusActive = uiState.isPlusActive,
                        onExpandToggle = { isExpanded = !isExpanded },
                        onApplyClick = { 
                            // Usar -1 como applicationId cuando no hay aplicación asociada
                            // La ruta ya maneja esto correctamente
                            onNavigateToJobDetail(job.id, -1)
                        },
                        onPlusRequired = {
                            // Navegar a Modo Plus si se requiere
                            onNavigateToCommissions()
                        },
                        buttonText = "Seguir",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Solo mostrar loading si NO hay datos y está cargando
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    // Estado vacío si no hay nada y no está cargando
                    item {
                        EmptyStateCard(
                            title = "No tienes aplicaciones ni solicitudes",
                            subtitle = "Aplica a trabajos para verlos aquí"
                        )
                    }
                }
            }

            // Mostrar error general si existe
            uiState.errorMessage?.let { errorMessage ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Error al cargar datos",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.clearError()
                                    viewModel.refresh()
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationsFilterBar(
    applications: List<JobApplicationResponse>,
    selectedApplicationId: Int?,
    onFilterSelected: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Filtro",
            style = MaterialTheme.typography.labelLarge,
            color = RegisterColors.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow (
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedApplicationId == null,
                    onClick = { onFilterSelected(null) },
                    label = { Text("Todas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.15f),
                        selectedLabelColor = RegisterColors.PrimaryOrange,
                        labelColor = RegisterColors.DarkGray
                    )
                )
            }
            
            items(applications, key = { it.id }) { application ->
                val isSelected = selectedApplicationId == application.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onFilterSelected(if (isSelected) null else application.id)
                    },
                    label = {
                        Text(
                            text = "Trabajo #${application.job_id}",
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.15f),
                        selectedLabelColor = RegisterColors.PrimaryOrange,
                        labelColor = RegisterColors.DarkGray
                    )
                )
            }
        }
    }
}

@Composable
fun ApplicationCard(
    applicationWithJob: com.example.getjob.presentation.viewmodel.ApplicationWithJob,
    onChatClick: () -> Unit,
    onJobClick: () -> Unit,
    onRetryLoadJob: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = applicationWithJob.application
    val job = applicationWithJob.job
    val isLoadingJob = applicationWithJob.isLoadingJob
    val jobError = applicationWithJob.jobError
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoadingJob) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = RegisterColors.PrimaryOrange
                    )
                }
            } else if (job != null) {
                // Información del trabajo
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Manejo seguro de title (puede ser null según el modelo)
                            val jobTitle = job.title.takeIf { it.isNotBlank() } ?: "Trabajo #${job.id}"
                            Text(
                                text = jobTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RegisterColors.DarkGray
                            )
                            // Manejo seguro de service_type
                            val serviceType = job.service_type.takeIf { it.isNotBlank() } ?: ""
                            if (serviceType.isNotEmpty()) {
                                Text(
                                    text = serviceType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegisterColors.PrimaryBlue
                                )
                            }
                        }
                        // Badge de estado
                        Surface(
                            color = RegisterColors.PrimaryOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Aplicado",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = RegisterColors.PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Dirección - Manejo seguro de null
                    val address = job.address.takeIf { it.isNotBlank() } ?: "Sin dirección"
                    val shortAddress = address.split(",").firstOrNull()?.trim() ?: address
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RegisterColors.TextGray
                        )
                        Text(
                            text = shortAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                    }
                    
                    // Monto - Manejo seguro
                    Text(
                        text = "Total: S/ ${job.total_amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.PrimaryOrange
                    )
                }
                
                HorizontalDivider(color = RegisterColors.BorderGray)
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Chatear
                    Button(
                        onClick = onChatClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange,
                            contentColor = RegisterColors.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chatear", fontWeight = FontWeight.Bold)
                    }
                    
                    // Botón Ver Detalles
                    OutlinedButton(
                        onClick = onJobClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RegisterColors.PrimaryOrange
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            RegisterColors.PrimaryOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver Detalles", fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // Estado de error con opción de reintentar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = jobError ?: "Error al cargar información del trabajo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Ver Detalles (aunque falle la carga, puede intentar navegar)
                        OutlinedButton(
                            onClick = onJobClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RegisterColors.PrimaryOrange
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                RegisterColors.PrimaryOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ver Detalles", fontWeight = FontWeight.Medium)
                        }
                        // Botón Reintentar
                        Button(
                            onClick = onRetryLoadJob,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RegisterColors.PrimaryOrange,
                                contentColor = RegisterColors.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = RegisterColors.TextGray
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = RegisterColors.TextGray
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.TextGray
            )
        }
    }
}

