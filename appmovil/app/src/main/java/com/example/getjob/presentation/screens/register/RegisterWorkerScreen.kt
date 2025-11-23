package com.example.getjob.presentation.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.getjob.presentation.viewmodel.RegisterViewModel
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.data.repository.WorkerRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Usar colores compartidos de RegisterColors

@Composable
fun RegisterWorkerUserStep(
    email: String,
    password: String,
    confirmPassword: String,
    fullName: String,
    phone: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Banner azul
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.PrimaryBlue
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top // Alineación superior
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = RegisterColors.White
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Crea tu cuenta",
                        color = RegisterColors.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completa tus datos para comenzar",
                        color = RegisterColors.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Título
        Text(
            text = "Crear cuenta",
            color = RegisterColors.DarkGray,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Campos pill-shaped
        PillTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "Nombre y apellido",
            leadingIcon = Icons.Outlined.Person
        )
        
        PillTextField(
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = "Teléfono de contacto",
            leadingIcon = Icons.Outlined.Phone
        )
        
        PillTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Correo electrónico",
            leadingIcon = Icons.Outlined.Email
        )
        
        PillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Contraseña",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation()
        )
        
        PillTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Confirmar contraseña",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation()
        )
        
        // Error
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
        
        // Botón Continuar
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Aumentar altura
            enabled = !isLoading && 
                     fullName.isNotBlank() && 
                     phone.isNotBlank() && 
                     email.isNotBlank() && 
                     password.length >= 8 && 
                     password == confirmPassword,
            colors = ButtonDefaults.buttonColors(
                containerColor = RegisterColors.PrimaryOrange,
                contentColor = RegisterColors.White,
                disabledContainerColor = RegisterColors.PrimaryOrange, // Siempre naranja
                disabledContentColor = RegisterColors.White // Texto blanco siempre
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RegisterColors.White
                )
            } else {
                Text(
                    "Continuar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RegisterWorkerProfileStep(
    fullName: String,
    phone: String,
    description: String,
    district: String,
    selectedServices: List<String>,
    isAvailable: Boolean,
    isNearMe: Boolean,
    yapeNumber: String,
    availableServices: List<String>,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDistrictChange: (String) -> Unit,
    onServiceToggle: (String) -> Unit,
    onAvailableToggle: (Boolean) -> Unit,
    onNearMeToggle: (Boolean) -> Unit,
    onYapeNumberChange: (String) -> Unit,
    onComplete: () -> Unit,
    userId: Int?,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    // Calcular padding responsive para mantener diseño original
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val screenWidth = maxWidth
        // Diseño original está basado en pantalla de 360dp de ancho
        val baseWidth = 360.dp
        val scaleFactor = screenWidth / baseWidth
        val cardInternalPadding = (16.dp * scaleFactor).coerceIn(12.dp, 20.dp)
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Banner azul con icono de herramienta
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.PrimaryBlue
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically // Centrado verticalmente en la columna
            ) {
                Icon(
                    Icons.Outlined.Build, // Icono outline (contorno blanco)
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = RegisterColors.White
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Crea tu perfil profesional",
                        color = RegisterColors.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completa tus datos para recibir solicitudes cercanas y cobrar por Yape o efectivo.",
                        color = RegisterColors.White.copy(alpha = 0.9f),
                        fontSize = 14.sp // Aumentado de 12.sp a 14.sp
                    )
                }
            }
        }
        
        // Información básica - Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardInternalPadding, vertical = 16.dp), // Padding responsive que mantiene diseño original
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Información básica",
                    color = RegisterColors.DarkGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Layout horizontal: Avatar + Campos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Avatar simple con icono de usuario - color uniforme
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .offset(y = 24.dp) // Mover el avatar más abajo verticalmente
                            .background(
                                color = RegisterColors.PrimaryBlue, // Color uniforme azul
                                shape = CircleShape
                            )
                            .shadow(20.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = RegisterColors.White
                        )
                    }
                    
                    // Campos a la derecha
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PillTextField(
                            value = fullName,
                            onValueChange = onFullNameChange,
                            placeholder = "Nombre y apellido",
                            leadingIcon = Icons.Outlined.Person
                        )
                        
                        PillTextField(
                            value = phone,
                            onValueChange = onPhoneChange,
                            placeholder = "Teléfono de contacto",
                            leadingIcon = Icons.Outlined.Phone
                        )
                    }
                }
            }
        }
        
        // Tus servicios - Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardInternalPadding, vertical = 16.dp), // Padding responsive que mantiene diseño original
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tus servicios",
                    color = RegisterColors.DarkGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Chips de servicios
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableServices.forEach { service ->
                        val isSelected = selectedServices.contains(service)
                        Surface(
                            modifier = Modifier
                                .clickable(
                                    indication = null, // Eliminar el efecto ripple que causa el "cuadrado plomo"
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onServiceToggle(service) }
                                .border(
                                    width = 1.dp,
                                    color = RegisterColors.BorderGray, // Siempre borde gris
                                    shape = RoundedCornerShape(32.dp) // Mismo borde redondeado que las tarjetas grandes
                                ),
                            shape = RoundedCornerShape(32.dp), // Mismo borde redondeado que las tarjetas grandes
                            color = if (isSelected) Color(0xFFF1F5F9) else RegisterColors.White // Fondo gris claro cuando está seleccionado
                        ) {
                            Text(
                            text = service,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = RegisterColors.DarkGray, // Siempre texto gris oscuro
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        }
                    }
                }
                
                // Campo descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    placeholder = { 
                        Text(
                            "Descripción corta (ej. 5 años de experiencia)",
                            fontSize = 13.sp, // Reducir tamaño de fuente para que quepa mejor
                            color = RegisterColors.PlaceholderGray,
                            maxLines = 2, // Permitir 2 líneas para el placeholder
                            softWrap = true, // Permitir que el texto se ajuste en múltiples líneas
                            lineHeight = 16.sp // Reducir altura de línea para que quepa mejor
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp), // Aumentar altura para que quepa el texto completo
                    maxLines = 3,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = RegisterColors.White,
                        focusedContainerColor = RegisterColors.White,
                        unfocusedBorderColor = RegisterColors.BorderGray, // Borde gris claro igual que otros campos
                        focusedBorderColor = RegisterColors.BorderGray, // Borde gris claro igual que otros campos
                        unfocusedTextColor = RegisterColors.DarkGray
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Edit, // Cambiar a icono outlined como los demás
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = RegisterColors.IconGray // Mismo color que otros iconos
                        )
                    },
                    trailingIcon = {
                        if (description.isNotBlank()) {
                            IconButton(
                                onClick = { onDescriptionChange("") }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    modifier = Modifier.size(18.dp),
                                    tint = RegisterColors.IconGray
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // Ubicación y disponibilidad - Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardInternalPadding, vertical = 16.dp), // Padding responsive que mantiene diseño original
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ubicación y disponibilidad",
                    color = RegisterColors.DarkGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Campo distrito con botón "Cerca de mi" dentro del mismo contenedor
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = RegisterColors.White, // Fondo blanco del contenedor
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegisterColors.BorderGray) // Borde gris claro
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Mismo tamaño que el campo "Número Yape"
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp), // Mínimo espacio entre campo y botón
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Campo de texto sin borde (dentro del contenedor) - usando BasicTextField para más control
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp) // Ajustado proporcionalmente al nuevo tamaño del contenedor
                                .padding(horizontal = 8.dp, vertical = 0.dp) // Padding mínimo
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Icono de ubicación
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = RegisterColors.IconGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Campo de texto básico
                                Box(modifier = Modifier.weight(1f)) {
                                    if (district.isEmpty()) {
                                        Text(
                                            "Distrito / zona",
                                            fontSize = 14.sp, // Mismo tamaño que el campo "Número Yape"
                                            color = RegisterColors.PlaceholderGray, // Mismo color plomo que en el login
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = district,
                                        onValueChange = onDistrictChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 14.sp, // Mismo tamaño que el campo "Número Yape"
                                            color = RegisterColors.DarkGray
                                        ),
                                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
                                    )
                                }
                            }
                        }
                        
                        // Botón "Cerca de mi" dentro del mismo contenedor - altura reducida, fondo extendido
                        Surface(
                            modifier = Modifier
                                .height(44.dp) // Ajustado proporcionalmente al nuevo tamaño del contenedor
                                .width(100.dp) // Ancho fijo para extender el fondo plomo
                                .padding(end = 8.dp) // Padding mínimo a la derecha
                                .clickable(
                                    indication = null, // Eliminar el efecto ripple que causa el "cuadrado plomo"
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onNearMeToggle(!isNearMe) },
                            shape = RoundedCornerShape(30.dp),
                            color = if (isNearMe) Color(0xFF4CAF50) else Color(0xFFF1F5F9) // Verde cuando está activo, gris cuando no
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize() // Ocupar todo el espacio del Surface
                                    .padding(horizontal = 12.dp, vertical = 0.dp), // Padding aumentado para margen del texto
                                contentAlignment = Alignment.Center
                            ) {                                                                                                                                                                                                                                                                                 
                                Text(
                                    text = "Cerca de mi",
                                    color = if (isNearMe) RegisterColors.White else Color(0xFF9CA3AF), // Texto blanco cuando está activo, gris cuando no
                                    fontSize = 11.sp, // Tamaño de fuente más pequeño
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis // Evitar desbordamiento
                                )
                            }
                        }
                    }
                }
                
                // Toggle "Disponible ahora" con fondo blanco y borde visible
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), // Misma altura que otros campos
                    shape = RoundedCornerShape(30.dp),
                    color = RegisterColors.White, // Fondo blanco como en la imagen
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegisterColors.BorderGray) // Borde gris claro visible
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime, // Cambiar a outlined
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = RegisterColors.IconGray
                            )
                            Text(
                                "Disponible ahora",
                                color = RegisterColors.DarkGray,
                                fontSize = 16.sp
                            )
                        }
                        // Toggle personalizado tipo píldora: "On" siempre visible, gris cuando desactivado, blanco cuando activado
                        Surface(
                            modifier = Modifier
                                .size(width = 52.dp, height = 28.dp)
                                .clickable { onAvailableToggle(!isAvailable) },
                            shape = RoundedCornerShape(32.dp), // Mismo radio que las tarjetas
                            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF1F5F9), // Verde cuando activado, gris claro cuando desactivado
                            // Sin borde
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Siempre mostrar "On", color cambia según el estado
                                Text(
                                    text = "On",
                                    fontSize = 11.sp,
                                    color = if (isAvailable) Color.White else Color(0xFF9CA3AF), // Blanco cuando activado, gris cuando desactivado
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Pagos y comisión - Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(32.dp), // Esquinas muy redondeadas (igual que login)
            colors = CardDefaults.cardColors(
                containerColor = RegisterColors.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardInternalPadding, vertical = 16.dp), // Padding responsive que mantiene diseño original
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Métodos de pago",
                    color = RegisterColors.DarkGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Botones de método y comisión
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Reducir spacing para dar más espacio a los botones
                ) {
                    // Método principal
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(32.dp), // Mismo radio que las tarjetas
                        colors = CardDefaults.cardColors(
                            containerColor = RegisterColors.PrimaryBlue
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp) // Padding aumentado para margen del texto
                        ) {
                            Text(
                                text = "Método principal",
                                color = RegisterColors.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Yape",
                                color = RegisterColors.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Modo Plus
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(32.dp), // Mismo radio que las tarjetas
                        colors = CardDefaults.cardColors(
                            containerColor = RegisterColors.PrimaryBlue
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp) // Padding aumentado para margen del texto
                        ) {
                            Text(
                                text = "Modo Plus",
                                color = RegisterColors.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Sin comisión",
                                color = RegisterColors.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Campo Yape
                PillTextField(
                    value = yapeNumber,
                    onValueChange = onYapeNumberChange,
                    placeholder = "Número Yape (opcional)",
                    leadingIcon = Icons.Outlined.AccountBalanceWallet
                )
                
                // Texto informativo
                Text(
                    text = "Los trabajadores pueden activar Modo Plus para aplicar a trabajos sin comisión.",
                    color = RegisterColors.MediumGray,
                    fontSize = 14.sp
                )
            }
        }
        
        // Error
        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                )
            }
        }
        
        // Botón Continuar
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Aumentar altura
            enabled = !isLoading && fullName.isNotBlank() && selectedServices.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = RegisterColors.PrimaryOrange,
                contentColor = RegisterColors.White,
                disabledContainerColor = RegisterColors.PrimaryOrange, // Siempre naranja
                disabledContentColor = RegisterColors.White // Texto blanco siempre
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RegisterColors.White
                )
            } else {
                Text(
                    "Continuar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Botón "Ver términos y condiciones"
        OutlinedButton(
            onClick = { /* TODO: Ver términos */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RegisterColors.BorderGray),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = RegisterColors.DarkGray
            )
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = RegisterColors.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Ver términos y condiciones",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Footer con padding adecuado para que no esté pegado a los bordes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 22.dp) // Padding horizontal y vertical inferior
        ) {
            Text(
                text = "Podrás editar tu perfil, fotos y zona luego en Perfil.",
                color = RegisterColors.MediumGray,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterWorkerScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (userRole: String) -> Unit,
    initialStep: Int = 1, // Permite empezar directamente en el paso 2 para completar perfil
    onProfileComplete: (() -> Unit)? = null, // Callback cuando se completa el perfil desde el paso 2
    registerViewModel: RegisterViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val workerRepository = remember { WorkerRepository() }
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(initialStep) } // 1: Auth, 2: Worker Profile
    
    // Obtener userId desde PreferencesManager si se inicia desde el paso 2
    val userIdFromPrefs = remember(initialStep) {
        if (initialStep == 2) preferencesManager.getUserId() else null
    }
    
    // Paso 1: Registro de usuario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    // Paso 2: Perfil de trabajador
    var workerFullName by remember { mutableStateOf("") }
    var workerPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var selectedServices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAvailable by remember { mutableStateOf(false) }
    var isNearMe by remember { mutableStateOf(false) }
    var yapeNumber by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(false) }
    
    val registerUiState by registerViewModel.uiState.collectAsState()
    val availableServices = listOf("Plomería", "Electricidad", "Limpieza", "Pintura", "Carpintería")
    
    // Cargar perfil existente cuando se abre en modo edición (initialStep == 2)
    LaunchedEffect(initialStep) {
        if (initialStep == 2) {
            isLoadingProfile = true
            coroutineScope.launch {
                workerRepository.getMyProfile()
                    .onSuccess { worker ->
                        // Llenar campos con datos existentes
                        workerFullName = worker.full_name ?: ""
                        workerPhone = worker.phone ?: ""
                        description = worker.description ?: ""
                        district = worker.district ?: ""
                        selectedServices = worker.services ?: emptyList()
                        isAvailable = worker.is_available
                        yapeNumber = worker.yape_number ?: ""
                        isLoadingProfile = false
                    }
                    .onFailure {
                        // Si hay error al cargar, dejar campos vacíos
                        isLoadingProfile = false
                    }
            }
        }
    }
    
    // Manejar el éxito del registro
    LaunchedEffect(registerUiState.isSuccess, registerUiState.userRole) {
        if (registerUiState.isSuccess && registerUiState.userRole != null) {
            if (currentStep == 1) {
                currentStep = 2
                registerViewModel.resetSuccess()
            }
        }
    }
    
    // Manejar la finalización del perfil
    LaunchedEffect(registerUiState.isWorkerProfileComplete) {
        if (registerUiState.isWorkerProfileComplete) {
            if (initialStep == 2 && onProfileComplete != null) {
                // Si empezó en el paso 2, usar el callback de completar perfil
                onProfileComplete()
            } else {
                // Si empezó en el paso 1, usar el callback de registro exitoso
                onRegisterSuccess(registerUiState.userRole ?: "worker")
            }
            registerViewModel.resetSuccess()
        }
    }
    
    // Diseño responsive
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RegisterColors.BackgroundColor)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val minDimension = minOf(screenWidth, screenHeight)

        // Calcular valores responsive - mantener diseño original en pantallas normales
        // Diseño original está basado en pantalla de 360dp de ancho
        val baseWidth = 360.dp
        val scaleFactor = screenWidth / baseWidth

        val horizontalPadding = (16.dp * scaleFactor).coerceIn(12.dp, 24.dp)
        val verticalSpacing = (12.dp * scaleFactor).coerceIn(8.dp, 20.dp)
        val cardPadding = (16.dp * scaleFactor).coerceIn(12.dp, 20.dp)
        val cardInternalPadding = (16.dp * scaleFactor).coerceIn(12.dp, 20.dp)
        val maxContentWidth = 600.dp // Ancho máximo para pantallas grandes

        // Centrar contenido en pantallas grandes
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
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TopAppBar
                TopAppBar(
                    title = {
                        Text(
                            if (initialStep == 2) "Editar Perfil" else "Registro Trabajador",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (initialStep == 2) {
                                // Si empezó en el paso 2, siempre volver atrás
                                onNavigateBack()
                            } else if (currentStep == 1) {
                                onNavigateBack()
                            } else {
                                currentStep = 1
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RegisterColors.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Indicador de progreso (solo mostrar si empezó en el paso 1)
                if (initialStep == 1) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { if (currentStep == 1) 0.33f else 0.8f },
                        color = RegisterColors.PrimaryOrange,
                        trackColor = RegisterColors.BorderGray
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .padding(top = verticalSpacing, bottom = verticalSpacing),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    when (currentStep) {
                        1 -> {
                            RegisterWorkerUserStep(
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword,
                                fullName = fullName,
                                phone = phone,
                                onEmailChange = { email = it },
                                onPasswordChange = { password = it },
                                onConfirmPasswordChange = { confirmPassword = it },
                                onFullNameChange = { fullName = it },
                                onPhoneChange = { phone = it },
                                onNext = {
                                    if (password == confirmPassword && password.length >= 8) {
                                        registerViewModel.register(
                                            email = email,
                                            password = password,
                                            role = "worker",
                                            fullName = fullName.ifBlank { null },
                                            phone = phone.ifBlank { null }
                                        )
                                    }
                                },
                                isLoading = registerUiState.isLoading,
                                errorMessage = registerUiState.errorMessage
                            )
                        }

                        2 -> {
                            // Mostrar indicador de carga mientras se carga el perfil
                            if (isLoadingProfile) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = RegisterColors.PrimaryOrange
                                    )
                                }
                            } else {
                                val displayFullName =
                                    if (workerFullName.isBlank()) fullName else workerFullName
                                val displayPhone = if (workerPhone.isBlank()) phone else workerPhone

                                RegisterWorkerProfileStep(
                                    fullName = displayFullName,
                                    phone = displayPhone,
                                    description = description,
                                    district = district,
                                    selectedServices = selectedServices,
                                    isAvailable = isAvailable,
                                    isNearMe = isNearMe,
                                    yapeNumber = yapeNumber,
                                    availableServices = availableServices,
                                    onFullNameChange = { workerFullName = it },
                                    onPhoneChange = { workerPhone = it },
                                    onDescriptionChange = { description = it },
                                    onDistrictChange = { district = it },
                                    onServiceToggle = { service ->
                                        selectedServices = if (selectedServices.contains(service)) {
                                            selectedServices - service
                                        } else {
                                            selectedServices + service
                                        }
                                    },
                                    onAvailableToggle = { isAvailable = it },
                                    onNearMeToggle = { isNearMe = it },
                                    onYapeNumberChange = { yapeNumber = it },
                                    onComplete = {
                                        // Si empezó en el paso 2, usar userId de PreferencesManager, sino del estado
                                        val userId =
                                            if (initialStep == 2) userIdFromPrefs else registerUiState.userId
                                        val finalFullName =
                                            if (workerFullName.isBlank()) fullName else workerFullName
                                        val finalPhone =
                                            if (workerPhone.isBlank()) phone else workerPhone

                                        // Validar campos requeridos
                                        if (userId == null) {
                                            registerViewModel.clearError()
                                            // Mostrar error - esto se manejará en el ViewModel
                                            return@RegisterWorkerProfileStep
                                        }

                                        if (finalFullName.isBlank()) {
                                            registerViewModel.clearError()
                                            // El ViewModel mostrará el error
                                            return@RegisterWorkerProfileStep
                                        }

                                        // Si se está editando (initialStep == 2), usar updateProfile
                                        if (initialStep == 2) {
                                            registerViewModel.updateWorkerProfile(
                                                fullName = finalFullName,
                                                phone = finalPhone.ifBlank { null },
                                                services = selectedServices.takeIf { it.isNotEmpty() },
                                                description = description.ifBlank { null },
                                                district = district.ifBlank { null },
                                                isAvailable = isAvailable,
                                                yapeNumber = yapeNumber.ifBlank { null }
                                            )
                                        } else {
                                            // Si se está registrando (initialStep == 1), usar registerWorkerProfile
                                            registerViewModel.registerWorkerProfile(
                                                userId = userId,
                                                fullName = finalFullName,
                                                phone = finalPhone.ifBlank { null },
                                                services = selectedServices.takeIf { it.isNotEmpty() },
                                                description = description.ifBlank { null },
                                                district = district.ifBlank { null },
                                                isAvailable = isAvailable,
                                                yapeNumber = yapeNumber.ifBlank { null }
                                            )
                                        }
                                    },
                                    userId = if (initialStep == 2) userIdFromPrefs else registerUiState.userId,
                                    isLoading = registerUiState.isLoading,
                                    errorMessage = registerUiState.errorMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
