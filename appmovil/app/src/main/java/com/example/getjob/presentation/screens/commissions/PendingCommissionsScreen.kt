package com.example.getjob.presentation.screens.commissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.SubscriptionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingCommissionsScreen(
    onNavigateBack: () -> Unit,
    onPaymentSubmitted: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPlan by remember { mutableStateOf<String?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentCode by remember { mutableStateOf("") }
    var showPlans by remember { mutableStateOf(false) }
    
    Scaffold(
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
                            "Modo Plus",
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
        if (uiState.isLoading && uiState.status == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RegisterColors.BackgroundColor)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RegisterColors.PrimaryOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RegisterColors.BackgroundColor)
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp // Solo padding básico - la barra de navegación es el límite natural
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mostrar error si existe
                uiState.errorMessage?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Error: $error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Card de estado actual
                item {
                    StatusCard(
                        isPlusActive = uiState.status?.is_plus_active ?: false,
                        expiresAt = uiState.status?.plus_expires_at,
                        showPlans = showPlans,
                        onPayClick = {
                            // Mostrar planes disponibles para pagar/renovar
                            showPlans = true
                        },
                        onCancelClick = {
                            // IMPORTANTE: Solo ocultar los planes de renovación, NO cancelar la suscripción activa
                            // El botón Cancelar solo cierra la vista de renovar, no desactiva el Modo Plus
                            showPlans = false
                            selectedPlan = null // Limpiar selección también
                        }
                    )
                }
                
                // Cards de planes - mostrar si está inactivo o si se hizo clic en Pagar/Renovar
                if (!(uiState.status?.is_plus_active ?: false) || showPlans) {
                    item {
                        Text(
                            text = if (uiState.status?.is_plus_active == true) "Renovar suscripción" else "Planes disponibles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    item {
                        PlanCard(
                            plan = "daily",
                            title = "Plan Diario",
                            price = "S/ 2.00",
                            days = "1 día",
                            isSelected = selectedPlan == "daily",
                            onSelect = { selectedPlan = "daily" }
                        )
                    }
                    
                    item {
                        PlanCard(
                            plan = "weekly",
                            title = "Plan Semanal",
                            price = "S/ 12.00",
                            days = "7 días (1 día gratis)",
                            isSelected = selectedPlan == "weekly",
                            onSelect = { selectedPlan = "weekly" },
                            isRecommended = true
                        )
                    }
                    
                    // Botón para pagar
                    item {
                        Button(
                            onClick = { 
                                if (selectedPlan != null) {
                                    showPaymentDialog = true
                                }
                            },
                            enabled = selectedPlan != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RegisterColors.PrimaryOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pagar con Yape")
                        }
                    }
                }
                
                // Historial
                if (uiState.history.isNotEmpty()) {
                    item {
                        Text(
                            text = "Historial de suscripciones",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.DarkGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.history) { subscription ->
                        SubscriptionHistoryCard(subscription = subscription)
                    }
                }
            }
        }
    }
    
    // Dialog de pago
    if (showPaymentDialog) {
        PaymentDialog(
            plan = selectedPlan ?: "",
            paymentCode = paymentCode,
            onPaymentCodeChange = { paymentCode = it },
            onDismiss = {
                showPaymentDialog = false
                paymentCode = ""
                viewModel.clearError()
            },
            onSubmit = {
                if (paymentCode.isNotBlank() && selectedPlan != null) {
                    viewModel.subscribe(selectedPlan!!, paymentCode)
                }
            },
            isSubmitting = uiState.isSubscribing
        )
    }
    
    // Cerrar diálogo cuando se complete exitosamente
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && showPaymentDialog) {
            showPaymentDialog = false
            paymentCode = ""
            selectedPlan = null
            showPlans = false // Ocultar planes después del pago exitoso
            viewModel.resetSuccess()
            onPaymentSubmitted()
        }
    }
}

@Composable
fun StatusCard(
    isPlusActive: Boolean,
    expiresAt: String?,
    showPlans: Boolean = false,
    onPayClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    // Calcular días restantes o días usados
    val timeInfo = remember(expiresAt, isPlusActive) {
        if (isPlusActive && expiresAt != null) {
            try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val expiryDate = inputFormat.parse(expiresAt)
                val now = java.util.Date()
                expiryDate?.let {
                    val diffInMillis = it.time - now.time
                    val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                    if (diffInDays > 0) {
                        "Quedan $diffInDays ${if (diffInDays == 1) "día" else "días"}"
                    } else {
                        "Expira hoy"
                    }
                } ?: ""
            } catch (e: Exception) {
                "Válido hasta: ${formatDate(expiresAt)}"
            }
        } else {
            null
        }
    }
    
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge circular elegante a la izquierda
                Box(
                    modifier = Modifier.width(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlusActive) {
                            RegisterColors.PrimaryOrange.copy(alpha = 0.1f)
                        } else {
                            RegisterColors.TextGray.copy(alpha = 0.1f)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlusActive) {
                                    RegisterColors.PrimaryOrange
                                } else {
                                    RegisterColors.TextGray
                                }
                            )
                        }
                    }
                }
                
                // Contenido principal
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado Modo Plus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121) // Gris oscuro como LoginScreen
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPlusActive) {
                                RegisterColors.PrimaryOrange
                            } else {
                                RegisterColors.TextGray.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = if (isPlusActive) "Activo" else "Inactivo",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isPlusActive) {
                                    Color.White
                                } else {
                                    RegisterColors.TextGray
                                }
                            )
                        }
                    }
                    
                    if (isPlusActive && expiresAt != null) {
                        if (timeInfo != null) {
                            Text(
                                text = timeInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Válido hasta: ${formatDate(expiresAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray
                        )
                    } else {
                        Text(
                            text = "Activa el Modo Plus para poder aplicar a trabajos y ser visible en tu zona.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Botones de acción
            if (isPlusActive) {
                // Solo mostrar botones cuando está activo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Cancelar solo aparece cuando se está mostrando la opción de renovar
                    if (showPlans) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RegisterColors.White,
                                contentColor = Color(0xFF212121)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = RegisterColors.BorderGray
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF212121)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cancelar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF212121)
                            )
                        }
                    }
                    
                    // Botón Renovar
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier
                            .weight(if (showPlans) 1f else 1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Renovar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Cuando está inactivo, no mostrar botones - los planes se muestran automáticamente
        }
    }
}

@Composable
fun PlanCard(
    plan: String,
    title: String,
    price: String,
    days: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isRecommended: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = RegisterColors.PrimaryOrange.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(32.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isRecommended) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RegisterColors.PrimaryOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Recomendado",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = RegisterColors.PrimaryOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    Text(
                        text = days,
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                }
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.PrimaryOrange
                )
            }
        }
    }
}

@Composable
fun SubscriptionHistoryCard(
    subscription: com.example.getjob.data.models.responses.SubscriptionResponse
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (subscription.plan == "daily") "Plan Diario" else "Plan Semanal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    Text(
                        text = formatDate(subscription.created_at),
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (subscription.status == "active") {
                        RegisterColors.PrimaryOrange.copy(alpha = 0.1f)
                    } else {
                        RegisterColors.TextGray.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = subscription.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (subscription.status == "active") {
                            RegisterColors.PrimaryOrange
                        } else {
                            RegisterColors.TextGray
                        }
                    )
                }
            }
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Monto pagado",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = "S/ ${subscription.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Válido hasta",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = formatDate(subscription.valid_until),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    plan: String,
    paymentCode: String,
    onPaymentCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean = false
) {
    val planName = if (plan == "daily") "Plan Diario - S/ 2.00" else "Plan Semanal - S/ 12.00"
    val yapeNumber = "999888777" // Hardcodeado según especificación
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pagar con Yape",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RegisterColors.YapePurple, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RegisterColors.YapePurple)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = planName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RegisterColors.White
                        )
                        Text(
                            text = "Número de Yape: $yapeNumber",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = RegisterColors.White
                        )
                    }
                }
                
                OutlinedTextField(
                    value = paymentCode,
                    onValueChange = onPaymentCodeChange,
                    label = { Text("Código de operación") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: 123456") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                    }
                )
                
                Text(
                    text = "Realiza el pago a través de Yape y luego ingresa el código de operación que recibiste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = paymentCode.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange,
                    contentColor = Color.White,
                    disabledContainerColor = RegisterColors.TextGray.copy(alpha = 0.3f),
                    disabledContentColor = RegisterColors.DarkGray
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Confirmar pago",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = RegisterColors.DarkGray
                )
            }
        }
    )
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
