package com.example.getjob.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import com.example.getjob.presentation.viewmodel.CreateJobViewModel
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.presentation.components.LocationPermissionHandler
import com.example.getjob.utils.GeocodingService
import com.example.getjob.utils.LocationService
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    onNavigateBack: () -> Unit,
    onJobCreated: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: CreateJobViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Plomería") }
    var paymentMethod by remember { mutableStateOf("yape") }
    var baseFee by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<BigDecimal?>(null) }
    var longitude by remember { mutableStateOf<BigDecimal?>(null) }
    var isGettingCoordinates by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val geocodingService = remember { GeocodingService() }
    val locationService = remember { LocationService.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val availableServices = listOf("Plomería", "Electricidad", "Limpieza", "Pintura", "Carpintería")

    // Responsive - usando el mismo sistema que LoginScreen
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val baseWidth = 360.dp
    val scaleFactor = screenWidth / baseWidth
    
    val horizontalPadding = (10.dp * scaleFactor).coerceIn(12.dp, 24.dp)
    val verticalSpacing = (12.dp * scaleFactor).coerceIn(8.dp, 20.dp)
    val cardInternalPadding = (16.dp * scaleFactor).coerceIn(12.dp, 20.dp)
    val cardBorderRadius = (32.dp * scaleFactor).coerceIn(28.dp, 36.dp) // Mismo que LoginScreen
    val fieldBorderRadius = (30.dp * scaleFactor).coerceIn(26.dp, 34.dp) // Pill-shaped como LoginScreen
    val buttonBorderRadius = (12.dp * scaleFactor).coerceIn(10.dp, 14.dp) // Mismo que LoginScreen

    // Debounce geolocalización
    LaunchedEffect(address) {
        if (address.isNotBlank() && address.length > 10) {
            kotlinx.coroutines.delay(1500)
            if (address.isNotBlank()) {
                isGettingCoordinates = true
                coroutineScope.launch {
                    val coords = geocodingService.geocodeAddress(address)
                    if (coords != null) {
                        latitude = BigDecimal(coords.first.toString())
                        longitude = BigDecimal(coords.second.toString())
                    }
                    isGettingCoordinates = false
                }
            }
        } else {
            latitude = null
            longitude = null
        }
    }

    // Navegación cuando se crea el trabajo
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            onJobCreated()
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
                            "Crear Trabajo",
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
                }
            )
        },
        bottomBar = {
            com.example.getjob.presentation.components.ClientBottomNavigationBar(
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToCreateJob = {},
                onNavigateToProfile = onNavigateToProfile,
                currentRoute = "create_job"
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {

            // -----------------------------
            // TÍTULO PRINCIPAL
            // -----------------------------
            item {
                Text(
                    "Nueva Solicitud de Servicio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            }

            // -----------------------------
            // TARJETA: TITLE + DESCRIPTION
            // -----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardInternalPadding),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título del trabajo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(fieldBorderRadius), // Pill-shaped como LoginScreen
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = RegisterColors.BorderGray,
                                focusedBorderColor = RegisterColors.BorderGray,
                                unfocusedTextColor = Color(0xFF212121)
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, null, tint = RegisterColors.IconGray)
                            }
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción (opcional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            maxLines = 4,
                            shape = RoundedCornerShape(fieldBorderRadius), // Pill-shaped como LoginScreen
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = RegisterColors.BorderGray,
                                focusedBorderColor = RegisterColors.BorderGray,
                                unfocusedTextColor = Color(0xFF212121)
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.Description, null, tint = RegisterColors.IconGray)
                            }
                        )
                    }
                }
            }

            // --------------------------------
            // TARJETA: TIPO DE SERVICIO
            // --------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                    colors = CardDefaults.cardColors(containerColor = RegisterColors.PrimaryBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardInternalPadding),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Work, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Tipo de servicio", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableServices) { service ->
                                FilterChip(
                                    selected = serviceType == service,
                                    onClick = { serviceType = service },
                                    label = {
                                        Text(
                                            service,
                                            fontSize = 12.sp,
                                            color = if (serviceType == service)
                                                RegisterColors.PrimaryBlue else Color.White
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------
            // TARJETA: MÉTODO DE PAGO
            // -------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardInternalPadding),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Payment, null, tint = RegisterColors.DarkGray)
                            Spacer(Modifier.width(8.dp))
                            Text("Método de pago", fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = paymentMethod == "yape",
                                onClick = { paymentMethod = "yape" },
                                label = { 
                                    Text(
                                        "Yape",
                                        color = if (paymentMethod == "yape") {
                                            RegisterColors.PrimaryOrange
                                        } else {
                                            RegisterColors.DarkGray
                                        }
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.15f),
                                    containerColor = RegisterColors.PrimaryBlue.copy(alpha = 0.1f),
                                    selectedLabelColor = RegisterColors.PrimaryOrange,
                                    labelColor = RegisterColors.DarkGray
                                )
                            )
                            FilterChip(
                                selected = paymentMethod == "cash",
                                onClick = { paymentMethod = "cash" },
                                label = { 
                                    Text(
                                        "Efectivo",
                                        color = if (paymentMethod == "cash") {
                                            RegisterColors.PrimaryOrange
                                        } else {
                                            RegisterColors.DarkGray
                                        }
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RegisterColors.PrimaryOrange.copy(alpha = 0.15f),
                                    containerColor = RegisterColors.PrimaryBlue.copy(alpha = 0.1f),
                                    selectedLabelColor = RegisterColors.PrimaryOrange,
                                    labelColor = RegisterColors.DarkGray
                                )
                            )
                        }
                    }
                }
            }

            // ----------------------------
            // TARJETA: TARIFA BASE
            // ----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardInternalPadding)
                    ) {
                        OutlinedTextField(
                            value = baseFee,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() || c == '.' }) baseFee = it
                            },
                            label = { Text("Tarifa base (S/)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(fieldBorderRadius), // Pill-shaped como LoginScreen
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = RegisterColors.BorderGray,
                                focusedBorderColor = RegisterColors.BorderGray,
                                unfocusedTextColor = Color(0xFF212121)
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.AttachMoney, null, tint = RegisterColors.IconGray)
                            }
                        )
                    }
                }
            }

            // ----------------------------
            // TARJETA: DIRECCIÓN
            // ----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardInternalPadding),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Dirección") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(fieldBorderRadius), // Pill-shaped como LoginScreen
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = RegisterColors.BorderGray,
                                focusedBorderColor = RegisterColors.BorderGray,
                                unfocusedTextColor = Color(0xFF212121)
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.LocationOn, null, tint = RegisterColors.IconGray)
                            },
                            trailingIcon = {
                                when {
                                    isGettingCoordinates -> CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    latitude != null -> Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        )

                        var showPermissionDialog by remember { mutableStateOf(false) }

                        if (showPermissionDialog) {
                            LocationPermissionHandler(
                                onPermissionGranted = {
                                    showPermissionDialog = false
                                    coroutineScope.launch {
                                        isGettingCoordinates = true
                                        val coords =
                                            geocodingService.getCurrentLocationCoordinates(locationService)
                                        if (coords != null) {
                                            latitude = BigDecimal(coords.first.toString())
                                            longitude = BigDecimal(coords.second.toString())
                                        }
                                        isGettingCoordinates = false
                                    }
                                },
                                onPermissionDenied = { showPermissionDialog = false },
                                showDialog = true
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (locationService.hasLocationPermission() &&
                                    locationService.isGpsEnabled()
                                ) {
                                    coroutineScope.launch {
                                        isGettingCoordinates = true
                                        val coords =
                                            geocodingService.getCurrentLocationCoordinates(locationService)
                                        if (coords != null) {
                                            latitude = BigDecimal(coords.first.toString())
                                            longitude = BigDecimal(coords.second.toString())
                                        }
                                        isGettingCoordinates = false
                                    }
                                } else {
                                    showPermissionDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGettingCoordinates,
                            shape = RoundedCornerShape(buttonBorderRadius), // Mismo que LoginScreen
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF212121)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = RegisterColors.BorderGray
                            )
                        ) {
                            Icon(Icons.Default.MyLocation, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Usar mi ubicación actual")
                        }
                    }
                }
            }

            // ----------------------------
            // BANNER INFORMATIVO
            //-----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(buttonBorderRadius), // Mismo que LoginScreen banner
                    colors = CardDefaults.cardColors(containerColor = RegisterColors.PrimaryBlue)
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
                            "Pagos: Yape o Efectivo. El trabajador puede usar Modo Plus para no pagar comisión.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            // ----------------------------
            // MENSAJE DE ERROR
            // ----------------------------
            if (uiState.errorMessage != null) {
                val error = uiState.errorMessage!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cardBorderRadius), // Mismo que LoginScreen
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(cardInternalPadding)
                        )
                    }
                }
            }

            // ----------------------------
            // BOTÓN CREAR
            // ----------------------------
            item {
                val isValidForm = remember(title, address, baseFee) {
                    val fee = baseFee.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    title.isNotBlank() &&
                            address.isNotBlank() &&
                            fee > BigDecimal.ZERO
                }

                    Button(
                        onClick = {
                            val fee = baseFee.toBigDecimalOrNull() ?: BigDecimal.ZERO

                            if (isValidForm) {
                                viewModel.createJob(
                                    title,
                                    description.ifBlank { null },
                                    serviceType,
                                    paymentMethod,
                                    fee,
                                    address,
                                    latitude,
                                    longitude
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((56.dp * scaleFactor).coerceIn(50.dp, 64.dp)), // Mismo que LoginScreen
                        enabled = !uiState.isLoading && isValidForm,
                        shape = RoundedCornerShape(buttonBorderRadius), // Mismo que LoginScreen
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterColors.PrimaryOrange,
                            contentColor = Color.White,
                            disabledContainerColor = RegisterColors.PrimaryOrange,
                            disabledContentColor = Color.White
                        )
                    ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Crear Trabajo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
