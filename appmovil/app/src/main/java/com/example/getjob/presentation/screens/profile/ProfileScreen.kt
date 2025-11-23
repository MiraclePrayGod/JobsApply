package com.example.getjob.presentation.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.ProfileViewModel
import com.example.getjob.presentation.viewmodel.ProfileUiState
import com.example.getjob.presentation.viewmodel.ProfileError
import com.example.getjob.utils.ImageStorageManager
import com.example.getjob.utils.ImageProcessingService
import com.example.getjob.utils.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ProfileHeader(
    fullName: String,
    profileImageUrl: String?,
    isAvailable: Boolean,
    isVerified: Boolean = false,
    localProfileImagePath: String? = null,
    onSelectProfileImage: () -> Unit = {}
) {
    val cardPadding = responsiveVerticalPadding() * 1.5f
    val spacing = responsiveSpacing()
    val avatarSize = responsiveAvatarSize()
    val titleFontSize = responsiveSubtitleFontSize()
    val cardRadius = responsiveCardCornerRadius()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Avatar con botón de añadir
            Box {
                // Mostrar imagen local si existe, sino la URL
                val imageModel = localProfileImagePath ?: profileImageUrl ?: ""
                
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(RegisterColors.BackgroundColor),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(avatarSize),
                            tint = RegisterColors.TextGray
                        )
                    },
                    error = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(avatarSize),
                            tint = RegisterColors.TextGray
                        )
                    }
                )
                
                // Botón de añadir en la esquina inferior derecha
                Surface(
                    onClick = onSelectProfileImage,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp),
                    shape = CircleShape,
                    color = RegisterColors.PrimaryOrange,
                    border = BorderStroke(2.dp, RegisterColors.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Añadir foto",
                            tint = RegisterColors.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                fontSize = titleFontSize
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isAvailable)
                        RegisterColors.PrimaryOrange.copy(alpha = 0.1f)
                    else
                        RegisterColors.TextGray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isAvailable)
                                RegisterColors.PrimaryOrange
                            else
                                RegisterColors.TextGray
                        )
                        Text(
                            text = if (isAvailable) "Disponible" else "No disponible",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAvailable)
                                RegisterColors.PrimaryOrange
                            else
                                RegisterColors.TextGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isVerified) {
                    Surface(
                        color = RegisterColors.PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RegisterColors.PrimaryBlue
                            )
                            Text(
                                text = "Verificado",
                                style = MaterialTheme.typography.labelMedium,
                                color = RegisterColors.PrimaryBlue,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    items: List<ProfileItem>
) {
    val cardPadding = responsiveVerticalPadding()
    val spacing = responsiveSpacing() * 0.75f
    val titleFontSize = responsiveSubtitleFontSize()
    val cardRadius = responsiveCardCornerRadius()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray,
                fontSize = titleFontSize
            )
            items.forEach { item ->
                ProfileItemRow(item = item)
            }
        }
    }
}

data class ProfileItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String
)

@Composable
fun ProfileItemRow(item: ProfileItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = RegisterColors.PrimaryOrange
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = RegisterColors.TextGray,
                fontSize = 12.sp
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                color = RegisterColors.DarkGray,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun VerificationSection(
    isVerified: Boolean,
    verificationPhotoUrl: String?,
    isSubmitting: Boolean,
    onSelectVerificationPhoto: () -> Unit
) {
    val cardPadding = responsiveVerticalPadding()
    val spacing = responsiveSpacing() * 0.75f
    val titleFontSize = responsiveSubtitleFontSize()
    val bodyFontSize = responsiveBodyFontSize()
    val cardRadius = responsiveCardCornerRadius()
    val iconSize = responsiveIconSize()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = RegisterColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verificación de cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray,
                        fontSize = titleFontSize
                    )
                    Text(
                        text = if (isVerified) 
                            "Tu cuenta está verificada y es confiable" 
                        else if (verificationPhotoUrl != null)
                            "Verificación en revisión"
                        else
                            "Verifica tu identidad subiendo una foto de tu DNI",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray,
                        fontSize = bodyFontSize * 0.85f
                    )
                }
                if (isVerified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = RegisterColors.PrimaryBlue,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
            if (!isVerified) {
                if (verificationPhotoUrl != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubcomposeAsyncImage(
                            model = verificationPhotoUrl,
                            contentDescription = "Foto de verificación",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(RegisterColors.BackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = RegisterColors.PrimaryOrange,
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(RegisterColors.BackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = RegisterColors.TextGray
                                    )
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foto enviada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RegisterColors.DarkGray,
                                fontSize = bodyFontSize,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Esperando aprobación del administrador",
                                style = MaterialTheme.typography.bodySmall,
                                color = RegisterColors.TextGray,
                                fontSize = bodyFontSize * 0.85f
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSelectVerificationPhoto,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviando...", fontSize = 14.sp)
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Subir foto de DNI", fontSize = 14.sp)
                        }
                    }
                    Text(
                        text = "Sube una foto clara de tu DNI para verificar tu identidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = RegisterColors.TextGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context = LocalContext.current
    val imageStorageManager = remember { ImageStorageManager(context) }
    val imageProcessingService = remember { ImageProcessingService(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val userId = preferencesManager.getUserId()
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para la ruta local de la foto de perfil
    var localProfileImagePath by remember { mutableStateOf<String?>(null) }
    
    // Cargar foto de perfil local al iniciar
    LaunchedEffect(userId) {
        if (userId > 0) {
            val path = imageStorageManager.getProfileImagePath(userId)
            localProfileImagePath = path
        }
    }
    
    // Launcher para seleccionar foto de perfil
    val profileImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Procesar imagen en background usando el servicio
            coroutineScope.launch {
                val bitmap = imageProcessingService.uriToBitmap(uri)
                bitmap?.let {
                    // Guardar localmente
                    if (userId > 0) {
                        val savedPath = imageStorageManager.saveProfileImage(it, userId)
                        if (savedPath != null) {
                            localProfileImagePath = savedPath
                        }
                    }
                }
            }
        }
    }
    
    // Launcher para seleccionar imagen de verificación
    val verificationImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Procesar imagen en background usando el servicio
            coroutineScope.launch {
                val imageDataUri = imageProcessingService.processImageForVerification(uri)
                imageDataUri?.let {
                    viewModel.submitVerification(it)
                }
            }
        }
    }
    
    // Mostrar snackbar de éxito
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionState.verificationSuccess) {
        if (actionState.verificationSuccess) {
            snackbarHostState.showSnackbar(
                message = "Foto de verificación enviada. Tu cuenta será revisada por un administrador.",
                duration = SnackbarDuration.Long
            )
            viewModel.clearVerificationSuccess()
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        when (uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RegisterColors.PrimaryOrange)
                }
            }
            is ProfileUiState.Error -> {
                val errorState = uiState as ProfileUiState.Error // Cast explícito para evitar smart cast issues
                val error = errorState.error
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
                            text = when (error) {
                                is ProfileError.Network -> ProfileError.Network.message
                                is ProfileError.Server -> ProfileError.Server.message
                                is ProfileError.Validation -> error.message
                                is ProfileError.Unknown -> error.message
                            },
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
            }
            is ProfileUiState.Success -> {
                val successState = uiState as ProfileUiState.Success // Cast explícito para evitar smart cast issues
                ProfileContent(
                    worker = successState.worker,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RegisterColors.BackgroundColor)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    actionState = actionState,
                    localProfileImagePath = localProfileImagePath,
                    onSelectProfileImage = {
                        profileImagePicker.launch("image/*")
                    },
                    onSelectVerificationPhoto = {
                        verificationImagePicker.launch("image/*")
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    worker: com.example.getjob.data.models.responses.WorkerResponse,
    modifier: Modifier = Modifier,
    actionState: com.example.getjob.presentation.viewmodel.ProfileActionState = com.example.getjob.presentation.viewmodel.ProfileActionState(),
    localProfileImagePath: String? = null,
    onSelectProfileImage: () -> Unit = {},
    onSelectVerificationPhoto: () -> Unit = {}
) {
    val horizontalPadding = responsiveHorizontalPadding()
    val verticalPadding = responsiveVerticalPadding()
    val spacing = responsiveSpacing()
    val maxWidth = responsiveMaxContentWidth()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (maxWidth != Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Foto de perfil y nombre
            ProfileHeader(
                fullName = worker.full_name,
                profileImageUrl = worker.profile_image_url,
                isAvailable = worker.is_available,
                isVerified = worker.is_verified,
                localProfileImagePath = localProfileImagePath,
                onSelectProfileImage = onSelectProfileImage
            )

            // Sección de verificación
            VerificationSection(
                isVerified = worker.is_verified,
                verificationPhotoUrl = worker.verification_photo_url,
                isSubmitting = actionState.isSubmittingVerification,
                onSelectVerificationPhoto = onSelectVerificationPhoto
            )

            // Información personal
            ProfileSection(
                title = "Información Personal",
                items = listOf(
                    ProfileItem(
                        icon = Icons.Default.Phone,
                        label = "Teléfono",
                        value = worker.phone ?: "No especificado"
                    ),
                    ProfileItem(
                        icon = Icons.Default.LocationOn,
                        label = "Distrito",
                        value = worker.district ?: "No especificado"
                    ),
                    ProfileItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Número Yape",
                        value = worker.yape_number ?: "No especificado"
                    )
                )
            )

            // Servicios
            if (!worker.services.isNullOrEmpty()) {
                ProfileSection(
                    title = "Servicios",
                    items = listOf(
                        ProfileItem(
                            icon = Icons.Default.Work,
                            label = "Servicios ofrecidos",
                            value = worker.services.joinToString(", ")
                        )
                    )
                )
            }

            // Descripción
            if (!worker.description.isNullOrEmpty()) {
                ProfileSection(
                    title = "Descripción",
                    items = listOf(
                        ProfileItem(
                            icon = Icons.Default.Description,
                            label = "Acerca de mí",
                            value = worker.description
                        )
                    )
                )
            }

            // Estado de disponibilidad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RegisterColors.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (worker.is_available) "Disponible" else "No disponible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (worker.is_available)
                                RegisterColors.PrimaryOrange
                            else
                                RegisterColors.TextGray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (worker.is_available)
                                "Estás recibiendo solicitudes"
                            else
                                "No estás recibiendo solicitudes",
                            style = MaterialTheme.typography.bodySmall,
                            color = RegisterColors.TextGray,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        if (worker.is_available) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (worker.is_available)
                            RegisterColors.PrimaryOrange
                        else
                            RegisterColors.TextGray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}


