# 📱 Arquitectura Completa de la App Android - ServiFast

**Proyecto**: GetJob (ServiFast)  
**Lenguaje**: Kotlin  
**Framework UI**: Jetpack Compose  
**Arquitectura**: MVVM (Model-View-ViewModel)  
**Total de Archivos**: 86 archivos Kotlin

---

## 📂 Árbol Completo de la Arquitectura

```
app/src/main/java/com/example/getjob/
│
├── 📱 GetJobApplication.kt              # Application class (inicialización global)
├── 🎬 MainActivity.kt                    # Activity principal (entry point)
│
├── 📦 data/                              # Capa de Datos
│   │
│   ├── 🌐 api/                          # Interfaces Retrofit (7 archivos)
│   │   ├── ApiClient.kt                 # Configuración Retrofit + Interceptor JWT
│   │   ├── AuthApi.kt                   # POST /api/auth/login, /register, /me
│   │   ├── JobApi.kt                    # GET/POST /api/jobs/* (trabajos)
│   │   ├── WorkerApi.kt                 # GET/POST /api/workers/* (perfil trabajador)
│   │   ├── CommissionApi.kt             # GET/POST /api/commissions/* (comisiones)
│   │   ├── ChatApi.kt                   # GET/POST /api/chat/* (mensajes HTTP)
│   │   └── LocationApi.kt               # POST /api/location/update (geolocalización)
│   │
│   ├── 📋 models/                       # Modelos de Datos (DTOs)
│   │   │
│   │   ├── 📤 requests/                 # Request Models (8 archivos)
│   │   │   ├── LoginRequest.kt          # { email, password }
│   │   │   ├── RegisterRequest.kt        # { email, password, role, full_name, phone }
│   │   │   ├── WorkerRegisterRequest.kt   # { full_name, phone, services[], ... }
│   │   │   ├── CreateJobRequest.kt       # { title, description, service_type, ... }
│   │   │   ├── AddExtraRequest.kt        # { extra_amount, description }
│   │   │   ├── RateJobRequest.kt        # { rating, comment }
│   │   │   ├── CommissionSubmitPaymentRequest.kt  # { payment_code, payment_proof_url }
│   │   │   └── SendMessageRequest.kt     # { message, application_id? }
│   │   │
│   │   └── 📥 responses/                # Response Models (12 archivos)
│   │       ├── AuthResponse.kt          # { access_token, token_type, user }
│   │       ├── UserResponse.kt           # { id, email, role, full_name, phone }
│   │       ├── WorkerResponse.kt         # { id, user_id, full_name, services[], ... }
│   │       ├── WorkerInfo.kt             # Info básica trabajador (embebido)
│   │       ├── ClientInfo.kt             # Info básica cliente (embebido)
│   │       ├── ClientResponse.kt         # Respuesta completa cliente
│   │       ├── JobResponse.kt            # { id, title, status, client, worker, ... }
│   │       ├── JobApplicationResponse.kt # { id, job_id, worker_id, is_accepted, worker }
│   │       ├── CommissionResponse.kt    # { id, amount, status, job, ... }
│   │       ├── RatingResponse.kt         # { worker_rating, client_rating, comments }
│   │       └── MessageResponse.kt        # { id, message, sender_id, created_at }
│   │
│   ├── 🗄️ repository/                  # Repositories (5 archivos)
│   │   ├── AuthRepository.kt            # Lógica de autenticación (login, registro)
│   │   ├── JobRepository.kt             # Lógica de trabajos (crear, listar, aplicar)
│   │   ├── WorkerRepository.kt          # Lógica de trabajadores (perfil, búsqueda)
│   │   ├── CommissionRepository.kt      # Lógica de comisiones (pendientes, historial)
│   │   └── ChatRepository.kt            # Lógica de chat (mensajes HTTP)
│   │
│   └── 🔌 websocket/                    # WebSocket (1 archivo)
│       └── ChatWebSocketClient.kt       # Cliente WebSocket para chat en tiempo real
│
├── 🎨 presentation/                     # Capa de Presentación
│   │
│   ├── 🧩 components/                  # Componentes Reutilizables (4 archivos)
│   │   ├── BottomNavigationBar.kt       # Barra de navegación inferior
│   │   ├── OSMMapView.kt                # Vista de mapa básica (OSMDroid)
│   │   ├── EnhancedOSMMapView.kt      # Vista de mapa mejorada (con marcadores, rutas)
│   │   └── LocationPermissionHandler.kt # Manejo de permisos de ubicación
│   │
│   ├── 🧭 navigation/                   # Navegación (2 archivos)
│   │   ├── NavGraph.kt                  # Grafo de navegación (rutas entre pantallas)
│   │   └── NavigationGuard.kt            # Guard de navegación (verifica autenticación)
│   │
│   ├── 📺 screens/                      # Pantallas Compose (20 archivos)
│   │   │
│   │   ├── 🔐 login/
│   │   │   └── LoginScreen.kt           # Pantalla de login/registro
│   │   │
│   │   ├── 📝 register/
│   │   │   ├── RegisterClientScreen.kt  # Registro de cliente
│   │   │   ├── RegisterWorkerScreen.kt  # Registro de trabajador (onboarding completo)
│   │   │   ├── RegisterColors.kt        # Colores del tema de registro
│   │   │   └── RegisterComponents.kt    # Componentes reutilizables de registro
│   │   │
│   │   ├── 🏠 dashboard/
│   │   │   └── DashboardScreen.kt       # Dashboard trabajador (trabajos disponibles)
│   │   │
│   │   ├── 👤 client/
│   │   │   ├── ClientDashboardScreen.kt # Dashboard cliente (mis trabajos)
│   │   │   ├── CreateJobScreen.kt       # Crear nuevo trabajo
│   │   │   └── ClientRateWorkerScreen.kt # Calificar trabajador
│   │   │
│   │   ├── 📋 jobdetail/
│   │   │   └── JobDetailScreen.kt       # Detalle de trabajo (cliente/trabajador)
│   │   │
│   │   ├── 🚗 onroute/
│   │   │   └── OnRouteScreen.kt         # Trabajador en ruta al cliente (mapa + ETA)
│   │   │
│   │   ├── 📍 onsite/
│   │   │   └── OnSiteScreen.kt          # Trabajador llegó al sitio
│   │   │
│   │   ├── 🔧 service/
│   │   │   └── ServiceInProgressScreen.kt # Servicio en progreso (cronómetro, evidencias)
│   │   │
│   │   ├── 💰 payment/
│   │   │   └── PaymentAndReviewScreen.kt # Confirmar pago y calificar
│   │   │
│   │   ├── 💵 commissions/
│   │   │   └── PendingCommissionsScreen.kt # Comisiones pendientes del trabajador
│   │   │
│   │   ├── 💬 chat/
│   │   │   └── ChatScreen.kt            # Pantalla de chat en tiempo real
│   │   │
│   │   ├── 👤 profile/
│   │   │   ├── ProfileScreen.kt         # Perfil trabajador
│   │   │   ├── ClientProfileScreen.kt    # Perfil cliente
│   │   │   └── EditClientProfileScreen.kt # Editar perfil cliente
│   │   │
│   │   └── 📨 requests/
│   │       └── WorkerRequestsScreen.kt  # Mis aplicaciones (trabajador)
│   │
│   └── 🧠 viewmodel/                    # ViewModels (12 archivos)
│       ├── LoginViewModel.kt            # Lógica de login/registro
│       ├── RegisterViewModel.kt          # Lógica de registro (cliente/trabajador)
│       ├── DashboardViewModel.kt        # Lógica dashboard trabajador
│       ├── ClientDashboardViewModel.kt  # Lógica dashboard cliente
│       ├── CreateJobViewModel.kt        # Lógica crear trabajo
│       ├── JobDetailViewModel.kt        # Lógica detalle trabajo
│       ├── WorkerRequestsViewModel.kt    # Lógica mis aplicaciones
│       ├── PaymentAndReviewViewModel.kt  # Lógica pago y calificación
│       ├── PendingCommissionsViewModel.kt # Lógica comisiones pendientes
│       ├── ProfileViewModel.kt          # Lógica perfil trabajador
│       ├── ClientProfileViewModel.kt     # Lógica perfil cliente
│       ├── ClientRateWorkerViewModel.kt  # Lógica calificar trabajador
│       └── ChatViewModel.kt              # Lógica chat (WebSocket + HTTP)
│
├── 🎨 ui/                                # Sistema de Diseño
│   └── theme/                            # Tema de la App (3 archivos)
│       ├── Color.kt                     # Paleta de colores
│       ├── Theme.kt                     # Tema Material Design 3
│       └── Type.kt                      # Tipografía
│
└── 🛠️ utils/                             # Utilidades (10 archivos)
    ├── NetworkConfig.kt                 # Configuración de red (BASE_URL)
    ├── PreferencesManager.kt            # SharedPreferences (token, user, settings)
    ├── AuthEventBus.kt                 # EventBus para eventos de autenticación
    ├── ErrorParser.kt                  # Parser de errores HTTP
    ├── LocationService.kt              # Servicio de geolocalización (GPS)
    ├── GeocodingService.kt             # Geocodificación (dirección ↔ coordenadas)
    ├── RouteService.kt                 # Cálculo de rutas y ETA
    ├── ImageStorageManager.kt         # Gestión de imágenes (subida, cache)
    ├── ProximityNotifier.kt           # Notificaciones de proximidad
    └── ResponsiveUtils.kt             # Utilidades responsive (dimensión de pantalla)
```

---

## 📊 Estadísticas de la Arquitectura

### Por Capa:

| Capa | Archivos | Descripción |
|------|----------|-------------|
| **Data** | 33 | APIs, Models, Repositories, WebSocket |
| **Presentation** | 38 | Screens, ViewModels, Components, Navigation |
| **UI** | 3 | Theme (Colors, Typography) |
| **Utils** | 10 | Servicios y utilidades |
| **Root** | 2 | Application, MainActivity |
| **TOTAL** | **86** | |

### Por Tipo de Archivo:

| Tipo | Cantidad | Ejemplos |
|------|----------|----------|
| **Screens** | 20 | LoginScreen, DashboardScreen, ChatScreen |
| **ViewModels** | 12 | LoginViewModel, JobDetailViewModel |
| **APIs** | 7 | AuthApi, JobApi, WorkerApi, CommissionApi |
| **Repositories** | 5 | AuthRepository, JobRepository |
| **Request Models** | 8 | LoginRequest, CreateJobRequest |
| **Response Models** | 12 | AuthResponse, JobResponse, WorkerResponse |
| **Utils** | 10 | LocationService, PreferencesManager |
| **Components** | 4 | BottomNavigationBar, OSMMapView |
| **Navigation** | 2 | NavGraph, NavigationGuard |
| **Theme** | 3 | Color, Theme, Type |
| **Otros** | 3 | ApiClient, ChatWebSocketClient, GetJobApplication |

---

## 🏗️ Descripción Detallada por Módulo

### 📦 1. DATA LAYER (Capa de Datos)

#### 🌐 API (Interfaces Retrofit)

**`ApiClient.kt`**
- Configuración centralizada de Retrofit
- Interceptor JWT (agrega token automáticamente)
- Manejo de errores HTTP
- Configuración de base URL (desarrollo/producción)

**`AuthApi.kt`**
```kotlin
POST   /api/auth/register     → RegisterRequest → UserResponse
POST   /api/auth/login         → LoginRequest → AuthResponse (token + user)
GET    /api/auth/me            → UserResponse (usuario actual)
PUT    /api/auth/me            → UserUpdateRequest → UserResponse
```

**`JobApi.kt`** (17 endpoints)
```kotlin
# Crear y listar
POST   /api/jobs                    → CreateJobRequest → JobResponse
GET    /api/jobs/available           → Query(service_type, search) → List<JobResponse>
GET    /api/jobs/my-jobs             → List<JobResponse> (según rol)
GET    /api/jobs/my-applications     → List<JobApplicationResponse>
GET    /api/jobs/{jobId}             → JobResponse
GET    /api/jobs/{jobId}/applications → List<JobApplicationResponse>

# Aplicar y aceptar
POST   /api/jobs/{jobId}/apply                    → JobResponse
POST   /api/jobs/{jobId}/accept-worker/{appId}     → JobResponse

# Estados del trabajo
POST   /api/jobs/{jobId}/start-route      → JobResponse (PENDING → IN_ROUTE)
POST   /api/jobs/{jobId}/confirm-arrival   → JobResponse (IN_ROUTE → ON_SITE)
POST   /api/jobs/{jobId}/start-service    → JobResponse (ON_SITE → IN_PROGRESS)
POST   /api/jobs/{jobId}/add-extra        → AddExtraRequest → JobResponse
POST   /api/jobs/{jobId}/complete          → JobResponse (IN_PROGRESS → COMPLETED)
POST   /api/jobs/{jobId}/cancel            → JobResponse (cualquier estado → CANCELLED)

# Calificaciones
POST   /api/jobs/{jobId}/rate              → RateJobRequest → RatingResponse (worker califica)
POST   /api/jobs/{jobId}/rate-worker        → RateJobRequest → RatingResponse (client califica)
GET    /api/jobs/{jobId}/rating             → RatingResponse
```

**`WorkerApi.kt`** (6 endpoints)
```kotlin
POST   /api/workers/register          → WorkerRegisterRequest → WorkerResponse
GET    /api/workers/me                → WorkerResponse (perfil actual)
PUT    /api/workers/me                → WorkerUpdate → WorkerResponse
GET    /api/workers/{id}              → WorkerResponse
GET    /api/workers/search/list       → Query(service_type, district, is_available, is_verified) → List<WorkerResponse>
POST   /api/workers/me/verify        → VerificationRequest → WorkerResponse
```

**`CommissionApi.kt`**
```kotlin
GET    /api/commissions/pending
GET    /api/commissions/history
POST   /api/commissions/{id}/submit-payment
```

**`ChatApi.kt`** (2 endpoints HTTP + WebSocket)
```kotlin
GET    /api/chat/{jobId}/messages     → Query(application_id?) → List<MessageResponse>
POST   /api/chat/{jobId}/send         → SendMessageRequest → MessageResponse
# WebSocket: ws://BASE_URL/ws/chat/{jobId}/{applicationId?}
```

**`LocationApi.kt`** (1 endpoint)
```kotlin
POST   /api/location/update           → LocationUpdateRequest → Response (actualiza ubicación trabajador)
```

#### 📋 Models

**Requests (8 archivos):**
- `LoginRequest.kt` - Email y contraseña
- `RegisterRequest.kt` - Datos de registro básico
- `WorkerRegisterRequest.kt` - Registro completo trabajador
- `CreateJobRequest.kt` - Crear trabajo (título, descripción, servicio, etc.)
- `AddExtraRequest.kt` - Agregar extra al trabajo
- `RateJobRequest.kt` - Calificar trabajo (rating, comentario)
- `CommissionSubmitPaymentRequest.kt` - Enviar pago de comisión
- `SendMessageRequest.kt` - Enviar mensaje en chat

**Responses (12 archivos):**
- `AuthResponse.kt` - Token JWT + información usuario
- `UserResponse.kt` - Datos del usuario
- `WorkerResponse.kt` - Perfil completo trabajador
- `WorkerInfo.kt` - Info básica trabajador (embebido en otras respuestas)
- `ClientInfo.kt` - Info básica cliente (embebido)
- `ClientResponse.kt` - Respuesta completa cliente
- `JobResponse.kt` - Trabajo completo con relaciones
- `JobApplicationResponse.kt` - Aplicación de trabajador
- `CommissionResponse.kt` - Comisión con información del trabajo
- `RatingResponse.kt` - Calificaciones mutuas
- `MessageResponse.kt` - Mensaje del chat

#### 🗄️ Repositories

**`AuthRepository.kt`**
- `register(email, password, role, fullName?, phone?)` → `Result<UserResponse>`
- `login(email, password)` → `Result<AuthResponse>`
- `getCurrentUser()` → `Result<UserResponse>`
- `updateProfile(email?, password?, fullName?, phone?)` → `Result<UserResponse>`
- Manejo de errores con `ErrorParser`
- Retorna `Result<T>` para manejo seguro de errores

**`JobRepository.kt`** (15+ métodos)
- `getAvailableJobs(serviceType?, search?)` → `Result<List<JobResponse>>`
- `getMyJobs()` → `Result<List<JobResponse>>` (según rol)
- `getMyApplications()` → `Result<List<JobApplicationResponse>>`
- `getJob(jobId)` → `Result<JobResponse>`
- `getJobApplications(jobId)` → `Result<List<JobApplicationResponse>>`
- `createJob(createJobRequest)` → `Result<JobResponse>`
- `applyToJob(jobId)` → `Result<JobResponse>`
- `acceptWorker(jobId, applicationId)` → `Result<JobResponse>`
- `startRoute(jobId)` → `Result<JobResponse>`
- `confirmArrival(jobId)` → `Result<JobResponse>`
- `startService(jobId)` → `Result<JobResponse>`
- `addExtra(jobId, extraAmount, description)` → `Result<JobResponse>`
- `completeJob(jobId)` → `Result<JobResponse>`
- `cancelJob(jobId)` → `Result<JobResponse>`
- `rateJob(jobId, rating, comment)` → `Result<RatingResponse>`
- `rateWorker(jobId, rating, comment)` → `Result<RatingResponse>`
- `getJobRating(jobId)` → `Result<RatingResponse>`

**`WorkerRepository.kt`** (6 métodos)
- `registerWorker(workerRegisterRequest)` → `Result<WorkerResponse>`
- `getMyProfile()` → `Result<WorkerResponse>`
- `updateMyProfile(workerUpdate)` → `Result<WorkerResponse>`
- `getWorker(workerId)` → `Result<WorkerResponse>`
- `searchWorkers(serviceType?, district?, isAvailable?, isVerified?)` → `Result<List<WorkerResponse>>`
- `submitVerification(photoUrl)` → `Result<WorkerResponse>`

**`CommissionRepository.kt`** (3 métodos)
- `getPendingCommissions()` → `Result<List<CommissionResponse>>`
- `getCommissionHistory()` → `Result<List<CommissionResponse>>`
- `submitPayment(commissionId, paymentCode, paymentProofUrl?)` → `Result<CommissionResponse>`

**`ChatRepository.kt`** (2 métodos HTTP + WebSocket)
- `getMessages(jobId, applicationId?)` → `Result<List<MessageResponse>>`
- `sendMessage(jobId, message, applicationId?)` → `Result<MessageResponse>`
- WebSocket: Conexión en tiempo real (manejado por `ChatWebSocketClient`)

#### 🔌 WebSocket

**`ChatWebSocketClient.kt`**
- Conexión WebSocket para chat en tiempo real
- Envío/recepción de mensajes
- Reconexión automática
- Manejo de estados de conexión

---

### 🎨 2. PRESENTATION LAYER (Capa de Presentación)

#### 🧩 Components (Componentes Reutilizables)

**`BottomNavigationBar.kt`**
- Barra de navegación inferior
- 4 elementos: Inicio, Solicitudes, Perfil, Comisiones
- Solo visible después de autenticación

**`OSMMapView.kt`**
- Vista básica de mapa (OSMDroid)
- Muestra ubicación en mapa

**`EnhancedOSMMapView.kt`**
- Vista de mapa mejorada
- Marcadores personalizados
- Rutas entre puntos
- ETA (tiempo estimado de llegada)

**`LocationPermissionHandler.kt`**
- Manejo de permisos de ubicación
- Solicita permisos si no están concedidos
- Verifica estado de permisos

#### 🧭 Navigation

**`NavGraph.kt`**
- Define todas las rutas de la app
- Navegación entre pantallas
- Argumentos de navegación
- Deep links

**`NavigationGuard.kt`**
- Verifica autenticación antes de navegar
- Redirige a login si no está autenticado
- Verifica permisos según rol

#### 📺 Screens (Pantallas)

**🔐 Autenticación:**
- `LoginScreen.kt` - Login/Registro con selección de rol

**📝 Registro:**
- `RegisterClientScreen.kt` - Registro básico cliente
- `RegisterWorkerScreen.kt` - Registro completo trabajador (onboarding)
- `RegisterColors.kt` - Colores del tema de registro
- `RegisterComponents.kt` - Componentes reutilizables de registro

**🏠 Dashboard:**
- `DashboardScreen.kt` - Dashboard trabajador (trabajos disponibles, disponibilidad)

**👤 Cliente:**
- `ClientDashboardScreen.kt` - Dashboard cliente (mis trabajos)
- `CreateJobScreen.kt` - Crear nuevo trabajo
- `ClientRateWorkerScreen.kt` - Calificar trabajador

**📋 Trabajos:**
- `JobDetailScreen.kt` - Detalle de trabajo (cliente/trabajador)
- `WorkerRequestsScreen.kt` - Mis aplicaciones (trabajador)

**🚗 Flujo de Servicio:**
- `OnRouteScreen.kt` - Trabajador en ruta (mapa + ETA)
- `OnSiteScreen.kt` - Trabajador llegó al sitio
- `ServiceInProgressScreen.kt` - Servicio en progreso (cronómetro, evidencias)
- `PaymentAndReviewScreen.kt` - Confirmar pago y calificar

**💵 Comisiones:**
- `PendingCommissionsScreen.kt` - Comisiones pendientes del trabajador

**💬 Chat:**
- `ChatScreen.kt` - Chat en tiempo real (WebSocket)

**👤 Perfil:**
- `ProfileScreen.kt` - Perfil trabajador
- `ClientProfileScreen.kt` - Perfil cliente
- `EditClientProfileScreen.kt` - Editar perfil cliente

#### 🧠 ViewModels

**`LoginViewModel.kt`**
- Estado de login/registro
- Validación de formularios
- Llamadas a AuthRepository
- Manejo de errores

**`RegisterViewModel.kt`**
- Estado de registro (cliente/trabajador)
- Validación de campos
- Llamadas a AuthRepository/WorkerRepository
- Navegación post-registro

**`DashboardViewModel.kt`**
- Estado del dashboard trabajador
- Lista de trabajos disponibles
- Filtros (service_type, search)
- Disponibilidad del trabajador

**`ClientDashboardViewModel.kt`**
- Estado del dashboard cliente
- Lista de mis trabajos
- Crear nuevo trabajo
- Estados de trabajos

**`CreateJobViewModel.kt`**
- Estado del formulario crear trabajo
- Validación de campos
- Geocodificación de dirección
- Llamada a JobRepository

**`JobDetailViewModel.kt`**
- Estado del detalle de trabajo
- Información del trabajo
- Aplicaciones (si es cliente)
- Acciones según rol

**`WorkerRequestsViewModel.kt`**
- Estado de mis aplicaciones
- Lista de aplicaciones pendientes/aceptadas
- Navegación a detalle de trabajo

**`PaymentAndReviewViewModel.kt`**
- Estado de pago y calificación
- Confirmar método de pago
- Calificar cliente/trabajador
- Finalizar trabajo

**`PendingCommissionsViewModel.kt`**
- Estado de comisiones pendientes
- Lista de comisiones
- Enviar pago de comisión
- Historial de comisiones

**`ProfileViewModel.kt`**
- Estado del perfil trabajador
- Actualizar perfil
- Enviar verificación
- Disponibilidad

**`ClientProfileViewModel.kt`**
- Estado del perfil cliente
- Actualizar perfil
- Ver trabajos completados

**`ClientRateWorkerViewModel.kt`**
- Estado de calificación
- Calificar trabajador
- Validación de rating

**`ChatViewModel.kt`**
- Estado del chat
- Mensajes (HTTP + WebSocket)
- Envío de mensajes
- Conexión WebSocket

---

### 🎨 3. UI LAYER (Sistema de Diseño)

**`Color.kt`**
- Paleta de colores de la app
- Colores primarios, secundarios
- Colores de estado (éxito, error, advertencia)

**`Theme.kt`**
- Tema Material Design 3
- Configuración de colores y tipografía
- Modo claro/oscuro (si aplica)

**`Type.kt`**
- Tipografía de la app
- Estilos de texto (h1, h2, body, caption)

---

### 🛠️ 4. UTILS (Utilidades)

**`NetworkConfig.kt`**
- Configuración de red
- BASE_URL (desarrollo/producción)
- Timeouts y configuración HTTP

**`PreferencesManager.kt`**
- `saveAuthData(token, userId, email, role)` - Guardar datos de autenticación
- `getToken()` → `String?` - Obtener token JWT
- `getUserId()` → `Int` - Obtener ID de usuario
- `getUserEmail()` → `String?` - Obtener email
- `getUserRole()` → `String?` - Obtener rol (client/worker/manager)
- `isLoggedIn()` → `Boolean` - Verificar si está logueado
- `clearAuthData()` - Limpiar datos de autenticación (logout)
- `setProfileCreatedFirstTime(value)` - Marcar perfil creado
- `isProfileCreatedFirstTime()` → `Boolean` - Verificar si perfil fue creado
- Usa `SharedPreferences` con nombre "ServiFastPrefs"

**`AuthEventBus.kt`**
- EventBus para eventos de autenticación
- Logout, cambio de usuario
- Comunicación entre componentes

**`ErrorParser.kt`**
- Parser de errores HTTP
- Extrae mensajes de error del backend
- Manejo de errores de red

**`LocationService.kt`**
- Servicio de geolocalización
- Obtener ubicación actual (GPS)
- Actualizar ubicación periódicamente
- Permisos de ubicación

**`GeocodingService.kt`**
- Geocodificación (dirección → coordenadas)
- Reverse geocoding (coordenadas → dirección)
- Integración con servicios de geocodificación

**`RouteService.kt`**
- Cálculo de rutas entre puntos
- ETA (tiempo estimado de llegada)
- Distancia entre puntos
- Integración con servicios de routing

**`ImageStorageManager.kt`**
- Gestión de imágenes
- Subida de imágenes al servidor
- Cache de imágenes
- Compresión de imágenes

**`ProximityNotifier.kt`**
- Notificaciones de proximidad
- Alerta cuando trabajador está cerca
- Notificaciones push (si aplica)

**`ResponsiveUtils.kt`**
- Utilidades responsive
- Dimensiones de pantalla
- Densidad de píxeles
- Helpers para diseño adaptativo

---

## 🔄 Flujo de Datos (MVVM)

```
Screen (Compose UI)
    ↓ (observa State)
ViewModel
    ↓ (llama métodos)
Repository
    ↓ (llama API)
API (Retrofit)
    ↓ (HTTP Request)
Backend (FastAPI)
    ↓ (Response)
API (Retrofit)
    ↓ (convierte a Model)
Repository
    ↓ (retorna Result)
ViewModel
    ↓ (actualiza State)
Screen (Compose UI)
    ↓ (recompone con nuevo State)
```

---

## 🔐 Flujo de Autenticación

```
1. LoginScreen
   ↓
2. LoginViewModel.login()
   ↓
3. AuthRepository.login()
   ↓
4. AuthApi.login()
   ↓
5. Backend valida y retorna JWT
   ↓
6. PreferencesManager.saveToken()
   ↓
7. NavigationGuard verifica token
   ↓
8. Navega a Dashboard según rol
```

---

## 📱 Flujo de un Trabajo (Worker)

```
1. DashboardScreen
   ↓ Ver trabajos disponibles
2. JobDetailScreen
   ↓ Ver detalles
3. WorkerRequestsScreen
   ↓ Aplicar a trabajo
4. JobDetailScreen (trabajo aceptado)
   ↓
5. OnRouteScreen
   ↓ Iniciar ruta
6. OnSiteScreen
   ↓ Confirmar llegada
7. ServiceInProgressScreen
   ↓ Iniciar servicio
8. PaymentAndReviewScreen
   ↓ Completar y calificar
9. DashboardScreen
```

---

## 📱 Flujo de un Trabajo (Client)

```
1. ClientDashboardScreen
   ↓
2. CreateJobScreen
   ↓ Crear trabajo
3. ClientDashboardScreen
   ↓ Ver aplicaciones
4. JobDetailScreen
   ↓ Ver aplicaciones
5. JobDetailScreen
   ↓ Aceptar trabajador
6. JobDetailScreen (seguimiento)
   ↓ Ver estado en tiempo real
7. ClientRateWorkerScreen
   ↓ Calificar trabajador
8. ClientDashboardScreen
```

---

## 🗂️ Organización por Funcionalidad

### Autenticación
- `LoginScreen.kt` + `LoginViewModel.kt`
- `RegisterClientScreen.kt` + `RegisterViewModel.kt`
- `RegisterWorkerScreen.kt` + `RegisterViewModel.kt`
- `AuthApi.kt` + `AuthRepository.kt`

### Trabajos
- `DashboardScreen.kt` + `DashboardViewModel.kt`
- `JobDetailScreen.kt` + `JobDetailViewModel.kt`
- `WorkerRequestsScreen.kt` + `WorkerRequestsViewModel.kt`
- `OnRouteScreen.kt` + `JobDetailViewModel.kt`
- `OnSiteScreen.kt` + `JobDetailViewModel.kt`
- `ServiceInProgressScreen.kt` + `JobDetailViewModel.kt`
- `PaymentAndReviewScreen.kt` + `PaymentAndReviewViewModel.kt`
- `JobApi.kt` + `JobRepository.kt`

### Cliente
- `ClientDashboardScreen.kt` + `ClientDashboardViewModel.kt`
- `CreateJobScreen.kt` + `CreateJobViewModel.kt`
- `ClientRateWorkerScreen.kt` + `ClientRateWorkerViewModel.kt`
- `ClientProfileScreen.kt` + `ClientProfileViewModel.kt`

### Perfil Trabajador
- `ProfileScreen.kt` + `ProfileViewModel.kt`
- `WorkerApi.kt` + `WorkerRepository.kt`

### Comisiones
- `PendingCommissionsScreen.kt` + `PendingCommissionsViewModel.kt`
- `CommissionApi.kt` + `CommissionRepository.kt`

### Chat
- `ChatScreen.kt` + `ChatViewModel.kt`
- `ChatApi.kt` + `ChatRepository.kt`
- `ChatWebSocketClient.kt`

---

## 📊 Métricas de Complejidad

### Pantallas más Complejas:
1. **RegisterWorkerScreen.kt** - Onboarding completo (múltiples pasos)
2. **ServiceInProgressScreen.kt** - Cronómetro, evidencias, notas
3. **OnRouteScreen.kt** - Mapa en tiempo real, ETA dinámico
4. **ChatScreen.kt** - WebSocket + HTTP, tiempo real
5. **DashboardScreen.kt** - Lista de trabajos, filtros, disponibilidad

### ViewModels más Complejos:
1. **ChatViewModel.kt** - Manejo de WebSocket + HTTP
2. **JobDetailViewModel.kt** - Múltiples estados y acciones
3. **DashboardViewModel.kt** - Filtros, búsqueda, disponibilidad
4. **CreateJobViewModel.kt** - Validación, geocodificación

---

## 🔗 Dependencias entre Módulos

```
presentation/screens
    ↓ depende de
presentation/viewmodel
    ↓ depende de
data/repository
    ↓ depende de
data/api
    ↓ depende de
utils (NetworkConfig, PreferencesManager)
```

---

## 📝 Notas de Arquitectura

### Principios Aplicados:

1. **Separación de Responsabilidades**
   - Data Layer: Solo acceso a datos
   - Presentation Layer: Solo UI y lógica de presentación
   - Utils: Funcionalidades transversales

2. **MVVM Pattern**
   - View (Screen) observa State del ViewModel
   - ViewModel llama a Repository
   - Repository llama a API
   - Flujo unidireccional de datos

3. **Single Source of Truth**
   - ViewModel es la única fuente de verdad
   - State inmutable
   - Recompone UI cuando cambia State

4. **Dependency Injection**
   - Repositories inyectados en ViewModels
   - APIs inyectadas en Repositories
   - Configuración centralizada en ApiClient

---

## 🚀 Próximas Mejoras Sugeridas

1. **Room Database** - Cache local de datos
2. **Hilt/Dagger** - Dependency Injection formal
3. **Coroutines Flow** - Flujos reactivos
4. **StateFlow/SharedFlow** - Estado reactivo mejorado
5. **Testing** - Unit tests y UI tests
6. **Offline Support** - Funcionalidad sin conexión

---

## 🧭 Rutas de Navegación (NavGraph)

### Pantallas Principales:

```kotlin
// Autenticación
Screen.Login                    → "login"
Screen.Register                 → "register/{role}" (client/worker)

// Dashboard
Screen.Dashboard                → "dashboard" (worker)
Screen.ClientDashboard          → "client_dashboard" (client)

// Trabajos
Screen.CreateJob                → "create_job"
Screen.SolicitudDetail          → "solicitud_detail/{jobId}/{applicationId}"
Screen.WorkerRequests            → "worker_requests"

// Flujo de Servicio
Screen.OnRoute                  → "on_route/{jobId}"
Screen.OnSite                   → "on_site/{jobId}"
Screen.ServiceInProgress         → "service/{jobId}"
Screen.PaymentAndReview         → "payment_review/{jobId}"

// Perfil
Screen.Profile                   → "profile" (worker)
Screen.ClientProfile            → "client_profile"
Screen.EditClientProfile        → "edit_client_profile"
Screen.CompleteProfile          → "complete_profile" (worker onboarding)

// Comisiones
Screen.PendingCommissions       → "pending_commissions"

// Chat
Screen.Chat                     → "chat/{jobId}/{applicationId}"

// Calificaciones
Screen.ClientRateWorker         → "client_rate_worker/{jobId}"
```

### Flujo de Navegación Típico:

**Worker:**
```
Login → Register (worker) → CompleteProfile → Dashboard
  ↓
Dashboard → SolicitudDetail → Apply → WorkerRequests
  ↓
WorkerRequests → SolicitudDetail (aceptado) → OnRoute
  ↓
OnRoute → OnSite → ServiceInProgress → PaymentAndReview
  ↓
Dashboard (trabajo completado)
```

**Client:**
```
Login → Register (client) → ClientDashboard
  ↓
ClientDashboard → CreateJob → ClientDashboard (trabajo creado)
  ↓
ClientDashboard → SolicitudDetail → Ver aplicaciones → Accept Worker
  ↓
SolicitudDetail (seguimiento) → Chat → ClientRateWorker
  ↓
ClientDashboard (trabajo completado)
```

---

## 🔐 Gestión de Autenticación

### Flujo de Login:

1. **LoginScreen** - Usuario ingresa email/password
2. **LoginViewModel.login()** - Valida y llama a AuthRepository
3. **AuthRepository.login()** - Llama a AuthApi
4. **Backend** - Valida y retorna JWT token
5. **PreferencesManager.saveAuthData()** - Guarda token y datos
6. **NavigationGuard** - Verifica token y redirige según rol:
   - `role == "worker"` → Dashboard
   - `role == "client"` → ClientDashboard

### Manejo de Token Expirado:

1. **ApiClient.authInterceptor** - Detecta 401/403
2. **PreferencesManager.clearAuthData()** - Limpia sesión
3. **AuthEventBus.emitTokenExpired()** - Emite evento
4. **NavGraph** - Escucha evento y redirige a Login

---

## 📱 Estados de la Aplicación

### Estados de Trabajo (JobStatus):

```kotlin
PENDING        → Trabajo creado, esperando aplicaciones
ACCEPTED       → Trabajador aceptado
IN_ROUTE       → Trabajador en camino
ON_SITE        → Trabajador llegó al sitio
IN_PROGRESS    → Servicio en progreso
COMPLETED      → Servicio completado
CANCELLED      → Trabajo cancelado
```

### Estados de Comisión (CommissionStatus):

```kotlin
PENDING              → Comisión pendiente de pago
PAYMENT_SUBMITTED    → Pago enviado (esperando aprobación)
APPROVED             → Pago aprobado por manager
REJECTED             → Pago rechazado
```

---

## 🗂️ Organización de Archivos por Funcionalidad

### Autenticación (4 archivos):
- `LoginScreen.kt` + `LoginViewModel.kt`
- `RegisterClientScreen.kt` + `RegisterWorkerScreen.kt` + `RegisterViewModel.kt`
- `AuthApi.kt` + `AuthRepository.kt`

### Trabajos (12 archivos):
- `DashboardScreen.kt` + `DashboardViewModel.kt`
- `JobDetailScreen.kt` + `JobDetailViewModel.kt`
- `WorkerRequestsScreen.kt` + `WorkerRequestsViewModel.kt`
- `OnRouteScreen.kt`, `OnSiteScreen.kt`, `ServiceInProgressScreen.kt`
- `PaymentAndReviewScreen.kt` + `PaymentAndReviewViewModel.kt`
- `JobApi.kt` + `JobRepository.kt`
- `CreateJobRequest.kt`, `JobResponse.kt`, `JobApplicationResponse.kt`

### Cliente (6 archivos):
- `ClientDashboardScreen.kt` + `ClientDashboardViewModel.kt`
- `CreateJobScreen.kt` + `CreateJobViewModel.kt`
- `ClientRateWorkerScreen.kt` + `ClientRateWorkerViewModel.kt`
- `ClientProfileScreen.kt` + `ClientProfileViewModel.kt`

### Perfil Trabajador (4 archivos):
- `ProfileScreen.kt` + `ProfileViewModel.kt`
- `WorkerApi.kt` + `WorkerRepository.kt`
- `WorkerRegisterRequest.kt`, `WorkerResponse.kt`

### Comisiones (4 archivos):
- `PendingCommissionsScreen.kt` + `PendingCommissionsViewModel.kt`
- `CommissionApi.kt` + `CommissionRepository.kt`
- `CommissionResponse.kt`, `CommissionSubmitPaymentRequest.kt`

### Chat (5 archivos):
- `ChatScreen.kt` + `ChatViewModel.kt`
- `ChatApi.kt` + `ChatRepository.kt`
- `ChatWebSocketClient.kt`
- `MessageResponse.kt`, `SendMessageRequest.kt`

---

## 🛠️ Configuración y Setup

### NetworkConfig.kt:
```kotlin
// Desarrollo con ngrok (acceso desde cualquier dispositivo)
const val BASE_URL = "https://tu-url.ngrok-free.app"

// Desarrollo local (emulador)
// const val BASE_URL = "http://10.0.2.2:8000"

// Desarrollo local (dispositivo físico, misma WiFi)
// const val BASE_URL = "http://192.168.1.100:8000"

// Producción
// const val BASE_URL = "https://tu-app.railway.app"
```

### ApiClient.kt:
- Interceptor JWT automático
- Manejo de tokens expirados
- Logging condicional (solo en debug)
- Timeouts configurados (30 segundos)
- Soporte para ngrok (skip browser warning)

### GetJobApplication.kt:
- Inicializa OSMDroid (mapas)
- Configura cache de tiles
- User agent para mapas

---

## 📊 Métricas de Complejidad

### Pantallas más Complejas (por líneas de código):
1. **RegisterWorkerScreen.kt** - Onboarding completo (múltiples pasos, validaciones)
2. **ServiceInProgressScreen.kt** - Cronómetro, evidencias, notas, estados
3. **OnRouteScreen.kt** - Mapa en tiempo real, ETA dinámico, actualización de ubicación
4. **ChatScreen.kt** - WebSocket + HTTP, mensajes en tiempo real, scroll automático
5. **DashboardScreen.kt** - Lista de trabajos, filtros, disponibilidad, pull-to-refresh

### ViewModels más Complejos:
1. **ChatViewModel.kt** - Manejo de WebSocket + HTTP, estados de conexión
2. **JobDetailViewModel.kt** - Múltiples estados, acciones según rol, validaciones
3. **DashboardViewModel.kt** - Filtros, búsqueda, disponibilidad, actualización periódica
4. **CreateJobViewModel.kt** - Validación de campos, geocodificación, estados de formulario

---

## 🔗 Dependencias Externas

### Librerías Principales:
- **Jetpack Compose** - UI moderna
- **Retrofit** - Cliente HTTP
- **OkHttp** - Cliente HTTP + WebSocket
- **Gson** - Serialización JSON
- **OSMDroid** - Mapas OpenStreetMap
- **Navigation Compose** - Navegación
- **ViewModel** - Gestión de estado
- **Coroutines** - Programación asíncrona
- **SharedPreferences** - Almacenamiento local

---

## 📝 Convenciones de Código

### Nomenclatura:
- **Screens**: `*Screen.kt` (ej: `LoginScreen.kt`)
- **ViewModels**: `*ViewModel.kt` (ej: `LoginViewModel.kt`)
- **APIs**: `*Api.kt` (ej: `AuthApi.kt`)
- **Repositories**: `*Repository.kt` (ej: `AuthRepository.kt`)
- **Requests**: `*Request.kt` (ej: `LoginRequest.kt`)
- **Responses**: `*Response.kt` (ej: `AuthResponse.kt`)

### Estructura de ViewModel:
```kotlin
class XViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(XUiState())
    val uiState: StateFlow<XUiState> = _uiState.asStateFlow()
    
    fun action() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Lógica
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
```

### Estructura de Repository:
```kotlin
class XRepository {
    private val api = ApiClient.xApi
    
    suspend fun method(): Result<XResponse> {
        return try {
            val response = api.method()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

**Última actualización**: 2024  
**Total de Archivos**: 86 archivos Kotlin  
**Arquitectura**: MVVM con Jetpack Compose  
**Lenguaje**: Kotlin 100%  
**UI Framework**: Jetpack Compose  
**API Client**: Retrofit + OkHttp  
**Mapas**: OSMDroid (OpenStreetMap)  
**Navegación**: Navigation Compose  
**Estado**: StateFlow + ViewModel

