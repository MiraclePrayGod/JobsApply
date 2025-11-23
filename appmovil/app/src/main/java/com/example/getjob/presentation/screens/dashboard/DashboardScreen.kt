package com.example.getjob.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import com.example.getjob.presentation.viewmodel.DashboardViewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.utils.*
import com.example.getjob.utils.PreferencesManager
import android.location.Location
import androidx.compose.material.icons.outlined.Info
import kotlinx.coroutines.launch

// ==================== FUNCIÓN BASE COMPARTIDA ====================

@Composable
fun DashboardCardBase(
    modifier: Modifier = Modifier,
    backgroundColor: Color = RegisterColors.PrimaryBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
                .heightIn(min = 110.dp), // 🔥 ESTANDARIZA LA ALTURA (ajustado a 110dp)
            content = content
        )
    }
}

// ==================== COMPONENTE DE BOTÓN REUTILIZABLE ====================

@Composable
fun ActionButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = background,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
            }
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = textColor,
                modifier = Modifier
                    .padding(vertical = 6.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileReadyCard(
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = RegisterColors.PrimaryBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AddTask,
                contentDescription = null,
                tint = RegisterColors.White,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tu perfil está listo",
                    style = MaterialTheme.typography.titleMedium,
                    color = RegisterColors.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ahora recibirás solicitudes cercanas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "Métodos de pago: Yape y efectivo. Usa Modo Plus para aplicar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.9f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = RegisterColors.White
                )
            }
        }
    }
}

@Composable
fun AvailabilityCard(
    isAvailable: Boolean,
    isExpanded: Boolean = false,
    isUpdating: Boolean = false,
    onExpandToggle: () -> Unit = {},
    onToggleAvailability: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DashboardCardBase(
        modifier = modifier,
        backgroundColor = if (isAvailable) RegisterColors.PrimaryBlue else RegisterColors.TextGray
    ) {
        // --- Texto principal (clickeable para expandir) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
        ) {
            Text(
                text = "Disponibilidad",
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.White.copy(alpha = 0.9f)
            )
            Text(
                text = if (isAvailable) "Activo" else "Inactivo",
                style = MaterialTheme.typography.titleLarge,
                color = RegisterColors.White,
                fontWeight = FontWeight.Bold
            )

            // --- Expandido ---
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAvailable) "Estado: Disponible para trabajar" else "Estado: No disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Horario: 24/7",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Zona: Cerca de tu ubicación",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.8f)
                )
            }
        }

        // Botones fuera del área clickeable para evitar clicks anidados
        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            // Botón cerrar
            ActionButton(
                text = "Cerrar",
                background = RegisterColors.White.copy(alpha = 0.2f),
                textColor = RegisterColors.White,
                onClick = onExpandToggle
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            // Botón Activar/Desactivar
            ActionButton(
                text = if (isAvailable) "Desactivar" else "Activar",
                background = RegisterColors.White,
                textColor = RegisterColors.DarkGray,
                onClick = { if (!isUpdating) onToggleAvailability() },
                isLoading = isUpdating
            )
        }
    }
}

@Composable
fun ModoPlusChip(
    isPlusActive: Boolean,
    plusExpiresAt: String?,
    onNavigateToModoPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Formatear fecha de expiración
    val formattedDate = remember(plusExpiresAt) {
        if (isPlusActive && plusExpiresAt != null) {
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = dateFormat.parse(plusExpiresAt)
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                outputFormat.format(date ?: java.util.Date())
            } catch (e: Exception) {
                plusExpiresAt
            }
        } else null
    }
    
    // Banner discreto en la parte superior
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isPlusActive, onClick = onNavigateToModoPlus),
        shape = RoundedCornerShape(12.dp),
        color = if (isPlusActive) {
            RegisterColors.PrimaryOrange.copy(alpha = 0.08f)
        } else {
            RegisterColors.TextGray.copy(alpha = 0.05f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Texto principal
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isPlusActive) RegisterColors.PrimaryOrange else RegisterColors.TextGray
                )
                Text(
                    text = if (isPlusActive) "Modo Plus" else "Activa Modo Plus para aplicar más",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlusActive) {
                        RegisterColors.PrimaryOrange
                    } else {
                        RegisterColors.TextGray
                    }
                )
                
                // Badge pequeño de estado
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPlusActive) {
                        RegisterColors.PrimaryOrange
                    } else {
                        RegisterColors.TextGray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = if (isPlusActive) "Activo" else "Inactivo",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isPlusActive) {
                            Color.White
                        } else {
                            RegisterColors.TextGray
                        }
                    )
                }
                
                // Fecha de expiración (solo si está activo)
                if (isPlusActive && formattedDate != null) {
                    Text(
                        text = "• Hasta $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Icono de navegación solo si está inactivo
            if (!isPlusActive) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Activar Modo Plus",
                    modifier = Modifier.size(18.dp),
                    tint = RegisterColors.TextGray.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EarningsCard(
    estimatedEarnings: Double,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    completedJobsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    DashboardCardBase(
        modifier = modifier,
        backgroundColor = RegisterColors.PrimaryBlue
    ) {
        // Contenido clickeable para expandir
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
        ) {
            Text(
                text = "Ganancias estimadas",
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.White.copy(alpha = 0.9f)
            )

            Text(
                text = "S/ ${String.format("%.2f", estimatedEarnings)}",
                style = MaterialTheme.typography.titleLarge,
                color = RegisterColors.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Semana",
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.White.copy(alpha = 0.9f)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total del mes: S/ ${String.format("%.2f", estimatedEarnings * 4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Trabajos completados: $completedJobsCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.White.copy(alpha = 0.8f)
                )
            }
        }

        if (!isExpanded) {
            Spacer(modifier = Modifier.height(12.dp)) // 🔥 idéntico a AvailabilityCard
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = "Buscar trabajos...",
                color = RegisterColors.PlaceholderGray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = RegisterColors.IconGray,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = RegisterColors.White,
            focusedContainerColor = RegisterColors.White,
            unfocusedBorderColor = RegisterColors.BorderGray,
            focusedBorderColor = RegisterColors.BorderGray,
            unfocusedTextColor = RegisterColors.DarkGray,
            focusedTextColor = RegisterColors.DarkGray
        )
    )
}

@Composable
fun ServiceFiltersSection(
    selectedService: String?,
    onServiceSelected: (String?) -> Unit,
    onClearFilter: () -> Unit
) {
    val services = listOf("Todos", "Plomería", "Electricidad", "Limpieza", "Pintura", "Carpintería")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RegisterColors.BackgroundColor)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(services) { service ->
                val isSelected = if (service == "Todos") {
                    selectedService == null
                } else {
                    selectedService == service
                }
                Surface(
                    modifier = Modifier.clickable {
                        if (service == "Todos") {
                            onClearFilter()
                        } else {
                            onServiceSelected(service)
                        }
                    },
                    shape = RoundedCornerShape(32.dp),
                    color = if (isSelected) RegisterColors.PrimaryOrange else RegisterColors.White,
                    border = if (isSelected) null else BorderStroke(1.dp, RegisterColors.BorderGray)
                ) {
                    Text(
                        text = service,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (isSelected) RegisterColors.White else RegisterColors.DarkGray,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

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
            createdDate == today -> {
                val hour = dateTime.hour
                val minute = dateTime.minute
                val amPm = if (hour < 12) "am" else "pm"
                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "Hoy ${displayHour}:${String.format("%02d", minute)}${amPm}"
            }
            createdDate == today.minusDays(1) -> {
                val hour = dateTime.hour
                val minute = dateTime.minute
                val amPm = if (hour < 12) "am" else "pm"
                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "Ayer ${displayHour}:${String.format("%02d", minute)}${amPm}"
            }
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

fun formatPaymentMethod(paymentMethod: String): String {
    return when (paymentMethod.lowercase()) {
        "cash" -> "Efectivo"
        "yape" -> "Yape"
        "plin" -> "Plin"
        else -> paymentMethod
    }
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconColor: Color = RegisterColors.PrimaryBlue
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = iconColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = RegisterColors.DarkGray,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
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
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
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
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun JobCard(
    job: com.example.getjob.data.models.responses.JobResponse,
    isExpanded: Boolean = false,
    isApplying: Boolean = false,
    isPlusActive: Boolean = false,
    distance: String = "N/A",
    onExpandToggle: () -> Unit = {},
    onApplyClick: () -> Unit,
    onPlusRequired: () -> Unit = {},
    buttonText: String = "Aplicar",
    modifier: Modifier = Modifier
) {
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
            // HEADER (solo esta parte es clickeable para expandir)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.service_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = job.status)
            }
            
            // CLIENTE
            if (job.client != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cliente: ${job.client.full_name?.takeIf { it.isNotBlank() } ?: "Sin nombre"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "${job.address.split(",").firstOrNull() ?: job.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "4.6",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (job.client.phone != null) {
                            Text(
                                text = "Teléfono: ${job.client.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.DarkGray
                            )
                        }
                        Text(
                            text = "Dirección completa: ${job.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
            }
            
            // DESCRIPCIÓN
            if (!job.description.isNullOrBlank()) {
                Text(
                    text = job.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.DarkGray,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3
                )
            }
            
            // INFO RÁPIDA - Iconos más coloridos y elegantes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoCard(
                    icon = Icons.Default.LocationOn,
                    text = distance,
                    modifier = Modifier.weight(1f),
                    iconColor = Color(0xFFE91E63) // Rosa vibrante para ubicación
                )
                InfoCard(
                    icon = Icons.Default.AccessTime,
                    text = formatCreatedDate(job.created_at),
                    modifier = Modifier.weight(1f),
                    iconColor = RegisterColors.PrimaryOrange // Naranja para tiempo
                )
                InfoCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    text = formatPaymentMethod(job.payment_method),
                    modifier = Modifier.weight(1f),
                    iconColor = RegisterColors.YapePurple // Morado para pago
                )
            }
            
            // DETALLES (solo si expandido)
            if (isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = RegisterColors.BorderGray)
                    Text(
                        text = "Detalles del trabajo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    Text(
                        text = "Presupuesto base: S/ ${job.base_fee}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.DarkGray
                    )
                    if (job.extras.toDouble() > 0) {
                        Text(
                            text = "Extras: S/ ${job.extras}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.DarkGray
                        )
                    }
                    Text(
                        text = "Total: S/ ${job.total_amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.PrimaryOrange
                    )
                    if (job.scheduled_at != null) {
                        Text(
                            text = "Programado para: ${job.scheduled_at}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.DarkGray
                        )
                    }
                }
            }
            
            // ACCIONES
            if (!isPlusActive) {
                // Texto explicativo
                Text(
                    text = "Este trabajo requiere Modo Plus activo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RegisterColors.TextGray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // Botón para adquirir Modo Plus
                Button(
                    onClick = onPlusRequired,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryBlue,
                        contentColor = RegisterColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adquirir Modo Plus para aplicar", fontWeight = FontWeight.Bold)
                }
            } else {
                // Botón normal de aplicar
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApplying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange,
                        contentColor = RegisterColors.White,
                        disabledContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = RegisterColors.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplicando...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToJobDetail: (Int) -> Unit,
    onLogout: () -> Unit,
    onJobAccepted: (Int) -> Unit = onNavigateToJobDetail, // Por defecto navega a detalles
    onNavigateToCompleteProfile: () -> Unit, // Nueva función para navegar a completar perfil
    onNavigateToRequests: () -> Unit = {}, // Nueva función para navegar a solicitudes
    onNavigateToCommissions: () -> Unit = {}, // Nueva función para navegar a comisiones
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val preferencesManager = remember { PreferencesManager(context) }
    val locationService = remember { LocationService.getInstance(context) }
    
    // Estado para ubicación del trabajador y distancias
    var workerLocation by remember { mutableStateOf<Location?>(null) }
    val distancesMap = remember { mutableStateMapOf<Int, String>() }

    // Estado para mostrar diálogos
    var showCompleteProfileDialog by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var showPlusRequiredDialog by remember { mutableStateOf(false) }
    
    // Obtener ubicación del trabajador
    LaunchedEffect(Unit) {
        if (locationService.hasLocationPermission() && locationService.isGpsEnabled()) {
            workerLocation = locationService.getCurrentLocation()
        }
    }
    
    // Calcular distancias cuando cambian los trabajos o la ubicación
    LaunchedEffect(uiState.availableJobs, workerLocation) {
        if (workerLocation != null) {
            uiState.availableJobs.forEach { job ->
                val jobLat = job.latitude?.toDouble()
                val jobLng = job.longitude?.toDouble()
                if (jobLat != null && jobLng != null) {
                    val distanceInMeters = locationService.calculateDistance(
                        workerLocation!!.latitude,
                        workerLocation!!.longitude,
                        jobLat,
                        jobLng
                    )
                    distancesMap[job.id] = locationService.formatDistance(distanceInMeters)
                } else {
                    distancesMap[job.id] = "N/A"
                }
            }
        } else {
            // Si no hay ubicación, limpiar distancias
            distancesMap.clear()
        }
    }

    // Verificar si el perfil se acaba de crear por primera vez
    var showProfileReadyBanner by remember { mutableStateOf(false) }

    // Verificar el estado del banner al cargar
    LaunchedEffect(Unit) {
        showProfileReadyBanner = preferencesManager.isProfileCreatedFirstTime()
        // Si el perfil está completo y el banner se mostró, marcarlo como visto
        if (uiState.isProfileComplete && showProfileReadyBanner) {
            // El banner se mostrará una vez y luego se marcará como visto
        }
    }

    // Recargar estado del perfil cuando la pantalla se vuelve visible
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkProfileStatus()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Función para cerrar sesión
    val logout: () -> Unit = {
        preferencesManager.clearAuthData()
        onLogout()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar Snackbar de éxito (NO mostrar si estamos aplicando a un trabajo)
    LaunchedEffect(uiState.successMessage, uiState.isApplyingToJob) {
        // Solo mostrar mensaje de éxito si NO estamos aplicando a un trabajo
        if (!uiState.isApplyingToJob && uiState.successMessage != null) {
            uiState.successMessage?.let { message ->
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearSuccessMessage()
            }
        }
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

    Scaffold(
        containerColor = RegisterColors.BackgroundColor, // Fondo igual que login/register
        topBar = {
            TopAppBar(
                // Sin padding adicional - completamente al top
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp), // quita todos los insets
                // Altura un poco más para que "Inicio" no esté tan pegado al top
                modifier = Modifier.height(48.dp), // Un poco más de altura (default es 64dp)
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Inicio",
                            color = RegisterColors.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // TODO: Implementar sistema de notificaciones
                    // Por ahora, el botón está presente pero sin funcionalidad
                    // Futuro: Mostrar notificaciones de trabajos aceptados, mensajes nuevos, etc.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { 
                                // TODO: Navegar a pantalla de notificaciones o mostrar diálogo
                                // onNavigateToNotifications()
                            },
                            modifier = Modifier.size(48.dp) // Tamaño normal del botón
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notificaciones",
                                tint = RegisterColors.DarkGray,
                                modifier = Modifier.size(24.dp) // Un poco más grande
                            )
                        }
                    }
                    // Botón de cerrar sesión
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = logout,
                            modifier = Modifier.size(48.dp) // Tamaño normal del botón
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ExitToApp,
                                contentDescription = "Cerrar sesión",
                                tint = RegisterColors.PrimaryOrange,
                                modifier = Modifier.size(24.dp) // Un poco más grande
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        val horizontalPadding = responsiveHorizontalPadding()
        val verticalPadding = responsiveVerticalPadding()
        val spacing = responsiveSpacing()
        val maxWidth = responsiveMaxContentWidth()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RegisterColors.BackgroundColor)
                // Aplicar solo el padding top del Scaffold (que ahora es menor porque el TopAppBar es más pequeño)
                .padding(
                    start = 0.dp,
                    top = innerPadding.calculateTopPadding(),
                    end = 0.dp,
                    bottom = 0.dp
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (maxWidth != Dp.Unspecified) {
                            Modifier.widthIn(max = maxWidth)
                        } else {
                            Modifier
                        }
                    ),
                // Agregar padding top para dar espacio al contenido después del TopAppBar reducido
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = verticalPadding, // Espacio normal para el contenido
                    end = horizontalPadding,
                    bottom = 0.dp // PEGADO al bottom bar
                ),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Tarjeta de perfil listo - solo mostrar cuando se crea el perfil por primera vez
                if (showProfileReadyBanner && uiState.isProfileComplete) {
                    item {
                        ProfileReadyCard(
                            onDismiss = {
                                // Marcar como visto para que no vuelva a aparecer
                                preferencesManager.setProfileCreatedFirstTime(false)
                                showProfileReadyBanner = false
                            }
                        )
                    }
                }

                // Tarjetas de disponibilidad y ganancias
                item {
                    var isAvailabilityExpanded by remember { mutableStateOf(false) }
                    var isEarningsExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AvailabilityCard(
                                isAvailable = uiState.isWorkerAvailable,
                                isExpanded = isAvailabilityExpanded,
                                isUpdating = uiState.isUpdatingAvailability,
                                onExpandToggle = {
                                    isAvailabilityExpanded = !isAvailabilityExpanded
                                },
                                onToggleAvailability = {
                                    viewModel.toggleAvailability {
                                        // Mostrar mensaje de éxito
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            EarningsCard(
                                estimatedEarnings = uiState.estimatedEarnings,
                                isExpanded = isEarningsExpanded,
                                onExpandToggle = { isEarningsExpanded = !isEarningsExpanded },
                                completedJobsCount = uiState.completedJobsCount,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Chip de Modo Plus
                item {
                    ModoPlusChip(
                        isPlusActive = uiState.isPlusActive,
                        plusExpiresAt = uiState.plusExpiresAt,
                        onNavigateToModoPlus = onNavigateToCommissions
                    )
                }

                // Barra de búsqueda
                item {
                    SearchBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) }
                    )
                }

                // Filtros de servicios
                item {
                    ServiceFiltersSection(
                        selectedService = uiState.selectedServiceFilter,
                        onServiceSelected = { service -> viewModel.filterByService(service) },
                        onClearFilter = { viewModel.filterByService(null) }
                    )
                }

                // Título de sección
                item {
                    Text(
                        text = "Solicitudes cercanas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray, // Gris oscuro igual que login/register
                        fontSize = responsiveSubtitleFontSize()
                    )
                }

                // Lista de trabajos disponibles
                // Mostrar trabajos siempre (estilo TikTok) - sin importar disponibilidad
                if (uiState.isLoading && uiState.availableJobs.isEmpty()) {
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
                } else if (uiState.availableJobs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = RegisterColors.TextGray // Gris igual que login/register
                                )
                                Text(
                                    "No hay trabajos disponibles",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = RegisterColors.TextGray // Gris igual que login/register
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.availableJobs) { job ->
                        var isExpanded by remember(job.id) { mutableStateOf(false) }
                        val distance = distancesMap[job.id] ?: "N/A"

                        JobCard(
                            job = job,
                            isExpanded = isExpanded,
                            isApplying = uiState.isApplyingToJob && uiState.applyingJobId == job.id,
                            isPlusActive = uiState.isPlusActive,
                            distance = distance,
                            onExpandToggle = { isExpanded = !isExpanded },
                            onPlusRequired = {
                                showPlusRequiredDialog = true
                            },
                            onApplyClick = {
                                android.util.Log.d("DashboardScreen", "Click en aplicar trabajo ${job.id}")
                                viewModel.applyToJob(
                                    jobId = job.id,
                                    onSuccess = {
                                        // Navegar automáticamente a Solicitudes después de aplicar
                                        android.util.Log.d("DashboardScreen", "Callback onSuccess llamado, navegando a Solicitudes")
                                        onNavigateToRequests()
                                    },
                                    onProfileIncomplete = {
                                        // Mostrar diálogo pidiendo completar perfil
                                        showCompleteProfileDialog = true
                                    },
                                    onAvailabilityRequired = {
                                        // Mostrar diálogo pidiendo activar disponibilidad
                                        showAvailabilityDialog = true
                                    },
                                    onPlusRequired = {
                                        // Mostrar diálogo pidiendo adquirir Modo Plus
                                        showPlusRequiredDialog = true
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

            }
        }
    }
    
    // Diálogo para completar perfil
    if (showCompleteProfileDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteProfileDialog = false },
            title = {
                Text(
                    "Completa tu perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            },
            text = {
                Text(
                    "Para aplicar a trabajos, necesitas completar tu perfil profesional. Completa tu información en la sección de Perfil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteProfileDialog = false
                        onNavigateToCompleteProfile()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Completar perfil",
                        color = RegisterColors.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCompleteProfileDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RegisterColors.TextGray
                    )
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = RegisterColors.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Diálogo para activar disponibilidad
    if (showAvailabilityDialog) {
        AlertDialog(
            onDismissRequest = { showAvailabilityDialog = false },
            title = {
                Text(
                    "Activa tu disponibilidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            },
            text = {
                Text(
                    "Para aplicar a trabajos, necesitas activar tu disponibilidad. Puedes hacerlo desde la tarjeta de disponibilidad arriba.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAvailabilityDialog = false
                        // Activar disponibilidad directamente
                        viewModel.toggleAvailability {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Activar ahora",
                        color = RegisterColors.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAvailabilityDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RegisterColors.TextGray
                    )
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = RegisterColors.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Diálogo para Modo Plus requerido
    if (showPlusRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showPlusRequiredDialog = false },
            title = {
                Text(
                    "Modo Plus requerido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            },
            text = {
                Text(
                    "Necesitas un plan Modo Plus activo para aplicar a trabajos. Adquiere tu plan en la sección Modo Plus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPlusRequiredDialog = false
                        onNavigateToCommissions()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange,
                        contentColor = RegisterColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ir a Modo Plus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPlusRequiredDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RegisterColors.TextGray
                    )
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = RegisterColors.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}


