package com.example.getjob.presentation.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.getjob.presentation.viewmodel.RegisterViewModel

// Usar colores compartidos de RegisterColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterClientScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (userRole: String) -> Unit,
    registerViewModel: RegisterViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    val registerUiState by registerViewModel.uiState.collectAsState()
    
    // Manejar el éxito del registro
    LaunchedEffect(registerUiState.isSuccess, registerUiState.userRole) {
        if (registerUiState.isSuccess) {
            registerUiState.userRole?.let { role ->
                onRegisterSuccess(role)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RegisterColors.BackgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        // TopAppBar
        TopAppBar(
            title = { 
                Text(
                    "Registro Cliente",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = RegisterColors.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 24.dp),
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
                        Icons.Outlined.ShoppingBag,
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
                onValueChange = { fullName = it },
                placeholder = "Nombre y apellido",
                leadingIcon = Icons.Outlined.Person
            )
            
            PillTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Teléfono de contacto",
                leadingIcon = Icons.Outlined.Phone
            )
            
            PillTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Correo electrónico",
                leadingIcon = Icons.Outlined.Email
            )
            
            PillTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation()
            )
            
            PillTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirmar contraseña",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation()
            )
            
            // Error
            registerUiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            // Botón Continuar
            Button(
                onClick = {
                    if (password == confirmPassword && password.length >= 8) {
                        registerViewModel.register(
                            email = email,
                            password = password,
                            role = "client",
                            fullName = fullName.ifBlank { null },
                            phone = phone.ifBlank { null }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !registerUiState.isLoading && 
                         fullName.isNotBlank() && 
                         phone.isNotBlank() && 
                         email.isNotBlank() && 
                         password.length >= 8 && 
                         password == confirmPassword,
                colors = ButtonDefaults.buttonColors(
                containerColor = RegisterColors.PrimaryOrange,
                contentColor = RegisterColors.White,
                disabledContainerColor = RegisterColors.BorderGray,
                disabledContentColor = RegisterColors.PlaceholderGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (registerUiState.isLoading) {
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
}


