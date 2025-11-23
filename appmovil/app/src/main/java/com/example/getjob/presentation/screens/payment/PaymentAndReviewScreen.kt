package com.example.getjob.presentation.screens.payment

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.DeprecationLevel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.getjob.presentation.viewmodel.PaymentAndReviewViewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Deprecated(
    message = "Usar JobFlowScreen en su lugar",
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAndReviewScreen(
    jobId: Int,
    onContact: () -> Unit,
    onFinish: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCompleteProfile: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    viewModel: PaymentAndReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.job != null) {
            uiState.errorMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }
    
    var paymentMethod by remember { mutableStateOf("yape") } // "yape" or "cash" - por defecto Yape
    var paymentConfirmed by remember { mutableStateOf(false) }
    var clientRating by remember { mutableStateOf(4) } // 1-5
    var reviewComment by remember { mutableStateOf("") }

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    // Obtener datos del trabajo
    val job = uiState.job
    val jobTitle = job?.title ?: ""
    val clientName = job?.client?.full_name?.takeIf { it.isNotBlank() } ?: job?.client?.email ?: ""
    val baseFee = job?.base_fee?.toString() ?: "0.00"
    val extras = job?.extras?.toString() ?: "0.00"
    val jobStatus = job?.status?.lowercase() ?: ""
    
    val isCompleted = jobStatus == "completed"
    
    // Calcular total_amount desde base_fee + extras (según backend: total_amount = base_fee + extras)
    val baseFeeDouble = job?.base_fee?.toDouble() ?: 0.0
    val extrasDouble = job?.extras?.toDouble() ?: 0.0
    val calculatedTotal = baseFeeDouble + extrasDouble
    val totalAmount = String.format("%.2f", calculatedTotal)

    // Total a cobrar (sin comisión)

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
                            "Confirmar pago y reseña",
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
        } else if (uiState.errorMessage != null && job == null) {
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
                        text = uiState.errorMessage ?: "Error al cargar el trabajo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else if (job != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RegisterColors.BackgroundColor)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Resumen del trabajo
                JobSummaryCard(
                    jobTitle = jobTitle,
                    clientName = clientName ?: "Sin nombre",
                    baseFee = baseFee,
                    extras = extras,
                    totalAmount = totalAmount
                )

                // Método de pago
                PaymentMethodCard(
                    paymentMethod = paymentMethod,
                    paymentConfirmed = paymentConfirmed,
                    totalAmount = totalAmount,
                    onPaymentMethodChange = { paymentMethod = it },
                    onPaymentConfirmedChange = { paymentConfirmed = it },
                    onCopyAmount = {
                        // Copiar monto al portapapeles
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Monto a cobrar", totalAmount)
                        clipboard.setPrimaryClip(clip)
                        
                        // Mostrar mensaje de confirmación
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Monto copiado al portapapeles",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                ClientRatingCard(
                    rating = clientRating,
                    reviewComment = reviewComment,
                    onRatingChange = { clientRating = it },
                    onReviewCommentChange = { reviewComment = it }
                )
                
                if (!isCompleted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = RegisterColors.PrimaryBlue.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = RegisterColors.PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Al finalizar, este trabajo se marcará como completado automáticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.DarkGray
                            )
                        }
                    }
                }

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Contactar
                    OutlinedButton(
                        onClick = onContact,
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contactar")
                    }

                    // Botón Finalizar y enviar - siempre en naranja
                    Button(
                        onClick = {
                            viewModel.finalizePaymentAndRate(
                                jobId = jobId,
                                isAlreadyCompleted = isCompleted,
                                rating = clientRating,
                                comment = reviewComment.takeIf { it.isNotBlank() }
                            ) {
                                onFinish()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = paymentConfirmed && clientRating > 0 && !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange, // Naranja siempre
                            contentColor = RegisterColors.White,
                            disabledContainerColor = RegisterColors.PrimaryOrange, // Naranja incluso deshabilitado
                            disabledContentColor = RegisterColors.White // Texto blanco siempre
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = RegisterColors.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isSubmitting) "Guardando..." else "Finalizar y enviar")
                    }
                }

                // Texto informativo
                Text(
                    text = "Al enviar, se cerrará la solicitud y se notificará al cliente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun JobSummaryCard(
    jobTitle: String,
    clientName: String,
    baseFee: String,
    extras: String,
    totalAmount: String
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
            // Título y foto de perfil con chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Foto de perfil circular
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
                
                // Título, chips y estado
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fila con título y chip "Servicio finalizado"
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
                        
                        // Chip "Servicio finalizado" alineado a la derecha
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Servicio finalizado",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    
                    // Chips: "En sitio" y "Cliente: [nombre]" - estilo "8+ chars" en la misma fila
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip "En sitio"
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
                        
                        // Chip "Cliente: [nombre]"
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
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = RegisterColors.TextGray
                                )
                                Text(
                                    text = "Cliente: $clientName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = RegisterColors.BorderGray,
                thickness = 1.dp
            )

            // Desglose financiero
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
                    text = "S/ $baseFee",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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
                    text = "S/ $extras",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = RegisterColors.DarkGray
                )
            }

            HorizontalDivider(
                color = RegisterColors.BorderGray,
                thickness = 1.dp
            )

            // Total a cobrar
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total a cobrar",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                Text(
                    text = "S/ $totalAmount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.PrimaryOrange,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // Texto informativo
            Text(
                text = "Confirma el método de pago recibido.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray
            )
        }
    }
}

@Composable
fun PaymentMethodCard(
    paymentMethod: String,
    paymentConfirmed: Boolean,
    totalAmount: String,
    onPaymentMethodChange: (String) -> Unit,
    onPaymentConfirmedChange: (Boolean) -> Unit,
    onCopyAmount: () -> Unit
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
            Text(
                text = "Método de pago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )

            // Opción Yape - fondo blanco con borde como LoginScreen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPaymentMethodChange("yape") }
                    .then(
                        if (paymentMethod == "yape") {
                            Modifier.border(2.dp, RegisterColors.PrimaryOrange, RoundedCornerShape(30.dp))
                        } else {
                            Modifier.border(1.dp, RegisterColors.BorderGray, RoundedCornerShape(30.dp))
                        }
                    ),
                shape = RoundedCornerShape(30.dp), // Igual que los campos de LoginScreen
                color = Color.White // Fondo blanco como LoginScreen
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Smartphone, // Icono de smartphone táctil
                            contentDescription = null,
                            tint = RegisterColors.PrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Yape",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = RegisterColors.DarkGray
                        )
                    }
                    // Chip "Recomendado" - estilo "8+ chars" de LoginScreen
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9) // Fondo gris claro como "8+ chars"
                    ) {
                        Text(
                            text = "Recomendado",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF) // Texto gris más claro como "8+ chars"
                        )
                    }
                }
            }

            // Opción Efectivo - fondo blanco con borde como LoginScreen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPaymentMethodChange("cash") }
                    .then(
                        if (paymentMethod == "cash") {
                            Modifier.border(2.dp, RegisterColors.PrimaryOrange, RoundedCornerShape(30.dp))
                        } else {
                            Modifier.border(1.dp, RegisterColors.BorderGray, RoundedCornerShape(30.dp))
                        }
                    ),
                shape = RoundedCornerShape(30.dp), // Igual que los campos de LoginScreen
                color = Color.White // Fondo blanco como LoginScreen
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.AttachMoney, // Icono outlined como en LoginScreen
                        contentDescription = null,
                        tint = RegisterColors.PrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Efectivo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = RegisterColors.DarkGray
                    )
                }
            }

            // Botones de acción - estilo "8+ chars" de LoginScreen.kt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón "Copiar monto" - estilo "8+ chars"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCopyAmount() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9) // Fondo gris claro como "8+ chars"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF9CA3AF) // Texto gris más claro como "8+ chars"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Copiar monto",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF) // Texto gris más claro como "8+ chars"
                        )
                    }
                }

                // Botón "Confirmar recibido" - estilo "8+ chars" pero con fondo verde cuando está confirmado
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPaymentConfirmedChange(!paymentConfirmed) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (paymentConfirmed) {
                        Color(0xFF4CAF50).copy(alpha = 0.1f) // Fondo verde claro cuando está confirmado
                    } else {
                        Color(0xFFF1F5F9) // Fondo gris claro como "8+ chars" cuando no está confirmado
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (paymentConfirmed) {
                                Color(0xFF4CAF50) // Verde cuando está confirmado
                            } else {
                                Color(0xFF9CA3AF) // Gris cuando no está confirmado
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Confirmar recibido",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (paymentConfirmed) {
                                Color(0xFF4CAF50) // Verde cuando está confirmado
                            } else {
                                Color(0xFF9CA3AF) // Gris cuando no está confirmado
                            }
                        )
                    }
                }
            }

            // Banner verde de comisión - verde sólido con texto blanco e icono de escudo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF4CAF50), // Verde sólido
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Verified, // Icono de escudo con checkmark
                        contentDescription = null,
                        tint = Color.White, // Icono blanco
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cobrarás directamente al cliente: S/ $totalAmount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White // Texto blanco
                    )
                }
            }
        }
    }
}

@Composable
fun ClientRatingCard(
    rating: Int,
    reviewComment: String,
    onRatingChange: (Int) -> Unit,
    onReviewCommentChange: (String) -> Unit
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
            Text(
                text = "Calificar cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )

            // Estrellas de calificación - pequeñas y alineadas a la izquierda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start, // Alineadas a la izquierda
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val isSelected = i <= rating
                    
                    // Espaciado pequeño entre estrellas (excepto la primera)
                    if (i > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Surface(
                        modifier = Modifier
                            .size(36.dp) // Tamaño más pequeño
                            .clickable { onRatingChange(i) },
                        shape = CircleShape,
                        color = if (isSelected) {
                            RegisterColors.PrimaryOrange // Círculo naranja sólido cuando está seleccionada
                        } else {
                            Color(0xFFF1F5F9) // Círculo gris claro cuando no está seleccionada
                        },
                        border = if (isSelected) {
                            BorderStroke(
                                width = 1.5.dp,
                                color = RegisterColors.PrimaryOrange.copy(alpha = 0.7f) // Borde naranja más oscuro
                            )
                        } else {
                            null // Sin borde cuando no está seleccionada
                        },
                        shadowElevation = 0.5.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                // Estrella blanca rellena cuando está seleccionada
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$i estrellas",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp) // Estrella más pequeña
                                )
                            } else {
                                // Estrella solo contorno (outlined) gris más fuerte sobre fondo gris claro
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder, // Icono de estrella solo contorno
                                    contentDescription = "$i estrellas",
                                    tint = Color(0xFF9CA3AF), // Gris más fuerte para el contorno
                                    modifier = Modifier.size(18.dp) // Estrella más pequeña
                                )
                            }
                        }
                    }
                }
            }

            // Campo de comentario
            OutlinedTextField(
                value = reviewComment,
                onValueChange = onReviewCommentChange,
                placeholder = { 
                    Text(
                        "Escribe un comentario (opcional)...",
                        color = RegisterColors.TextGray
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegisterColors.PrimaryOrange,
                    unfocusedBorderColor = RegisterColors.BorderGray,
                    unfocusedContainerColor = RegisterColors.White,
                    focusedContainerColor = RegisterColors.White,
                    unfocusedTextColor = RegisterColors.DarkGray,
                    focusedTextColor = RegisterColors.DarkGray
                )
            )

            Text(
                text = "Tu reseña ayuda a mantener la comunidad segura y confiable.",
                style = MaterialTheme.typography.bodySmall,
                color = RegisterColors.TextGray
            )
        }
    }
}

