package com.example.getjob.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.screens.register.PillTextField
import com.example.getjob.presentation.viewmodel.ClientProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClientProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Estados locales para los campos del formulario
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Pre-llenar campos cuando se carga el usuario
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            fullName = user.full_name ?: ""
            email = user.email
            phone = user.phone ?: ""
            password = ""
            confirmPassword = ""
        }
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
                            "Editar Perfil",
                            color = RegisterColors.DarkGray,
                            fontSize = 18.sp,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card con el formulario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información Personal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray,
                        fontSize = 18.sp
                    )
                    
                    // Campo Nombre completo
                    PillTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Nombre y apellido",
                        leadingIcon = Icons.Outlined.Person
                    )
                    
                    // Campo Email
                    PillTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Correo electrónico",
                        leadingIcon = Icons.Outlined.Email
                    )
                    
                    // Campo Teléfono
                    PillTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "Teléfono de contacto",
                        leadingIcon = Icons.Outlined.Phone
                    )
                    
                    HorizontalDivider(
                        color = RegisterColors.BorderGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = "Cambiar Contraseña (opcional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray,
                        fontSize = 16.sp
                    )
                    
                    // Campo Contraseña
                    PillTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Nueva contraseña",
                        leadingIcon = Icons.Outlined.Lock,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    // Campo Confirmar contraseña
                    PillTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirmar nueva contraseña",
                        leadingIcon = Icons.Outlined.Lock,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    // Mensaje de error
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Botón Guardar
            Button(
                onClick = {
                    // Validar contraseñas si se ingresaron
                    if (password.isNotEmpty() && password != confirmPassword) {
                        // Mostrar error de validación
                        return@Button
                    }
                    
                    // Actualizar perfil (solo enviar campos que cambiaron)
                    viewModel.updateProfile(
                        email = if (email.isNotEmpty() && email != uiState.user?.email) email else null,
                        password = if (password.isNotEmpty()) password else null,
                        fullName = if (fullName.isNotEmpty() && fullName != uiState.user?.full_name) fullName else null,
                        phone = if (phone.isNotEmpty() && phone != uiState.user?.phone) phone else null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterColors.PrimaryOrange,
                    contentColor = RegisterColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = RegisterColors.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Text(
                        "Guardar Cambios",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
    
    // Mostrar mensaje de éxito y volver atrás cuando se actualice exitosamente
    var previousIsUpdating by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isUpdating) {
        // Si terminó de actualizar (antes estaba actualizando y ahora no)
        if (previousIsUpdating && !uiState.isUpdating && uiState.errorMessage == null) {
            // Si se completó la actualización sin errores, volver atrás
            onNavigateBack()
        }
        previousIsUpdating = uiState.isUpdating
    }
}

