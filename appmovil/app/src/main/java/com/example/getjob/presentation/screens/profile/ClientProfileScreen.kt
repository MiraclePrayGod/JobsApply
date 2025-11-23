package com.example.getjob.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.ClientProfileViewModel
import com.example.getjob.presentation.screens.profile.ProfileSection
import com.example.getjob.presentation.screens.profile.ProfileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCreateJob: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    viewModel: ClientProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                            "Mi Perfil",
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
                actions = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateToEditProfile,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar perfil",
                                tint = RegisterColors.PrimaryOrange,
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
            com.example.getjob.presentation.components.ClientBottomNavigationBar(
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToCreateJob = onNavigateToCreateJob,
                onNavigateToProfile = { /* Ya estamos aquí */ },
                currentRoute = "profile"
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
                CircularProgressIndicator(color = RegisterColors.PrimaryOrange)
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RegisterColors.BackgroundColor)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = uiState.errorMessage ?: "Error desconocido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.refreshProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = RegisterColors.PrimaryOrange)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            uiState.user?.let { user ->
                ClientProfileContent(
                    user = user,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RegisterColors.BackgroundColor)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun ClientProfileContent(
    user: com.example.getjob.data.models.responses.UserResponse,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Foto de perfil y nombre
        ClientProfileHeader(
            fullName = user.full_name ?: "Usuario",
            email = user.email,
            role = user.role,
            profileImageUrl = user.profile_image_url
        )
        
        // Información personal
        ProfileSection(
            title = "Información Personal",
            items = listOf(
                ProfileItem(
                    icon = Icons.Default.Email,
                    label = "Correo electrónico",
                    value = user.email
                ),
                ProfileItem(
                    icon = Icons.Default.Phone,
                    label = "Teléfono",
                    value = user.phone ?: "No especificado"
                ),
                ProfileItem(
                    icon = Icons.Default.Person,
                    label = "Rol",
                    value = when (user.role.lowercase()) {
                        "client" -> "Cliente"
                        "worker" -> "Trabajador"
                        "manager" -> "Administrador"
                        else -> user.role
                    }
                )
            )
        )
        
        // Información de cuenta
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Información de cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray,
                    fontSize = 16.sp
                )
                Text(
                    text = "Cuenta creada: ${formatDate(user.created_at)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RegisterColors.TextGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ClientProfileHeader(
    fullName: String,
    email: String,
    role: String,
    profileImageUrl: String? = null
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar con imagen de perfil
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = RegisterColors.PrimaryOrange.copy(alpha = 0.1f)
            ) {
                if (profileImageUrl != null && profileImageUrl.isNotBlank()) {
                    // Cargar imagen desde URL usando Coil
                    SubcomposeAsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = RegisterColors.PrimaryOrange
                                )
                            }
                        },
                        error = {
                            // Si falla la carga, mostrar icono por defecto
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = RegisterColors.PrimaryOrange
                                )
                            }
                        }
                    )
                } else {
                    // Si no hay URL, mostrar icono por defecto
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = RegisterColors.PrimaryOrange
                        )
                    }
                }
            }
            
            // Nombre
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                fontSize = 20.sp
            )
            
            // Email
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.TextGray,
                fontSize = 14.sp
            )
            
            // Badge de rol
            Surface(
                color = RegisterColors.PrimaryOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = when (role.lowercase()) {
                        "client" -> "Cliente"
                        "worker" -> "Trabajador"
                        "manager" -> "Administrador"
                        else -> role
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = RegisterColors.PrimaryOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

