package com.example.getjob.presentation.screens.client

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.ClientRateWorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientRateWorkerScreen(
    jobId: Int,
    onNavigateBack: () -> Unit,
    onRatingSubmitted: () -> Unit,
    viewModel: ClientRateWorkerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var workerRating by remember { mutableStateOf(4) } // 1-5
    var reviewComment by remember { mutableStateOf("") }
    
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
    
    // Navegar cuando el rating sea exitoso
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            onRatingSubmitted()
        }
    }
    
    Scaffold(
        containerColor = RegisterColors.BackgroundColor,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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
                            "Calificar Trabajador",
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
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(RegisterColors.BackgroundColor)
                .padding(innerPadding)
        ) {
            val screenWidth = maxWidth
            val baseWidth = 360.dp
            val scaleFactor = screenWidth / baseWidth
            val horizontalPadding = (16.dp * scaleFactor).coerceIn(12.dp, 24.dp)
            val verticalSpacing = (12.dp * scaleFactor).coerceIn(8.dp, 20.dp)
            val cardBorderRadius = (32.dp * scaleFactor).coerceIn(28.dp, 36.dp)
            val maxContentWidth = 600.dp
            val contentWidth = if (screenWidth < maxContentWidth) screenWidth else maxContentWidth
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .width(contentWidth)
                        .fillMaxHeight()
                        .padding(horizontal = horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.job != null) {
                        uiState.job?.let { job ->
                            val workerName = job.worker?.full_name ?: "Trabajador"
                            val workerId = job.worker_id
                            
                            // Tarjeta principal - estilo LoginScreen
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(cardBorderRadius),
                                colors = CardDefaults.cardColors(
                                    containerColor = RegisterColors.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Título
                                Text(
                                    text = "¿Cómo fue tu experiencia?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RegisterColors.DarkGray
                                )
                                
                                // Información del trabajo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Work,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = RegisterColors.IconGray
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = job.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = RegisterColors.DarkGray
                                        )
                                        Text(
                                            text = "Trabajador: $workerName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RegisterColors.TextGray
                                        )
                                    }
                                }
                                
                                HorizontalDivider(color = RegisterColors.BorderGray)
                                
                                // Calificación con estrellas
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Calificación",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = RegisterColors.DarkGray
                                    )
                                    
                                    // Estrellas
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (i in 1..5) {
                                            IconButton(
                                                onClick = { workerRating = i },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    if (i <= workerRating) Icons.Default.Star else Icons.Outlined.Star,
                                                    contentDescription = "$i estrellas",
                                                    modifier = Modifier.size(32.dp),
                                                    tint = if (i <= workerRating) 
                                                        RegisterColors.PrimaryOrange 
                                                    else 
                                                        RegisterColors.BorderGray
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Texto de calificación
                                    Text(
                                        text = when (workerRating) {
                                            1 -> "Muy malo"
                                            2 -> "Malo"
                                            3 -> "Regular"
                                            4 -> "Bueno"
                                            5 -> "Excelente"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RegisterColors.TextGray
                                    )
                                }
                                
                                HorizontalDivider(color = RegisterColors.BorderGray)
                                
                                // Comentario opcional
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Comentario (opcional)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = RegisterColors.DarkGray
                                    )
                                    
                                    OutlinedTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(
                                                "Escribe tu opinión sobre el servicio...",
                                                color = RegisterColors.TextGray
                                            )
                                        },
                                        maxLines = 4,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = RegisterColors.IconGray
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = RegisterColors.BorderGray,
                                            focusedBorderColor = RegisterColors.PrimaryBlue
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                        
                            // Botón enviar
                            Button(
                                onClick = {
                                    viewModel.rateWorker(jobId, workerRating, reviewComment.takeIf { it.isNotBlank() })
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = workerRating > 0 && !uiState.isSubmitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RegisterColors.PrimaryOrange,
                                    contentColor = RegisterColors.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = RegisterColors.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enviar calificación")
                                }
                            }
                            
                            // Texto informativo
                            Text(
                                text = "Tu calificación ayuda a otros clientes a elegir el mejor trabajador.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.TextGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (uiState.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(cardBorderRadius),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.errorMessage ?: "Error al cargar el trabajo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

