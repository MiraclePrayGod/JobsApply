package com.example.getjob.presentation.screens.chat

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.getjob.data.models.responses.MessageResponse
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.viewmodel.ChatViewModel
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.utils.PreferencesManager
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    jobId: Int,
    applicationId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return ChatViewModel(application, jobId, applicationId) as T
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val currentUserId = preferencesManager.getUserId()
    val userRole = preferencesManager.getUserRole()
    
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Mostrar error en Snackbar cuando haya un error
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                // Limpiar el error después de mostrarlo
                viewModel.clearError()
            }
        }
    }
    
    // Launcher para seleccionar imagen
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val imageBytes = inputStream.readBytes()
                val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                viewModel.sendImage(base64Image)
            }
        }
    }
    
    // Scroll automático al final cuando hay nuevos mensajes
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        containerColor = RegisterColors.BackgroundColor,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
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
                            "Chat",
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Panel de información del trabajador (solo si hay applicationId y es cliente)
            if (applicationId != null && userRole == "client" && uiState.application != null) {
                WorkerInfoPanel(
                    application = uiState.application!!,
                    isLoading = uiState.isLoadingApplication,
                    isAccepting = uiState.isAcceptingWorker,
                    onAcceptWorker = {
                        android.util.Log.d("ChatScreen", "🔵 Botón 'Aceptar Trabajador' presionado")
                        viewModel.acceptWorker {
                            android.util.Log.d("ChatScreen", "✅ Callback onSuccess() ejecutado después de aceptar trabajador")
                            // Mostrar mensaje de éxito o navegar
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Indicador de conexión (solo mostrar si está desconectado)
            if (!uiState.isConnected) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Lista de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.sender_id == currentUserId,
                        viewModel = viewModel
                    )
                }
            }
            
            // Campo de entrada
            MessageInputField(
                messageText = uiState.messageText,
                onMessageTextChange = { viewModel.updateMessageText(it) },
                onSendMessage = {
                    if (uiState.messageText.isNotBlank()) {
                        viewModel.sendMessage(uiState.messageText)
                        viewModel.updateMessageText("")
                    }
                },
                onSelectImage = {
                    imagePicker.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageResponse,
    isCurrentUser: Boolean,
    viewModel: ChatViewModel
) {
    val backgroundColor = if (isCurrentUser) {
        RegisterColors.PrimaryOrange // Naranja para mensajes propios
    } else {
        Color(0xFFE8E8E8) // Gris claro para mensajes recibidos
    }
    val textColor = if (isCurrentUser) Color.White else RegisterColors.DarkGray
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        // Espaciado para mensajes del otro usuario
        if (!isCurrentUser) {
            Spacer(modifier = Modifier.width(40.dp))
        }
        
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Nombre del remitente (solo si no es el usuario actual)
            if (!isCurrentUser && message.sender != null) {
                Text(
                    text = message.sender.full_name ?: message.sender.email,
                    fontSize = 11.sp,
                    color = RegisterColors.TextGray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }
            
            // Burbuja del mensaje
            Surface(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isCurrentUser) 18.dp else 4.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 18.dp
                        )
                    ),
                color = backgroundColor,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Contenido del mensaje o imagen
                    if (message.has_image) {
                        // Intentar cargar imagen desde URL primero, luego local
                        var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        var isLoadingImage by remember { mutableStateOf(true) }
                        
                        LaunchedEffect(message.id, message.image_url) {
                            isLoadingImage = true
                            // Primero intentar desde URL si existe
                            if (message.image_url != null && message.image_url.startsWith("data:image")) {
                                try {
                                    // Es base64 data URL
                                    val base64 = message.image_url.substringAfter(",")
                                    val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                    imageBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    isLoadingImage = false
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatScreen", "Error al decodificar imagen base64", e)
                                    // Intentar obtener imagen local
                                    imageBitmap = viewModel.getImageForMessage(message.id, null)
                                    isLoadingImage = false
                                }
                            } else {
                                // Intentar obtener imagen local
                                imageBitmap = viewModel.getImageForMessage(message.id, null)
                                isLoadingImage = false
                            }
                        }
                        
                        if (isLoadingImage) {
                            // Mostrar placeholder mientras carga
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RegisterColors.BorderGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = RegisterColors.PrimaryOrange
                                )
                            }
                        } else if (imageBitmap != null) {
                            imageBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Imagen del mensaje",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            // Placeholder si no hay imagen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RegisterColors.BorderGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Imagen",
                                    tint = RegisterColors.MediumGray
                                )
                            }
                        }
                    } else if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }
                    
                    // Timestamp
                    Text(
                        text = formatTime(message.created_at),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        
        // Espaciado para mensajes propios
        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun MessageInputField(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = RegisterColors.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón para seleccionar imagen
            IconButton(
                onClick = onSelectImage,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Seleccionar imagen",
                    tint = RegisterColors.PrimaryOrange
                )
            }
            
            // Campo de texto
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...", color = RegisterColors.PlaceholderGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegisterColors.PrimaryOrange,
                    unfocusedBorderColor = RegisterColors.BorderGray
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4
            )
            
            // Botón de enviar
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(40.dp),
                containerColor = RegisterColors.PrimaryOrange
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White
                )
            }
        }
    }
}

fun formatTime(timestamp: String): String {
    return try {
        // Formato simple: "HH:mm"
        timestamp.substring(11, 16)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun WorkerInfoPanel(
    application: JobApplicationResponse,
    isLoading: Boolean,
    isAccepting: Boolean,
    onAcceptWorker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val worker = application.worker
    
    Card(
        modifier = modifier,
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
            // Título
            Text(
                text = "Información del Trabajador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RegisterColors.DarkGray
            )
            
            HorizontalDivider(color = RegisterColors.BorderGray)
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = RegisterColors.PrimaryBlue
                    )
                }
            } else {
                // Información del trabajador
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(RegisterColors.PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = RegisterColors.PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Nombre y verificación
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = worker?.full_name ?: "Trabajador",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RegisterColors.DarkGray
                            )
                            if (worker?.is_verified == true) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Verificado",
                                    modifier = Modifier.size(18.dp),
                                    tint = RegisterColors.PrimaryBlue
                                )
                            }
                        }
                        
                        if (worker?.phone != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = RegisterColors.MediumGray
                                )
                                Text(
                                    text = worker.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegisterColors.MediumGray
                                )
                            }
                        }
                    }
                }
                
                // Estado de aceptación
                if (application.is_accepted) {
                    Surface(
                        color = RegisterColors.PrimaryOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = RegisterColors.PrimaryOrange
                            )
                            Text(
                                text = "Trabajador aceptado",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = RegisterColors.PrimaryOrange
                            )
                        }
                    }
                } else {
                    // Botón aceptar trabajador
                    Button(
                        onClick = onAcceptWorker,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAccepting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange,
                            contentColor = RegisterColors.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = RegisterColors.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aceptando...")
                        } else {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Aceptar este Trabajador",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

