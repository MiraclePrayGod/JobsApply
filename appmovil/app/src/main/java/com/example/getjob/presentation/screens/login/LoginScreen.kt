package com.example.getjob.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.getjob.presentation.viewmodel.LoginViewModel
import com.example.getjob.ui.theme.BlueSecondary
import com.example.getjob.ui.theme.OrangePrimary
import com.example.getjob.presentation.screens.register.RegisterColors

@Composable
fun LoginScreen(
    onNavigateToRegister: (role: String) -> Unit,
    onLoginSuccess: (userRole: String) -> Unit,
    viewModel: LoginViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("client") } // "client" o "worker"
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Navegar cuando el login sea exitoso
    LaunchedEffect(uiState.isSuccess, uiState.userRole) {
        if (uiState.isSuccess) {
            uiState.userRole?.let { role ->
                onLoginSuccess(role)
            }
        }
    }
    
    Scaffold(
        containerColor = Color(0xFFF7FAFC) // rgb(247, 250, 252) - #F7FAFC
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAFC)) // rgb(247, 250, 252) - #F7FAFC
                .padding(innerPadding)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val minDimension = minOf(screenWidth, screenHeight)
            
            // Calcular valores responsive - mantener diseño original en pantallas normales
            // Diseño original está basado en pantalla de 360dp de ancho
            val baseWidth = 360.dp
            val scaleFactor = screenWidth / baseWidth
            
            val horizontalPadding = (10.dp * scaleFactor).coerceIn(12.dp, 24.dp)
            val verticalSpacing = (12.dp * scaleFactor).coerceIn(8.dp, 20.dp)
            val topSpacing = (24.dp * scaleFactor).coerceIn(16.dp, 40.dp)
            val cardInternalPadding = (8.dp * scaleFactor).coerceIn(6.dp, 12.dp)
            val maxContentWidth = 600.dp // Ancho máximo para pantallas grandes
            
            // Centrar contenido en pantallas grandes - calcular una sola vez
            val contentWidth = if (screenWidth < maxContentWidth) screenWidth else maxContentWidth
            
            // Tamaños responsive para iconos y elementos
            val logoSize = (70.dp * scaleFactor).coerceIn(60.dp, 80.dp)
            val logoIconSize = (35.dp * scaleFactor).coerceIn(30.dp, 40.dp)
            val personIconSize = (32.dp * scaleFactor).coerceIn(28.dp, 36.dp)
            val fieldIconSize = (18.dp * scaleFactor).coerceIn(16.dp, 20.dp)
            val buttonIconSize = (16.dp * scaleFactor).coerceIn(14.dp, 18.dp)
            val buttonHeight = (56.dp * scaleFactor).coerceIn(50.dp, 64.dp)
            val cardBorderRadius = (32.dp * scaleFactor).coerceIn(28.dp, 36.dp)
            val fieldBorderRadius = (30.dp * scaleFactor).coerceIn(26.dp, 34.dp)
            val buttonBorderRadius = (12.dp * scaleFactor).coerceIn(10.dp, 14.dp)
            val buttonPadding = (8.dp * scaleFactor).coerceIn(6.dp, 10.dp) // Mínimo para dar máximo espacio al texto
            
            // Espacio deseado desde el borde derecho de la tarjeta hasta el botón "Soy Trabajador"
            val rightEdgeSpacing = (8.dp * scaleFactor).coerceIn(6.dp, 12.dp) // Mínimo necesario
            // Espacio entre los dos botones - USAR LA MISMA VARIABLE EN TODOS LADOS
            val spacingBetweenButtons = (8.dp * scaleFactor).coerceIn(6.dp, 12.dp) // Espacio entre botones
            
            // Calcular ancho disponible para los botones dentro de la tarjeta
            val cardWidth = contentWidth - (horizontalPadding * 2)
            val iconWidth = 40.dp
            val iconSpacing = 8.dp
            val columnWidth = cardWidth - iconWidth - iconSpacing - (cardInternalPadding * 2)
            // Ancho disponible para los botones: ancho del Column - espacio derecho - espacio entre botones
            val availableWidthForButtons = columnWidth - rightEdgeSpacing - spacingBetweenButtons
            // Ancho base para los botones - suficiente para "Soy Trabajador" completo
            // Usar el espacio disponible directamente, dividido entre 2, con un mínimo garantizado
            val calculatedWidth = (availableWidthForButtons / 2)
            val minRequiredWidth = 160.dp // Mínimo absoluto para que quepa "Soy Trabajador" completo
            val finalButtonWidth = calculatedWidth.coerceAtLeast(minRequiredWidth)
            
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
                    Spacer(modifier = Modifier.height(topSpacing))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                    // Logo circular azul con rayo (responsive)
                    Box(
                        modifier = Modifier
                            .size(logoSize)
                            .clip(CircleShape)
                            .background(RegisterColors.PrimaryBlue), // Azul #0B74FF
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(logoIconSize)
                        )
                    }
                    
                    // Nombre de la app (gris oscuro, no negro puro)
                    Text(
                        text = "ServiFast",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121) // Gris oscuro (casi negro, pero no negro puro)
                    )
                    
                    // Tagline (gris medio claro)
                    Text(
                        text = "Conecta clientes y trabajadores cerca de ti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray // Plomo #94A3B8 para textos
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
            
                    // Tarjeta de selección de rol
                    Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardBorderRadius), // Esquinas muy redondeadas (responsive)
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardInternalPadding, vertical = 24.dp), // Padding responsive
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Menos espacio entre icono y contenido
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contenedor para el icono - CENTRADO VERTICALMENTE en el medio
                    Box(
                        modifier = Modifier
                            .width(40.dp), // Ancho reducido del contenedor
                        contentAlignment = Alignment.Center // CENTRADO en el medio verticalmente
                    ) {
                        Icon(
                            Icons.Outlined.Person, // Icono outlined (solo contorno)
                            contentDescription = "Icono de usuario",
                            modifier = Modifier.size(personIconSize),
                            tint = Color(0xFF212121) // Negro
                        )
                    }
                    
                    // Contenido de texto y botones a la derecha del icono
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "¿Cómo deseas continuar?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121), // Gris oscuro (casi negro)
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Elige tu rol para personalizar tu experiencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray, // Plomo #94A3B8 para textos
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Botones en una sola fila, alineados a la derecha con espacio del borde
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = rightEdgeSpacing), // Espacio responsive desde el borde derecho
                            horizontalArrangement = Arrangement.End, // Alinear a la derecha
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f)) // Empujar los botones a la derecha
                            
                            // Botón "Soy Cliente"
                            Surface(
                                modifier = Modifier
                                    .width(finalButtonWidth) // Ancho ajustado para que quepa dentro de la tarjeta
                                    .clickable { selectedRole = "client" },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedRole == "client") RegisterColors.PrimaryOrange else Color.White,
                                border = if (selectedRole == "client") null else androidx.compose.foundation.BorderStroke(1.dp, RegisterColors.BorderGray) // Plomo #EAF0F6
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth() // Ocupar todo el ancho disponible del botón
                                        .padding(vertical = buttonPadding, horizontal = buttonPadding), // Padding responsive
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.ShoppingBag, // Bolsa de compras para cliente
                                        contentDescription = null,
                                        modifier = Modifier.size(buttonIconSize),
                                        tint = if (selectedRole == "client") Color.White else Color(0xFF212121) // Negro cuando no está seleccionado
                                    )
                                    Spacer(modifier = Modifier.width(2.dp)) // Menos espacio entre icono y texto
                                    Text(
                                        "Soy Cliente",
                                        color = if (selectedRole == "client") Color.White else Color(0xFF000000), // Gris oscuro
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible // Permitir que el texto se muestre completo
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(spacingBetweenButtons)) // Spacing responsive entre botones
                            
                            // Botón "Soy Trabajador"
                            Surface(
                                modifier = Modifier
                                    .width(finalButtonWidth) // Ancho ajustado para que quepa dentro de la tarjeta
                                    // Sin offset para que use todo el espacio disponible
                                    .clickable { selectedRole = "worker" },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedRole == "worker") RegisterColors.PrimaryOrange else Color.White,
                                border = if (selectedRole == "worker") null else androidx.compose.foundation.BorderStroke(1.dp, RegisterColors.BorderGray) // Plomo #EAF0F6
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth() // Ocupar todo el ancho disponible del botón
                                        .padding(vertical = buttonPadding, horizontal = buttonPadding), // Padding responsive
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Build, // Llave inglesa para trabajador
                                        contentDescription = null,
                                        modifier = Modifier.size(buttonIconSize),
                                        tint = if (selectedRole == "worker") Color.White else Color(0xFF212121) // Negro cuando no está seleccionado
                                    )
                                    Spacer(modifier = Modifier.width(2.dp)) // Menos espacio entre icono y texto
                                    Text(
                                        "Soy Trabajador",
                                        color = if (selectedRole == "worker") Color.White else Color(0xFF212121), // Gris oscuro
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible // Permitir que el texto se muestre completo
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
                    // Campo de correo electrónico
                    OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { 
                    Text(
                        "Correo electrónico",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray // Plomo #94A3B8 para textos
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(fieldBorderRadius), // Esquinas muy redondeadas (pill-shaped, responsive)
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, // Fondo blanco fuerte
                    focusedContainerColor = Color.White, // Fondo blanco fuerte cuando está enfocado
                    unfocusedBorderColor = RegisterColors.BorderGray, // Plomo #94A3B8
                    focusedBorderColor = RegisterColors.BorderGray, // Plomo #94A3B8
                    unfocusedTextColor = Color(0xFF212121) // Gris oscuro para texto
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email, // Icono outlined (solo contorno)
                        contentDescription = "Email",
                        modifier = Modifier.size(fieldIconSize),
                        tint = RegisterColors.IconGray // Plomo #94A3B8
                    )
                }
            )
            
                    // Campo de contraseña
                    OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { 
                    Text(
                        "Contraseña",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray // Plomo #94A3B8 para textos
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(30.dp), // Esquinas muy redondeadas (pill-shaped)
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, // Fondo blanco fuerte
                    focusedContainerColor = Color.White, // Fondo blanco fuerte cuando está enfocado
                    unfocusedBorderColor = RegisterColors.BorderGray, // Plomo #94A3B8
                    focusedBorderColor = RegisterColors.BorderGray, // Plomo #94A3B8
                    unfocusedTextColor = Color(0xFF212121) // Gris oscuro para texto
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lock, // Icono outlined (solo contorno)
                        contentDescription = "Password",
                        modifier = Modifier.size(fieldIconSize),
                        tint = RegisterColors.IconGray // Plomo #94A3B8
                    )
                },
                trailingIcon = {
                    Surface(
                        shape = RoundedCornerShape(16.dp), // Esquinas más redondeadas (pill-shaped)
                        color = Color(0xFFF1F5F9), // Fondo gris claro - rgb(241, 245, 249)
                        modifier = Modifier.padding(end = 12.dp) // Más padding para moverlo más a la derecha
                    ) {
                        Text(
                            text = "8+ chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF), // Texto gris más claro - rgb(156, 163, 175)
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp) // Padding original
                        )
                    }
                }
            )
            
                    // Botón Iniciar sesión (naranja exacto #FF6F3D)
                    Button(
                onClick = { 
                    // Normalizar email antes de hacer login (trim + lowercase)
                    // Esto mejora la UX y está alineado con el backend que también normaliza
                    val normalizedEmail = email.trim().lowercase()
                    viewModel.login(normalizedEmail, password) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange, // Naranja #FF6B35
                    contentColor = Color.White,
                    disabledContainerColor = RegisterColors.PrimaryOrange, // Naranja siempre, incluso deshabilitado
                    disabledContentColor = Color.White // Texto blanco al 100% siempre
                ),
                shape = RoundedCornerShape(buttonBorderRadius),
                enabled = !uiState.isLoading && email.isNotBlank() && password.length >= 8
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
                    // Botón Crear cuenta
                    OutlinedButton(
                onClick = { onNavigateToRegister(selectedRole) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(buttonBorderRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White, // Fondo blanco al 100%
                    contentColor = Color(0xFF212121) // Gris oscuro (no negro puro)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = RegisterColors.BorderGray // Plomo #EAF0F6
                )
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF212121) // Gris oscuro
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear cuenta", color = Color(0xFF212121)) // Gris oscuro
            }
            
                    // Separador "o"
                    Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "o",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            
                    // Botón Iniciar con Google
                    OutlinedButton(
                onClick = { /* TODO: Google Sign-In */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(buttonBorderRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White, // Fondo blanco al 100%
                    contentColor = Color(0xFF212121) // Gris oscuro (no negro puro)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = RegisterColors.BorderGray // Plomo #EAF0F6
                )
            ) {
                // Logo de Google (simplificado con "G" en círculo)
                Box(
                    modifier = Modifier
                        .size(28.dp) // Más grande
                        .background(Color.White, CircleShape)
                        .border(1.dp, RegisterColors.BorderGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        style = MaterialTheme.typography.titleSmall, // Texto más grande para el icono
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4) // Azul de Google
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar con Google", color = Color(0xFF212121)) // Gris oscuro
            }
            
                    // Mensaje de error
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Texto de disclaimer
                    Text(
                        text = "Al continuar aceptas los Términos y Condiciones de GetJob.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.PlaceholderGray, // Gris #ADB8C8
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Banner azul informativo
                    Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(buttonBorderRadius),
                colors = CardDefaults.cardColors(
                    containerColor = RegisterColors.PrimaryBlue // Azul #0B74FF
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Pagos: Yape o Efectivo. Con Modo Plus aplicas sin límites y cobras directo al cliente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
                    }
                }
            }
        }
    }
}

